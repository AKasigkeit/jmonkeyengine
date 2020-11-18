/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.compute;

/**
 *
 *
 * @author Alexander Kasigkeit
 */
public interface MemoryBarrier {

    /**
     * Descriptions taken directly from
     * https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml
     */
    public enum Flag {
        /**
         * This is a special value that inserts memory barriers for all
         * supported types
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        All,
        /**
         * If set, vertex data sourced from buffer objects after the barrier
         * will reflect data written by shaders prior to the barrier. The set of
         * buffer objects affected by this bit is derived from the buffer object
         * bindings used for generic vertex attributes derived from the
         * GL_VERTEX_ATTRIB_ARRAY_BUFFER bindings.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        VertexAttribArray,
        /**
         * If set, vertex array indices sourced from buffer objects after the
         * barrier will reflect data written by shaders prior to the barrier.
         * The buffer objects affected by this bit are derived from the
         * GL_ELEMENT_ARRAY_BUFFER binding.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ElementArray,
        /**
         * Shader uniforms sourced from buffer objects after the barrier will
         * reflect data written by shaders prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        Uniform,
        /**
         * Texture fetches from shaders, including fetches from buffer object
         * memory via buffer textures, after the barrier will reflect data
         * written by shaders prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TextureFetch,
        /**
         * Memory accesses using shader image load, store, and atomic built-in
         * functions issued after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, image stores and atomics
         * issued after the barrier will not execute until all memory accesses
         * (e.g., loads, stores, texture fetches, vertex fetches) initiated
         * prior to the barrier complete.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ShaderImageAccess,
        /**
         * Command data sourced from buffer objects by Draw*Indirect commands
         * after the barrier will reflect data written by shaders prior to the
         * barrier. The buffer objects affected by this bit are derived from the
         * GL_DRAW_INDIRECT_BUFFER binding.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        Command,
        /**
         * Reads and writes of buffer objects via the GL_PIXEL_PACK_BUFFER and
         * GL_PIXEL_UNPACK_BUFFER bindings (via glReadPixels, glTexSubImage1D,
         * etc.) after the barrier will reflect data written by shaders prior to
         * the barrier. Additionally, buffer object writes issued after the
         * barrier will wait on the completion of all shader writes initiated
         * prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        PixelBuffer,
        /**
         * Writes to a texture via glTex(Sub)Image*, glCopyTex(Sub)Image*,
         * glCompressedTex(Sub)Image*, and reads via glGetTexImage after the
         * barrier will reflect data written by shaders prior to the barrier.
         * Additionally, texture writes from these commands issued after the
         * barrier will not execute until all shader writes initiated prior to
         * the barrier complete
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TextureUpdate,
        /**
         * Reads or writes via glBufferSubData, glCopyBufferSubData, or
         * glGetBufferSubData, or to buffer object memory mapped by glMapBuffer
         * or glMapBufferRange after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, writes via these commands
         * issued after the barrier will wait on the completion of any shader
         * writes to the same memory initiated prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        BufferUpdate,
        /**
         * Writes of buffer objects via the GL_QUERY_BUFFER binding after the
         * barrier will reflect data written by shaders prior to the barrier.
         * Additionally, buffer object writes issued after the barrier will wait
         * on the completion of all shader writes initiated prior to the
         * barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        QueryBuffer,
        /**
         * Access by the client to persistent mapped regions of buffer objects
         * will reflect data written by shaders prior to the barrier. Note that
         * this may cause additional synchronization operations.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ClientMappedBuffer,
        /**
         * Reads and writes via framebuffer object attachments after the barrier
         * will reflect data written by shaders prior to the barrier.
         * Additionally, framebuffer writes issued after the barrier will wait
         * on the completion of all shader writes issued prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        FrameBuffer,
        /**
         * <b>Currently not supported by the engine</b> Writes via transform
         * feedback bindings after the barrier will reflect data written by
         * shaders prior to the barrier. Additionally, transform feedback writes
         * issued after the barrier will wait on the completion of all shader
         * writes issued prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        TransformFeedback,
        /**
         * Accesses to atomic counters after the barrier will reflect writes
         * prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        AtomicCounter,
        /**
         * Accesses to shader storage blocks after the barrier will reflect
         * writes prior to the barrier.
         * (https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml)
         */
        ShaderStorage;
    }

    public boolean has(Flag flag);
} 
