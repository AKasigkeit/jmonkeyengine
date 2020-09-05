/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.BufferDataUsage;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class AtomicCounterBuffer extends TypedBuffer {

    /**
     * Creates a new AtomicCounterBuffer initialized already with the specified
     * values.
     *
     * @param binding the binding that was specified in the shader
     * (AtomicCounterBuffers NEED explicit bindings)
     * @param values the initial values
     * @return a new AtomicCounterBuffer with the specified values
     */
    public static AtomicCounterBuffer createWithInitialValues(int binding, int... values) {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(MemoryMode.CpuGpu, BufferDataUsage.DynamicDraw);
        ByteBuffer data = BufferUtils.createByteBuffer(values.length * 4);
        for (int value : values) {
            data.putInt(value);
        }
        data.flip();
        buffer.initialize(data);
        return buffer.asAtomicCounterBuffer(binding);
    }

    private final int BINDING;

    protected AtomicCounterBuffer(UntypedBuffer buffer, int binding) {
        super(buffer, Type.AtomicCounterBuffer);
        BINDING = binding;
    }

    /**
     * Sets the buffer to contain the specified values. Only works if the buffer
     * was created large enough to hold the provided amount of ints
     *
     * @param values values to set
     */
    public void setValues(int... values) {
        boolean isCpuGpu = BUFFER.getMemoryMode() == MemoryMode.CpuGpu;
        ByteBuffer data;
        if (isCpuGpu) {
            if (values.length * 4 > BUFFER.getSizeOnCpu()) {
                throw new IllegalArgumentException("Underlying buffer has size of " + BUFFER.getSizeOnCpu() + " but provided data covers " + values.length * 4 + " bytes");
            }
            data = BUFFER.getCpuData();
            data.clear();
        } else {
            data = getByteBuffer(values.length * 4);
        }

        for (int value : values) {
            data.putInt(value);
        }
        data.flip();

        if (isCpuGpu) {
            BUFFER.markUpdate(0, data.limit());
        } else {
            BUFFER.updateData(data, 0); //for GPU buffers we get automatic growth for free at this point in case specified array covers more bytes
        }
    }

    /**
     * Downloads the current values of the buffer from the GPU. Only works in
     * direct mode
     *
     * @param store the array to store the values in (buffer has to be large
     * enough to fill that data)
     */
    public void getValues(int[] store) {
        if (!BUFFER.isDirect()) {
            throw new UnsupportedOperationException("reading back values with this method is only possible with buffers in direct mode");
        }
        ByteBuffer data;
        if (BUFFER.getMemoryMode() == MemoryMode.CpuGpu) {
            if (store.length * 4 > BUFFER.getSizeOnCpu()) {
                throw new IllegalArgumentException("Underlying buffer has size of " + BUFFER.getSizeOnCpu() / 4 + " but requested " + store.length + " ints");
            }
            BUFFER.downloadData(0, store.length * 4);
            data = BUFFER.getCpuData();
        } else {
            if (store.length * 4 > BUFFER.getSizeOnGpu()) {
                throw new IllegalArgumentException("Underlying buffer has size of " + BUFFER.getSizeOnGpu() / 4 + " but requested " + store.length + " ints");
            }
            data = getByteBuffer(store.length * 4);
            BUFFER.downloadData(data, 0);
        }
        for (int i = 0; i < store.length; i++) {
            store[i] = data.getInt(i * 4); //TODO verify it's not just i
        }
    }

    /**
     * Returns the binding index of this AtomicCounterBuffer
     *
     * @return binding
     */
    public int getBinding() {
        return BINDING;
    }
}
