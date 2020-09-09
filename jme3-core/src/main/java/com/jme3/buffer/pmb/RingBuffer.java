/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer.pmb;

import com.jme3.buffer.UntypedBuffer;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public interface RingBuffer {

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
    public RingBufferBlock next();

    /**
     * Unmaps this RingBuffer. Cannot be used to upload data anymore afterwards,
     * but can still be used as source for rendering
     */
    public void unmap();

    /**
     * Returns the number of blocks this ring buffer is made of
     *
     * @return num blocks
     */
    public int getBlockCount();

    /**
     * Returns the size in bytes of a single block
     *
     * @return size of 1 bock
     */
    public int getBlockSize();

    /**
     * Returns the underlying UntypedBuffer. Should only be used to create typed
     * buffer views, not to change the buffers content.
     *
     * @return the underlying untyped buffer
     */
    public UntypedBuffer getBuffer();

    public static interface RingBufferBlock {

        /**
         * Sets the current position relative to the start of this block, that
         * is setPosition(0) will set the position to the start of the block and
         * setPosition(ringBuffer.getBlockSize() - 1) will set it to the end of
         * this block. After a call to ringBuffer.next() the position of the
         * returned RingBufferBlock will be at 0
         *
         * @param pos the position to set it to
         */
        public RingBufferBlock setPosition(int pos);

        /**
         * Puts the specified long at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putLong(long value);

        /**
         * Puts the specified int at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putInt(int value);

        /**
         * Puts the specified short at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putShort(short value);

        /**
         * Puts the specified byte at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putByte(byte value);

        /**
         * Puts the specified float at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putFloat(float value);

        /**
         * Puts the specified double at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putDouble(double value);

        /**
         * Puts the specified double at the current position
         *
         * @param value the value to set
         */
        public RingBufferBlock putByte(ByteBuffer values);

        /**
         * Puts the specified segment of the provided byte array at the current
         * position
         *
         * @param values the values to set
         * @param offset offset into the provided array
         * @param length length to read from the array
         */
        public RingBufferBlock putByte(byte[] values, int start, int length);

        /**
         * Returns the limit in bytes of this RingBufferBlock. Equal to
         * ringBuffer.getBlockSize()
         *
         * @return this blocks limit
         */
        public int getLimit();

        /**
         * Returns the offset in bytes of this RingBuffer. Its offset is the
         * number of bytes from the start of the underlying UntypedBuffer
         *
         * @return
         */
        public int getOffset();

        /**
         * Must be called once all data has been uploaded and all draw commands
         * that use that data have been made
         */
        public void finish();

        /**
         * Returns the UntypedBuffer that this RingBufferBlock is part of
         *
         * @return the untyped buffer
         */
        public UntypedBuffer getBuffer();
    }
}
