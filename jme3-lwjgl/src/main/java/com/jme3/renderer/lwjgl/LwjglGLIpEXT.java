/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.renderer.lwjgl;

import com.jme3.renderer.opengl.GLIp;
import org.lwjgl.opengl.ARBIndirectParameters;

/**
 *
 * @author Alexander Kasigkeit
 */
public class LwjglGLIpEXT implements GLIp {

    @Override
    public void glMultiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride) {
        ARBIndirectParameters.glMultiDrawArraysIndirectCountARB(mode, indirect, drawcount, maxdrawcount, stride);
    }

    @Override
    public void glMultiDrawElementsIndirectCount(int mode, int type, long indirect, long drawcount, int maxdrawcount, int stride) {
        ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(mode, type, indirect, drawcount, maxdrawcount, stride);
    }

}
