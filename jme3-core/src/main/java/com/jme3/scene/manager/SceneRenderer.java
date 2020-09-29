/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.manager;

import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort; 
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Spatial;

/**
 * Outsources the functionality of flattening the scenegraph into the
 * RenderQueue from the Renderer to this interface to provide easy way of custom
 * implementation
 *
 * @author Alexander Kasigkeit
 */
public interface SceneRenderer {

    public void renderScene(Spatial scene, ViewPort vp);
    
    public void postRenderBucket(RenderQueue queue, Bucket bucket, RenderManager rm, ViewPort vp, Camera cam);

}
