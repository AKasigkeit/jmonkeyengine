/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.conditional.GpuQuery;

/**
 *
 * @author Alexander Kasigkeit
 */
public class QueryBuffer extends TypedBuffer {

    /**
     * Creates a new Gpu-Only QueryBuffer big enough to hold the provided amount
     * of GpuQuery results. If bits64 is true, a result is considered to be 8
     * bytes, if it is false, a result is considered to be 4 bytes in size
     *
     * @param numQueries the amount of GpuQuery results the buffer needs to be
     * able to store
     * @param bits64 true to use unsigned long, false for unsigned int results
     * @return the new QueryBuffer
     */
    public static QueryBuffer createWithSize(int numQueries, boolean bits64) {
        UntypedBuffer buffer = UntypedBuffer.createNewStorageLazy(UntypedBuffer.MemoryMode.GpuOnly, StorageFlag.Dynamic);
        buffer.initialize(numQueries * (bits64 ? 8 : 4));
        return buffer.asQueryBuffer();
    }

    protected QueryBuffer(UntypedBuffer buffer) {
        super(buffer, Type.QueryBuffer);
    }

    /**
     * Stores the result of the provided query in this QueryBuffer at the
     * specified offset. Will wait in case the result is not yet available and
     * store 64 bits of data (unsigned long)
     *
     * @param query the query to store the result of
     * @param offset the offset in bytes into the buffer to store the result
     */
    public void storeResult(GpuQuery query, int offset) {
        storeResult(query, offset, true, true);
    }

    /**
     * Stores the result of the provided query in this QueryBuffer at the
     * specified offset. If bits64 is true, an unsigned long will be written,
     * otherwise an unsigned int will be written. If wait is true, the GPU will
     * wait for the result to become available, otherwise the buffers content
     * will not be changed in case the result is not available right away.
     *
     * @param query the query to store the result of
     * @param offset offset in bytes into the buffer to store the result
     * @param bits64 true to write unsigned long, false for unsigned int
     * @param wait true to wait for the result in case it is not available right
     * away
     */
    public void storeResult(GpuQuery query, int offset, boolean bits64, boolean wait) {
        int sizeNeeded = offset + (bits64 ? 8 : 4);
        if (sizeNeeded > BUFFER.getSizeOnGpu()) {
            throw new IllegalArgumentException("this QueryBuffer is not large enough to store a result at the provided offset");
        }
        if (!BUFFER.isDirect()) {
            throw new UnsupportedOperationException("to store a result in a QueryBuffer, the underlying UntypedBuffer has to be in direct mode");
        }
        BUFFER.getRenderer().getQueryResult(this, query, offset, bits64, wait);
    }

    /**
     * Stores the provided GpuQuery result's availability at the specified
     * offset. In case the result is available 1 will be written, otherwise 0,
     * but in any case will the result cover 4 bytes
     *
     * @param query the query to store the results availability of
     * @param offset the offset in bytes into the buffer to store the
     * availability int
     */
    public void storeResultsAvailability(GpuQuery query, int offset) {
        int sizeNeeded = offset + 4;
        if (sizeNeeded > BUFFER.getSizeOnGpu()) {
            throw new IllegalArgumentException("this QueryBuffer is not large enough to store a result's availability at the provided offset");
        }
        if (!BUFFER.isDirect()) {
            throw new UnsupportedOperationException("to store a result in a QueryBuffer, the underlying UntypedBuffer has to be in direct mode");
        }
        BUFFER.getRenderer().getQueryResultAvailability(this, query, offset);
    }
}
