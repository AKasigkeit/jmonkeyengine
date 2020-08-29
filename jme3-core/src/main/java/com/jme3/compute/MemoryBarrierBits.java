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

    public enum MemoryBarrierBit {
        All(GL4.GL_ALL_BARRIER_BITS),
        VertexAttribArray(GL4.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT),
        ElementArray(GL4.GL_ELEMENT_ARRAY_BARRIER_BIT),
        Uniform(GL4.GL_UNIFORM_BARRIER_BIT),
        TextureFetch(GL4.GL_TEXTURE_FETCH_BARRIER_BIT),
        ShaderImageAccess(GL4.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT),
        Command(GL4.GL_COMMAND_BARRIER_BIT),
        PixelBuffer(GL4.GL_PIXEL_BUFFER_BARRIER_BIT),
        TextureUpdate(GL4.GL_TEXTURE_UPDATE_BARRIER_BIT),
        BufferUpdate(GL4.GL_BUFFER_UPDATE_BARRIER_BIT),
        QuerBuffer(GL4.GL_QUERY_BUFFER_BARRIER_BIT),
        ClientMappedBuffer(GL4.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT),
        FrameBuffer(GL4.GL_FRAMEBUFFER_BARRIER_BIT),
        TransformFeedback(GL4.GL_TRANSFORM_FEEDBACK_BARRIER_BIT),
        AtomicCounter(GL4.GL_ATOMIC_COUNTER_BARRIER_BIT),
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
     * @return 
     */
    public int getBits() {
        return BITS;
    }

}
