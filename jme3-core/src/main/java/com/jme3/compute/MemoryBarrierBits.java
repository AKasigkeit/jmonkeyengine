/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.compute;

import com.jme3.renderer.opengl.GL4;

/**
 *
 *
 * @author Alexander Kasigkeit
 */
public class MemoryBarrierBits {

    /**
     * Descriptions taken directly from
     * https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml
     */
    public enum MemoryBarrierBit {
        /**
         * This is a special value that inserts memory barriers for all
         * supported types
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        All(GL4.GL_ALL_BARRIER_BITS),
        /**
         * If set, vertex data sourced from buffer objects after the barrier
         * will reflect data written by shaders prior to the barrier. The set of
         * buffer objects affected by this bit is derived from the buffer object
         * bindings used for generic vertex attributes derived from the
         * GL_VERTEX_ATTRIB_ARRAY_BUFFER bindings.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        VertexAttribArray(GL4.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT),
        /**
         * If set, vertex array indices sourced from buffer objects after the
         * barrier will reflect data written by shaders prior to the barrier.
         * The buffer objects affected by this bit are derived from the
         * GL_ELEMENT_ARRAY_BUFFER binding.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ElementArray(GL4.GL_ELEMENT_ARRAY_BARRIER_BIT),
        /**
         * Shader uniforms sourced from buffer objects after the barrier will
         * reflect data written by shaders prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        Uniform(GL4.GL_UNIFORM_BARRIER_BIT),
        /**
         * Texture fetches from shaders, including fetches from buffer object
         * memory via buffer textures, after the barrier will reflect data
         * written by shaders prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TextureFetch(GL4.GL_TEXTURE_FETCH_BARRIER_BIT),
        /**
         * Memory accesses using shader image load, store, and atomic built-in
         * functions issued after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, image stores and atomics
         * issued after the barrier will not execute until all memory accesses
         * (e.g., loads, stores, texture fetches, vertex fetches) initiated
         * prior to the barrier complete.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ShaderImageAccess(GL4.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT),
        /**
         * Command data sourced from buffer objects by Draw*Indirect commands
         * after the barrier will reflect data written by shaders prior to the
         * barrier. The buffer objects affected by this bit are derived from the
         * GL_DRAW_INDIRECT_BUFFER binding.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        Command(GL4.GL_COMMAND_BARRIER_BIT),
        /**
         * Reads and writes of buffer objects via the GL_PIXEL_PACK_BUFFER and
         * GL_PIXEL_UNPACK_BUFFER bindings (via glReadPixels, glTexSubImage1D,
         * etc.) after the barrier will reflect data written by shaders prior to
         * the barrier. Additionally, buffer object writes issued after the
         * barrier will wait on the completion of all shader writes initiated
         * prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        PixelBuffer(GL4.GL_PIXEL_BUFFER_BARRIER_BIT),
        /**
         * Writes to a texture via glTex(Sub)Image*, glCopyTex(Sub)Image*,
         * glCompressedTex(Sub)Image*, and reads via glGetTexImage after the
         * barrier will reflect data written by shaders prior to the barrier.
         * Additionally, texture writes from these commands issued after the
         * barrier will not execute until all shader writes initiated prior to
         * the barrier complete
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TextureUpdate(GL4.GL_TEXTURE_UPDATE_BARRIER_BIT),
        /**
         * Reads or writes via glBufferSubData, glCopyBufferSubData, or
         * glGetBufferSubData, or to buffer object memory mapped by glMapBuffer
         * or glMapBufferRange after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, writes via these commands
         * issued after the barrier will wait on the completion of any shader
         * writes to the same memory initiated prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        BufferUpdate(GL4.GL_BUFFER_UPDATE_BARRIER_BIT),
        /**
         * Writes of buffer objects via the GL_QUERY_BUFFER binding after the
         * barrier will reflect data written by shaders prior to the barrier.
         * Additionally, buffer object writes issued after the barrier will wait
         * on the completion of all shader writes initiated prior to the
         * barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        QuerBuffer(GL4.GL_QUERY_BUFFER_BARRIER_BIT),
        /**
         * Access by the client to persistent mapped regions of buffer objects
         * will reflect data written by shaders prior to the barrier. Note that
         * this may cause additional synchronization operations.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ClientMappedBuffer(GL4.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT),
        /**
         * Reads and writes via framebuffer object attachments after the barrier
         * will reflect data written by shaders prior to the barrier.
         * Additionally, framebuffer writes issued after the barrier will wait
         * on the completion of all shader writes issued prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        FrameBuffer(GL4.GL_FRAMEBUFFER_BARRIER_BIT),
        /**
         * <b>Currently not supported by the engine</b> Writes via transform
         * feedback bindings after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, transform feedback writes
         * issued after the barrier will wait on the completion of all shader
         * writes issued prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TransformFeedback(GL4.GL_TRANSFORM_FEEDBACK_BARRIER_BIT),
        /**
         * Accesses to atomic counters after the barrier will reflect writes
         * prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        AtomicCounter(GL4.GL_ATOMIC_COUNTER_BARRIER_BIT),
        /**
         * Accesses to shader storage blocks after the barrier will reflect
         * writes prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ShaderStorage(GL4.GL_SHADER_STORAGE_BARRIER_BIT);

        private final int GL_CONST;

        private MemoryBarrierBit(int cons) {
            GL_CONST = cons;
        }

        public int getGlConstant() {
            return GL_CONST;
        }
    }

    /**
     * Creates a MemoryBarrierBits instance from the given MemoryBarrierBit
     * flags
     *
     * @param bits the barrier bits to combine
     * @return the MemoryBarrierBits instance representing a barrier with
     * specified flags
     */
    public static MemoryBarrierBits from(MemoryBarrierBit... bits) {
        int b = 0;
        if (bits != null) {
            for (MemoryBarrierBit bit : bits) {
                b |= bit.getGlConstant();
            }
        }
        return new MemoryBarrierBits(b);
    }

    public static final MemoryBarrierBits NONE = from();
    public static final MemoryBarrierBits ALL = from(MemoryBarrierBit.All);
    public static final MemoryBarrierBits SHADER_IMAGE_ACCESS = from(MemoryBarrierBit.ShaderImageAccess);
    public static final MemoryBarrierBits SHADER_STORAGE = from(MemoryBarrierBit.ShaderStorage);
    public static final MemoryBarrierBits CLIENT_MAPPED_BUFFER = from(MemoryBarrierBit.ClientMappedBuffer);

    private final int BITS;

    private MemoryBarrierBits(int bits) {
        BITS = bits;
    }

    /**
     * Returns the bits representing the specified barriers
     *
     * @return
     */
    public int getBits() {
        return BITS;
    }

}
