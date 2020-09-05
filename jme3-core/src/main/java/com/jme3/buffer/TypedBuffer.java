/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.renderer.Caps;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class TypedBuffer {

    public enum Type {
        /**
         * Uniform Buffer Object (UBO). Can be used to upload several Uniforms
         * at once and to share Uniforms between shaders while only updating
         * them once. Limited in size to 16KB. To share buffer between shaders,
         * use Layout.std140 or Layout.shared. Does not support Layout.std430
         */
        UniformBuffer(Caps.UniformBufferObject, GL3.GL_UNIFORM_BUFFER),
        /**
         * Shader Storage Buffer Object (SSBO). Can be used for same purposes as
         * UniformBufferObjects. Have a guaranteed minimum size of 128MB (MB as
         * opposed to UBO's KB) but are likely to be slower in access. Can use
         * all layouts including Layout.std430 which is more memory efficient in
         * some cases which might somewhat counter the slower memory access.
         */
        ShaderStorageBuffer(Caps.ShaderStorageBufferObject, GL4.GL_SHADER_STORAGE_BUFFER),
        /**
         * Atomic Counter Buffer Object. Can be used in shaders to have atomic
         * counters obviously. Each counter is an unsigned 32 bit int.
         */
        AtomicCounterBuffer(Caps.OpenGL44, GL4.GL_ATOMIC_COUNTER_BUFFER),
        /**
         * Dispatch Indirect Buffer. Can be used to hold work group counts of
         * compute shader dispatches. Might improve performance over using
         * direct dispatching in some cases because it knows the sizes
         * beforehand. Skips some checks which might result in application
         * termination in case eg the specified sizes in the buffer exceed the
         * implementations maximum
         */
        DispatchIndirectBuffer(Caps.OpenGL44, GL4.GL_DISPATCH_INDIRECT_BUFFER),
        /**
         * Draw Indirect Buffer. Can be used to send one or more draw commands
         * to the gpu at once. Look into documentation of DrawIndirectBuffer for
         * more details explanation.1
         */
        DrawIndirectBuffer(Caps.OpenGL44, GL4.GL_DRAW_INDIRECT_BUFFER),
        /**
         * Query Buffer. Can be used to store QueryObject results so they dont
         * have to be read back to the CPU. Can then be used in shaders to use
         * lower tessellation / cheaper lighting / otherwise less performance
         * heavy computations if the fragments drawn fall below some threshold.
         */
        QueryBuffer(Caps.OpenGL44, GL4.GL_QUERY_BUFFER);

        private final Caps CAPS;
        private final int GL_CONST;

        private Type(Caps caps, int con) {
            CAPS = caps;
            GL_CONST = con;
        }

        public Caps getCaps() {
            return CAPS;
        }

        public int getGlConstant() {
            return GL_CONST;
        }
    }
    
    protected final Type TYPE;
    protected final UntypedBuffer BUFFER;
    private ByteBuffer dataBuffer = null;
    
    protected TypedBuffer(UntypedBuffer buffer, Type type) {
        TYPE = type;
        BUFFER = buffer;
    }
    
    public Type getType() {
        return TYPE;
    }
    
    public UntypedBuffer getUntypedBuffer() {
        return BUFFER;
    }
    
    //used by subclasses to write data into buffers via convenience methods in case underlying untyped buffer is gpu only
    protected ByteBuffer getByteBuffer(int size) {
        //TODO rather make it static and threadlocal for direct rendering
        if (dataBuffer != null && dataBuffer.capacity() < size) {
            BufferUtils.destroyDirectBuffer(dataBuffer);
            dataBuffer = null;
        }
        if (dataBuffer == null) {
            dataBuffer = BufferUtils.createByteBuffer(size);
            return dataBuffer;
        }
        dataBuffer.clear();
        return dataBuffer;
    }
}
