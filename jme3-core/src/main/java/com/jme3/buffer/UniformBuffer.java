/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.BufferDataUsage;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.shader.layout.BlockFieldLayout;
import com.jme3.shader.layout.BufferWriterUtils;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Alexander Kasigkeit
 */
public class UniformBuffer extends FieldBuffer {

    /**
     * Creates a new UniformBuffer. It will automatically layout the data
     * accoring to the layout specified in the shader as long as the names and
     * varTypes match
     *
     * @return a new UniformBuffer in autolayout mode
     */
    public static UniformBuffer createNewAutolayout() {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(MemoryMode.CpuGpu, BufferDataUsage.StaticRead);
        buffer.initialize(BufferUtils.createByteBuffer(0)); //empty for now, will adjust automatically
        //buffer.initialize(1);
        return buffer.asUniformBuffer(null);
    }

    /**
     * Creates a new UniformBuffer. It will use the provided FieldBufferWriter
     * to write the fields into the buffer, GL is not queried for the layout and
     * it is up to the user to make sure it matches the layout in the shader
     *
     * @param writer the writer to use
     * @return a new UniformBuffer
     */
    public static UniformBuffer createNewManualLayout(FieldBufferWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("for manual layouts, writer cannot be null");
        }
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(MemoryMode.GpuOnly, BufferDataUsage.StaticRead);
        //buffer.initialize(BufferUtils.createByteBuffer(0)); //empty for now, will adjust automatically
        buffer.initialize(Math.max(1, writer.getBufferSize()));
        return buffer.asUniformBuffer(writer);
    }

    protected UniformBuffer(UntypedBuffer buffer, FieldBufferWriter writer) {
        super(buffer, Type.UniformBuffer, writer);
    }

    @Override
    protected void flushFieldUpdatesSpecific() {
        boolean isCpuGpu = BUFFER.getMemoryMode() == UntypedBuffer.MemoryMode.CpuGpu;
        boolean isTempBuffer = false;
        ByteBuffer data;
        if (isCpuGpu) {
            if (layout.getSize() > BUFFER.getSizeOnCpu()) {
                data = ByteBuffer.allocate(layout.getSize()).order(ByteOrder.nativeOrder()); //will be destroyed further down again
                isTempBuffer = true;
            } else {
                data = BUFFER.getCpuData();
                data.clear();
            }
        } else {
            data = getByteBuffer(layout.getSize());
        }

        int bytesWritten = 0, bytesWrittenTotal = 0;
        int firstByte = Integer.MAX_VALUE, lastByte = Integer.MIN_VALUE;
        long start = System.nanoTime(), dur;

        for (BlockFieldLayout lay : layout.getFieldLayouts()) {
            //the layouts name might have [0] appended
            String name = lay.getName();
            if (name.endsWith("[0]")) {
                name = name.substring(0, name.length() - 3);
            }

            BlockField field = fieldsMap.get(name);
            if (field == null) {
                continue;
            }

            data.position(lay.getOffset());

            bytesWritten = BufferWriterUtils.writeField(data, lay.getArrayStride(), lay.getMatrixStride(), field.getBlockVarType(), field.getValue());
            bytesWrittenTotal += bytesWritten;
            //System.out.println("writing at offset: " + lay.getOffset() + ": " + field.getValue() + " -> " + bytesWritten + " bytes");

            if (lay.getOffset() < firstByte) {
                firstByte = lay.getOffset();
            }
            if (lay.getOffset() + bytesWritten > lastByte) {
                lastByte = lay.getOffset() + bytesWritten;
            }
        }
        data.flip();

        dur = System.nanoTime() - start;
        //System.out.println("writing " + fields.size() + " fields took " + (dur / 1000000.0) + " ms, " + bytesWrittenTotal + " bytes were written. The blocks total size is: " + layout.getSize() + ", started at " + firstByte + " and up to " + lastByte);

        if (isCpuGpu) {
            if (isTempBuffer) {
                data.position(firstByte).limit(lastByte);
                BUFFER.updateData(data, firstByte);
            } else {
                BUFFER.markUpdate(firstByte, lastByte - firstByte);
            }
        } else {
            data.position(firstByte).limit(lastByte);
            BUFFER.updateData(data, firstByte);
        }
    }

}
