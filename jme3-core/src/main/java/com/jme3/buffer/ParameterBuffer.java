/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.util.BufferUtils;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ParameterBuffer extends TypedBuffer {

    public static ParameterBuffer createNew(int numCounts) {
        UntypedBuffer buffer = UntypedBuffer.createNewStorageLazy(MemoryMode.CpuGpu, StorageFlag.Dynamic);
        buffer.initialize(BufferUtils.createByteBuffer(numCounts * 4));
        return buffer.asParameterBuffer();
    }

    public ParameterBuffer(UntypedBuffer buffer) {
        super(buffer, Type.ParameterBuffer);
    }

}
