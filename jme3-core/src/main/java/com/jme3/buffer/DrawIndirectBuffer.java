/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class DrawIndirectBuffer extends TypedBuffer {

    public static enum DrawIndirectMode {
        /**
         * The mesh that is holding the VertexData to draw is NOT making use of
         * a VertexBuffer of Type IndexBuffer (it does NOT use shared vertices).
         * Draw commands for such meshes take 4 ints ->
         * DrawArraysIndirectCommand
         */
        Draw,
        /**
         * The mesh that is holding the VertexData to draw is making use of a
         * Vertex Buffer of Type IndexBuffer (it uses shared vertices). Draw
         * commands for such meshes take 5 ints -> DrawElementsIndirectCommand
         */
        DrawIndices
    }

    public static class DrawCommand {

        private final int count;
        private final int instanceCount;
        private final int first;
        private final int baseInstance;

        /**
         * Creates a new DrawCommand
         *
         * @param count num vertices to draw
         * @param instanceCount num instances to draw
         * @param first offset into vertex data
         * @param baseInstance offset into instance data
         * @return new DrawCommand
         */
        public static DrawCommand withValues(int count, int instanceCount, int first, int baseInstance) {
            //exists purely because IDEs tend to show javadoc for methods more than constructors
            return new DrawCommand(count, instanceCount, first, baseInstance);
        }

        public DrawCommand(int count, int instanceCount, int first, int baseInstance) {
            this.count = count;
            this.instanceCount = instanceCount;
            this.first = first;
            this.baseInstance = baseInstance;
        }

        /**
         * Number of Vertices to draw
         *
         * @return num verts to draw
         */
        public int getCount() {
            return count;
        }

        /**
         * Number of instances to draw
         *
         * @return num instances to draw
         */
        public int getInstanceCount() {
            return instanceCount;
        }

        /**
         * Offsets into Vertices to start from
         *
         * @return offset into verts
         */
        public int getFirst() {
            return first;
        }

        /**
         * Offset to use into instanced data
         *
         * @return offset into instances
         */
        public int getBaseInstance() {
            return baseInstance;
        }
    }

    public static class DrawIndicesCommand {

        private final int count;
        private final int instanceCount;
        private final int firstIndex;
        private final int baseVertex;
        private final int baseInstance;

        /**
         * Creates a new DrawIndicesCommand
         *
         * @param count num indices to draw
         * @param instanceCount num instances to draw
         * @param firstIndex offset into indices
         * @param baseVertex offset into vertex data
         * @param baseInstance offset into intance data
         * @return new DrawIndicesCommand
         */
        public static DrawIndicesCommand withValues(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
            //exists purely because IDEs tend to show javadoc for methods more than constructors
            return new DrawIndicesCommand(count, instanceCount, firstIndex, baseVertex, baseInstance);
        }

        public DrawIndicesCommand(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
            this.count = count;
            this.instanceCount = instanceCount;
            this.firstIndex = firstIndex;
            this.baseVertex = baseVertex;
            this.baseInstance = baseInstance;
        }

        /**
         * Number of indices to draw
         *
         * @return num indices to draw
         */
        public int getCount() {
            return count;
        }

        /**
         * Number of instances to draw
         *
         * @return num instances to draw
         */
        public int getInstanceCount() {
            return instanceCount;
        }

        /**
         * Offsets into IndexBuffer to start from
         *
         * @return offset into indices
         */
        public int getFirstIndex() {
            return firstIndex;
        }

        /**
         * Offset to add to the index read from the index buffer before fetching
         * corresponding data from VertexBuffers
         *
         * @return the base vertex
         */
        public int getBaseVertex() {
            return baseVertex;
        }

        /**
         * Offset to use into instanced data
         *
         * @return offset into instances
         */
        public int getBaseInstance() {
            return baseInstance;
        }

    }

    /**
     * Creates a new DrawIndirectBuffer with the specified Commands. This buffer
     * will contain DrawCommands which are for meshes that DO NOT contain
     * IndexBuffers. In case they do contain IndexBuffers, the rendering will
     * not make use of them which will probably result in artifacts
     *
     * @param cmds the commands to set
     * @return a new DrawIndirectBuffer for Meshes without IndexBuffers
     * containing the provided DrawCommands
     */
    public static DrawIndirectBuffer createWithCommands(DrawCommand... cmds) {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticDraw);
        ByteBuffer data = BufferUtils.createByteBuffer(cmds.length * 4 * 4); //4 ints per command with 4 bytes each
        for (DrawCommand cmd : cmds) {
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirst()).putInt(cmd.getBaseInstance());
        }
        data.flip();
        buffer.initialize(data);
        return buffer.asDrawIndirectBuffer(DrawIndirectMode.Draw);

    }

    /**
     * Creates a new DrawIndirectBuffer with the specified Commands. This buffer
     * will contain DrawIndirectCommands which are for meshes that contain
     * IndexBuffers. in case they do not contain an IndexBuffer, an Exception
     * will be thrown when trying to render using this DrawIndirectBuffer.
     *
     * @param cmds the commands to set
     * @return a new DrawIndirectBuffer for Meshes with IndexBuffers containing
     * the provided DrawIndicesCommands
     */
    public static DrawIndirectBuffer createWithCommands(DrawIndicesCommand... cmds) {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticDraw);
        ByteBuffer data = BufferUtils.createByteBuffer(cmds.length * 5 * 4); //5 ints per command with 4 bytes each
        for (DrawIndicesCommand cmd : cmds) {
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirstIndex()).putInt(cmd.getBaseVertex()).putInt(cmd.getBaseInstance());
        }
        data.flip();
        buffer.initialize(data);
        return buffer.asDrawIndirectBuffer(DrawIndirectMode.DrawIndices);
    }

    private final DrawIndirectMode MODE;

    protected DrawIndirectBuffer(UntypedBuffer buffer, DrawIndirectMode mode) {
        super(buffer, Type.DrawIndirectBuffer);
        MODE = mode;
    }

    public void setCommand(DrawCommand cmd, int offset) {
        if (MODE == DrawIndirectMode.DrawIndices) {
            throw new UnsupportedOperationException("Cannot set DrawCommands for DrawIndirectBuffer which is in DrawIndirectMode.DrawIndices (need to use DrawIndicesCommands)");
        }
        boolean isCpuGpu = BUFFER.getMemoryMode() == MemoryMode.CpuGpu;
        ByteBuffer data;
        if (isCpuGpu) {
            if (offset + 16 > BUFFER.getSizeOnCpu()) {
                throw new IllegalArgumentException("buffer is not large enough to insert DrawCommand at specified offset");
            }
            data = BUFFER.getCpuData();
            data.clear().position(offset);
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirst()).putInt(cmd.getBaseInstance()).flip();
            BUFFER.markUpdate(offset, 16);
        } else {
            data = getByteBuffer(16);
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirst()).putInt(cmd.getBaseInstance()).flip();
            BUFFER.updateData(data, offset);
        }
    }

    public void setCommand(DrawIndicesCommand cmd, int offset) {
        if (MODE == DrawIndirectMode.Draw) {
            throw new UnsupportedOperationException("Cannot set DrawIndicesCommands for DrawIndirectBuffer which is in DrawIndirectMode.Draw (need to use DrawCommands)");
        }
        boolean isCpuGpu = BUFFER.getMemoryMode() == MemoryMode.CpuGpu;
        ByteBuffer data;
        if (isCpuGpu) {
            if (offset + 20 > BUFFER.getSizeOnCpu()) {
                throw new IllegalArgumentException("buffer is not large enough to insert DrawCommand at specified offset");
            }
            data = BUFFER.getCpuData();
            data.clear().position(offset);
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirstIndex()).putInt(cmd.getBaseVertex()).putInt(cmd.getBaseInstance());
            BUFFER.markUpdate(offset, 20);
        } else {
            data = getByteBuffer(20);
            data.putInt(cmd.getCount()).putInt(cmd.getInstanceCount()).putInt(cmd.getFirstIndex()).putInt(cmd.getBaseVertex()).putInt(cmd.getBaseInstance());
            BUFFER.updateData(data, offset);
        }
    }

    public DrawIndirectMode getDrawMode() {
        return MODE;
    }

}
