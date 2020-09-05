/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.BufferMappingHandle;
import com.jme3.buffer.UntypedBuffer.MappingFlag;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.conditional.SyncObject;
import com.jme3.conditional.SyncObject.Signal;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class RingBuffer {

    private final int BYTES;
    private final int BLOCKS;
    private final Renderer RENDERER;
    private final UntypedBuffer BUFFER;
    private final BufferMappingHandle MAPPING;

    private int currentBlock;
    private final SyncObject[] SYNC_OBJS;
    private final RingBufferBlock[] RING_BLOCKS;

    public RingBuffer(Renderer renderer, int bytes, int blocks) {
        if (!renderer.getCaps().contains(Caps.BufferStorage) || !renderer.getCaps().contains(Caps.MapBuffer)) {
            throw new UnsupportedOperationException("Hardware does not support Persistently Mapped Buffers");
        }
        RENDERER = renderer;
        BUFFER = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer,
                StorageFlag.Dynamic, StorageFlag.MapWrite, StorageFlag.MapPersistent, StorageFlag.MapCoherent);
        BYTES = bytes;
        BLOCKS = blocks;

        BUFFER.initialize(bytes * blocks);
        MAPPING = BUFFER.mapBuffer(MappingFlag.Write, MappingFlag.Persistent, MappingFlag.Coherent);
        SYNC_OBJS = new SyncObject[blocks];
        RING_BLOCKS = new RingBufferBlock[blocks];
        for (int i = 0; i < blocks; i++) {
            SYNC_OBJS[i] = new SyncObject();
            RING_BLOCKS[i] = new RingBufferBlock(i * BYTES, BYTES, MAPPING.getRawData());
        }
        currentBlock = blocks - 1;
    }

    /**
     * Will prepare the next block of memory in this ring buffer.That is, in
     * case this block has been used previously already, it will wait for all
     * commands that used this block to finish, otherwise it will return
     * immediately. In any case, after this method returns, the now current
     * block in this ring buffer can safely be used to write data without
     * overriding data that is needed for previously started draw commands.
     * NOTE: if you notice this method frequently stalls the CPU, consider
     * increasing the block count
     *
     * @return the new current RingBufferBlock that can safely be used to send
     * data to the GPU
     */
    public RingBufferBlock next() {
        if (RING_BLOCKS[currentBlock].valid) {
            RING_BLOCKS[currentBlock].valid = false;
        }
        currentBlock = (currentBlock + 1) % BLOCKS;
        SyncObject sync = SYNC_OBJS[currentBlock];
        if (sync.isPlaced()) {
            long nano = System.nanoTime();
            Signal sig = RENDERER.checkSyncObject(sync);
            while (sig != Signal.AlreadySignaled && sig != Signal.ConditionSatisfied) {
                sig = RENDERER.checkSyncObject(sync);
            }
            long dur = System.nanoTime() - nano;
            //System.out.println("waiting for sync took nanoseconds: " + dur + " = milliseconds: "+(dur / 1000000.0));
        }

        RingBufferBlock block = RING_BLOCKS[currentBlock];
        block.reset();
        return block;
    }

    /**
     * Needs to be called when all data has been uploaded and more importantly,
     * all GL calls that will use that data have been made. The RingBufferBlock
     * returned by the previous next() call is no longer valid
     */
    public void release() {
        SyncObject sync = SYNC_OBJS[currentBlock];
        if (sync.isPlaced()) {
            RENDERER.recycleSyncObject(sync);
        }
        RENDERER.placeSyncObject(sync);
        RING_BLOCKS[currentBlock].valid = false;
    }

    /**
     * Returns the number of blocks this ring buffer is made of
     *
     * @return num blocks
     */
    public int getBlockCount() {
        return BLOCKS;
    }

    /**
     * Returns the size in bytes of a single block
     *
     * @return size of 1 bock
     */
    public int getBlockSize() {
        return BYTES;
    }

    /**
     * Returns the underlying UntypedBuffer. Should only be used to create typed
     * buffer views, not to change the buffers content.
     *
     * @return the underlying untyped buffer
     */
    public UntypedBuffer getBuffer() {
        return BUFFER;
    }

    public static class RingBufferBlock {

        private final int START;
        private final int SIZE;
        private final ByteBuffer BUFFER;
        private int position = 0;
        private boolean valid = false;

        private RingBufferBlock(int start, int length, ByteBuffer buffer) {
            START = start;
            SIZE = length;
            BUFFER = buffer;

            position = START;
        }

        private void reset() {
            valid = true;
            position = START;
        }

        /**
         * Sets the current position relative to the start of this block, that
         * is setPosition(0) will set the position to the start of the block and
         * setPosition(ringBuffer.getBlockSize() - 1) will set it to the end of
         * this block. After a call to ringBuffer.next() the position of the
         * returned RingBufferBlock will be at 0
         *
         * @param pos the position to set it to
         */
        public RingBufferBlock setPosition(int pos) {
            if (pos < 0 || pos >= SIZE) {
                throw new IllegalArgumentException("pos must be in range 0 (incl) - " + SIZE + " (excl) but has value: " + pos);
            }
            position = START + pos;
            return this;
        }

        /**
         * Puts the specified long at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putLong(long value) {
            validate(8);
            BUFFER.putLong(position, value);
            position += 8;
            return this;
        }

        /**
         * Puts the specified int at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putInt(int value) {
            validate(4);
            BUFFER.putInt(position, value);
            position += 4;
            return this;
        }

        /**
         * Puts the specified short at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putShort(short value) {
            validate(2);
            BUFFER.putShort(position, value);
            position += 2;
            return this;
        }

        /**
         * Puts the specified byte at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putByte(byte value) {
            validate(1);
            BUFFER.put(position, value);
            position++;
            return this;
        }
        
        public RingBufferBlock putByte(ByteBuffer values) {
            int bytes = values.remaining();
            validate(bytes);
            int pos = BUFFER.position();
            BUFFER.position(position);
            BUFFER.put(values);
            position += bytes;
            BUFFER.position(pos);
            return this;
        }

        /**
         * Puts the specified segment of the provided byte array at the current
         * position
         *
         * @param values the values to set
         * @param offset offset into the provided array
         * @param length length to read from the array
         */
        public RingBufferBlock putByte(byte[] values, int offset, int length) {
            validate(length);
            int pos = BUFFER.position();
            BUFFER.position(position);
            BUFFER.put(values, offset, length);
            position += length;
            BUFFER.position(pos);
            return this;
        }

        /**
         * Puts the specified float at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putFloat(float value) {
            validate(4);
            BUFFER.putFloat(position, value);
            position += 4;
            return this;
        }

        /**
         * Puts the specified double at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putDouble(double value) {
            validate(8);
            BUFFER.putDouble(position, value);
            position += 8;
            return this;
        }

        /**
         * Returns the limit of this RingBufferBlock. Equal to
         * ringBuffer.getBlockSize()
         *
         * @return
         */
        public int getLimit() {
            return SIZE;
        }

        private void validate(int bytes) {
            //pos can be eg 0 and int will be written, that is 4 bytes
            //if buffer is 4 bytes in size, bytes 0, 1, 2, 3 will be written
            int relPos = position - START;
            if (relPos < 0 || (relPos + bytes) > SIZE) {
                throw new UnsupportedOperationException("position is out of bounds: " + relPos + ", given that it was tried to write " + bytes + " bytes");
            }
            if (!valid) {
                throw new UnsupportedOperationException("This RingBufferBlock is currently invalid");
            }
        }

    }

}
