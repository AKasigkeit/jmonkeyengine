/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer.pmb;

import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.BufferMappingHandle;
import com.jme3.buffer.UntypedBuffer.MappingFlag;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.conditional.SyncObject;
import com.jme3.conditional.SyncObject.Signal;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class MultiBufferRingBuffer implements RingBuffer {

    private final int BYTES;
    private final int BLOCKS;
    private final Renderer RENDERER;
    private final UntypedBuffer[] BUFFERS;
    private final BufferMappingHandle[] MAPPINGS;

    private int currentBlock;
    private final SyncObject[] SYNC_OBJS;
    private final MultiBufferRingBufferBlock[] RING_BLOCKS;
    private boolean unmapped = false;

    public MultiBufferRingBuffer(Renderer renderer, int bytes, int blocks) {
        if (!renderer.getCaps().contains(Caps.BufferStorage) || !renderer.getCaps().contains(Caps.MapBuffer)) {
            throw new UnsupportedOperationException("Hardware does not support Persistently Mapped Buffers");
        }
        RENDERER = renderer;
        BYTES = bytes;
        BLOCKS = blocks;
        BUFFERS = new UntypedBuffer[blocks];
        MAPPINGS = new BufferMappingHandle[blocks];
        SYNC_OBJS = new SyncObject[blocks];
        RING_BLOCKS = new MultiBufferRingBufferBlock[blocks];
        for (int i = 0; i < blocks; i++) {
            BUFFERS[i] = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer,
                    StorageFlag.Dynamic, StorageFlag.MapWrite, StorageFlag.MapPersistent, StorageFlag.MapCoherent);
            BUFFERS[i].initialize(bytes);
            MAPPINGS[i] = BUFFERS[i].mapBuffer(MappingFlag.Write, MappingFlag.Persistent, MappingFlag.Coherent);
            SYNC_OBJS[i] = new SyncObject(renderer);
            RING_BLOCKS[i] = new MultiBufferRingBufferBlock(i, BUFFERS[i], 0, BYTES, MAPPINGS[i].getRawData());
        }
        currentBlock = blocks - 1;
    }

    @Override
    public RingBufferBlock next() {
        if (unmapped) {
            throw new UnsupportedOperationException("This RingBuffer has already been unmapped");
        }
        RING_BLOCKS[currentBlock].valid = false;
        currentBlock = (currentBlock + 1) % BLOCKS;
        SyncObject sync = SYNC_OBJS[currentBlock];
        if (sync.isPlaced()) {
            long nano = System.nanoTime();
            Signal sig;
            do {
                sig = sync.checkSignal();
            } while (sig != Signal.AlreadySignaled && sig != Signal.ConditionSatisfied); 
            long dur = System.nanoTime() - nano;
            //System.out.println("waiting for sync took nanoseconds: " + dur + " = milliseconds: "+(dur / 1000000.0));
        }

        MultiBufferRingBufferBlock block = RING_BLOCKS[currentBlock];
        block.reset();
        return block;
    }

    @Override
    public void unmap() {
        if (unmapped) {
            throw new UnsupportedOperationException("This RingBuffer has already been unmapped");
        }
        for (BufferMappingHandle mapping : MAPPINGS) {
            mapping.unmap();
        }
        for (MultiBufferRingBufferBlock block : RING_BLOCKS) {
            block.valid = false;
        }
        unmapped = true;
    }

    @Override
    public int getBlockCount() {
        return BLOCKS;
    }

    @Override
    public int getBlockSize() {
        return BYTES;
    }

    @Override
    public UntypedBuffer getBuffer() {
        return BUFFERS[currentBlock];
    }
    
    public UntypedBuffer getBuffer(int ringIndex) {
        return BUFFERS[ringIndex];
    }

    private void release() {
        if (unmapped) {
            throw new UnsupportedOperationException("This RingBuffer has already been unmapped");
        }
        SyncObject sync = SYNC_OBJS[currentBlock];
        if (sync.isPlaced()) {
            sync.recycle();
        }
        sync.place(); 
        RING_BLOCKS[currentBlock].valid = false;
    }

    private class MultiBufferRingBufferBlock implements RingBufferBlock {

        private final int INDEX;
        private final UntypedBuffer UNTYPED;
        private final int START;
        private final int SIZE;
        private final ByteBuffer BUFFER;
        private int position = 0;
        private boolean valid = false;

        private MultiBufferRingBufferBlock(int index, UntypedBuffer untyped, int start, int length, ByteBuffer buffer) {
            INDEX = index;
            UNTYPED = untyped;
            START = start;
            SIZE = length;
            BUFFER = buffer;

            position = START;
        }

        private void reset() {
            valid = true;
            position = START;
        }

        @Override
        public RingBufferBlock setPosition(int pos) {
            if (pos < 0 || pos >= SIZE) {
                throw new IllegalArgumentException("pos must be in range 0 (incl) - " + SIZE + " (excl) but has value: " + pos);
            }
            position = START + pos;
            return this;
        }

        @Override
        public RingBufferBlock putLong(long value) {
            validate(8);
            BUFFER.putLong(position, value);
            position += 8;
            return this;
        }

        @Override
        public RingBufferBlock putInt(int value) {
            validate(4);
            BUFFER.putInt(position, value);
            position += 4;
            return this;
        }

        @Override
        public RingBufferBlock putShort(short value) {
            validate(2);
            BUFFER.putShort(position, value);
            position += 2;
            return this;
        }

        @Override
        public RingBufferBlock putByte(byte value) {
            validate(1);
            BUFFER.put(position, value);
            position++;
            return this;
        }

        @Override
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

        @Override
        public RingBufferBlock putByte(byte[] values, int offset, int length) {
            validate(length);
            int pos = BUFFER.position();
            BUFFER.position(position);
            BUFFER.put(values, offset, length);
            position += length;
            BUFFER.position(pos);
            return this;
        }

        @Override
        public RingBufferBlock putFloat(float value) {
            validate(4);
            BUFFER.putFloat(position, value);
            position += 4;
            return this;
        }

        @Override
        public RingBufferBlock putDouble(double value) {
            validate(8);
            BUFFER.putDouble(position, value);
            position += 8;
            return this;
        }

        @Override
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

        @Override
        public void finish() {
            release();
        }

        @Override
        public UntypedBuffer getBuffer() {
            return UNTYPED;
        }

        @Override
        public int getOffset() {
            return START;
        }

        @Override
        public int getPosition() {
            return position;
        }
        
        @Override
        public int getIndex() {
            return INDEX;
        }

    }

}
