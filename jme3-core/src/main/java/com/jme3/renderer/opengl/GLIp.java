/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.renderer.opengl;

/**
 *
 * indirect parameters via GL_ARB_indirect_parameters or OpenGL46
 *
 * @author Alexander Kasigkeit
 */
public interface GLIp {

    public static final int GL_PARAMETER_BUFFER = 33006;
    public static final int GL_PARAMETER_BUFFER_BINDING = 33007;

    /**
     * https://www.khronos.org/registry/OpenGL/extensions/ARB/ARB_indirect_parameters.txt
     *
     * behaves similarly to MultiDrawArraysIndirect, except that <drawcount>
     * defines an offset (in bytes) into the buffer object bound to the
     * PARAMETER_BUFFER_ARB binding point at which a single <sizei> typed value
     * is stored, which contains the draw count. <maxdrawcount> specifies the
     * maximum number of draws that are expected to be stored in the buffer. If
     * the value stored at <drawcount> into the buffer is greater than
     * <maxdrawcount>, an implementation stop processing draws after
     * <maxdrawcount> parameter sets. <drawcount> must be a multiple of four.
     *
     * @param mode
     * @param indirect
     * @param drawcount
     * @param maxdrawcount
     * @param stride
     */
    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride);

    /**
     *
     * https://www.khronos.org/registry/OpenGL/extensions/ARB/ARB_indirect_parameters.txt
     *
     * behaves similarly to MultiDrawElementsIndirect, except that <drawcount>
     * defines an offset (in bytes) into the buffer object bound to the
     * PARAMETER_BUFFER_ARB binding point at which a single <sizei> typed value
     * is stored, which contains the draw count. <maxdrawcount> specifies the
     * maximum number of draws that are expected to be stored in the buffer. If
     * the value stored at <drawcount> into the buffer is greater than
     * <maxdrawcount>, an implementation stop processing draws after
     * <maxdrawcount> parameter sets. <drawcount> must be a multiple of four.
     *
     * @param mode
     * @param type
     * @param indirect
     * @param drawcount
     * @param maxdrawcount
     * @param stride
     */
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride);
}
