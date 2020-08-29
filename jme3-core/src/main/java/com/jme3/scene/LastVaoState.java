/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene;

import com.jme3.renderer.IDList;

/**
 * Stores / Caches VertexArrayObject states so they dont have to be set each
 * time
 *
 * @author Alexander Kasigkeit (aka Samwise)
 */
public class LastVaoState {
    
    /**
     * USED INTERNALLY. Do not call manually. Called at startup by the renderer
     * after it has queried the GL implementation for the maximum amount of
     * VertexAttributes per VertexArrayObject. It defaults to 16 as this is the 
     * minimum guaranteed to be available by the OpenGL-specification, however
     * modern implementations tend to provide more slots, so why not make them available
     * 
     * @param maxVertexAttributes 
     */
    public static void initialize(int maxVertexAttributes) {
        maxVertAttribs = maxVertexAttributes;
    }

    private static int maxVertAttribs = 16;

    public int boundElementArray = -1;
    public final VertexAttributePointer[] vertexAttribs; 
    public final IDList vertexAttribsIndices = new IDList();
    
    public LastVaoState() {
        vertexAttribs = new VertexAttributePointer[maxVertAttribs];
        for (int i = 0; i < vertexAttribs.length; i++) {
            vertexAttribs[i] = new VertexAttributePointer();
        }
        reset();
    }
    
    /**
     * USED INTERNALLY. Do not call manually. Called by the mesh when the NativeObject
     * method reset() is called which results in a new VAO creation on the GPU, thus with a fresh state
     */
    public void reset() {
        boundElementArray = -1;
        for (VertexAttributePointer v : vertexAttribs) {
            v.bufferId = -1; 
            v.hash = 0;
            v.instanceSpan = 0;
        }
        vertexAttribsIndices.reset();
    }

    public static class VertexAttributePointer {

        public int bufferId;  
        public int instanceSpan;
        public int hash = 0;

        private VertexAttributePointer() {
        }
    }

}
