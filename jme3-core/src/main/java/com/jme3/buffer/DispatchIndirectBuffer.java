/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.BufferDataUsage;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.compute.DispatchCommand;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class DispatchIndirectBuffer extends TypedBuffer {

    /**
     * Creates a new DIspatchIndirectBuffer containing the specified
     * DispatchCommand.
     *
     * @param cmd the command to contain
     * @return the DispatchIndirectBuffer containing the DispatchCommand
     */
    public static DispatchIndirectBuffer createWithCommand(DispatchCommand cmd) {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(MemoryMode.CpuGpu, BufferDataUsage.StaticDraw);
        ByteBuffer data = BufferUtils.createByteBuffer(3 * 4); // 3 ints 4 bytes each
        data.putInt(cmd.getNumGroupsX()).putInt(cmd.getNumGroupsY()).putInt(cmd.getNumGroupsZ()).flip();
        buffer.initialize(data);
        return buffer.asDispatchIndirectBuffer();
    }

    protected DispatchIndirectBuffer(UntypedBuffer buffer) {
        super(buffer, Type.DispatchIndirectBuffer);
    }

    /**
     * Uploads the provided DispatchCommand into the buffer at the specified
     * offset in bytes (thus, one DispatchCommand covers 12 bytes (3 ints with 4
     * bytes each))
     *
     * @param cmd DispatchCommand to set
     * @param offset offset where to set it (in bytes)
     */
    public void setDispatchCommand(DispatchCommand cmd, int offset) {
        setDispatchCommand(cmd.getNumGroupsX(), cmd.getNumGroupsY(), cmd.getNumGroupsZ(), offset);
    }

    /**
     * Uploads the provided group sizes into the buffer at the specified offset
     * in bytes (thus, one DispatchCommand covers 12 bytes (3 ints with 4 bytes
     * each))
     *
     * @param numGroupsX groups counts in x dimension
     * @param numGroupsY groups counts in y dimension
     * @param numGroupsZ groups counts in z dimension
     * @param offset offset where to set it (in bytes)
     */
    public void setDispatchCommand(int numGroupsX, int numGroupsY, int numGroupsZ, int offset) {
        boolean isCpuGpu = BUFFER.getMemoryMode() == MemoryMode.CpuGpu;
        ByteBuffer data;
        if (isCpuGpu) {
            if (offset + 12 > BUFFER.getSizeOnCpu()) {
                throw new UnsupportedOperationException("buffer is not large enough to insert data at the specified offset");
            }
            data = BUFFER.getCpuData();
            data.clear().position(offset);
            data.putInt(numGroupsX).putInt(numGroupsY).putInt(numGroupsZ).flip();
            BUFFER.markUpdate(offset, 12);
        } else {
            data = getByteBuffer(12);
            data.putInt(numGroupsX).putInt(numGroupsY).putInt(numGroupsZ).flip();
            BUFFER.updateData(data, offset);
        }
    }

}
