/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader;

import com.jme3.buffer.UniformBuffer;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.system.Timer;

/**
 *
 * UniformBuffer that contains all kinds of data that is related to the Camera
 *
 * @author Alexander Kasigkeit
 */
public class CameraUniformBuffer {

    private final Camera CAM; 
    private final UniformBuffer BUFFER;
    private Timer timer = null;
    private float time, tpf;
    private int lastUpdate = 0;

    private final Matrix4f invViewMatrix = new Matrix4f();
    private final Matrix4f invProjMatrix = new Matrix4f();
    private final Matrix4f invViewProjMatrix = new Matrix4f();

    public CameraUniformBuffer(Camera cam) {
        CAM = cam; 
        BUFFER = UniformBuffer.createNewAutolayout();
    }

    protected void setLastUpdate(int frame) {
        lastUpdate = frame;
    }

    protected boolean needsUpdate(int currentFrame) {
        return lastUpdate < currentFrame;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void updateState(Matrix4f viewMatrix, Matrix4f projMatrix, Matrix4f viewProjMatrix) {
        //System.out.println("going to update state for camera buffer: " + BUFFER.getUntypedBuffer().getId() + " with cam " + CAM.getName() + " = " + CAM);
        time = timer == null ? 0f : timer.getTimeInSeconds();
        tpf = timer == null ? 0f : timer.getTimePerFrame();
        invViewMatrix.set(viewMatrix).invertLocal();
        invProjMatrix.set(projMatrix).invertLocal();
        invViewProjMatrix.set(viewProjMatrix).invertLocal();

        //mat4
        BUFFER.setField("cam_viewMatrix", viewMatrix);
        BUFFER.setField("cam_projectionMatrix", projMatrix);
        BUFFER.setField("cam_viewProjectionMatrix", viewProjMatrix);
        BUFFER.setField("cam_viewMatrixInverse", invViewMatrix);
        BUFFER.setField("cam_projectionMatrixInverse", invProjMatrix);
        BUFFER.setField("cam_viewProjectionMatrixInverse", invViewProjMatrix);
        //vec4
        BUFFER.setField("cam_rotation", CAM.getRotation());
        //vec3
        BUFFER.setField("cam_position", CAM.getLocation());
        BUFFER.setField("cam_height", (float) CAM.getHeight()); //here to reduce padding
        BUFFER.setField("cam_direction", CAM.getDirection());
        BUFFER.setField("cam_width", (float) CAM.getWidth()); //here to reduce padding
        BUFFER.setField("cam_left", CAM.getLeft());
        BUFFER.setField("cam_frustumTop", CAM.getFrustumTop()); //here to reduce padding
        BUFFER.setField("cam_up", CAM.getUp());
        BUFFER.setField("cam_frustumBottom", CAM.getFrustumBottom()); //here to reduce padding
        //float
        BUFFER.setField("cam_frustumLeft", CAM.getFrustumLeft());
        BUFFER.setField("cam_frustumRight", CAM.getFrustumRight());
        BUFFER.setField("cam_frustumNear", CAM.getFrustumNear());
        BUFFER.setField("cam_frustumFar", CAM.getFrustumFar());
        BUFFER.setField("cam_viewPortLeft", CAM.getViewPortLeft());
        BUFFER.setField("cam_viewPortRight", CAM.getViewPortRight());
        BUFFER.setField("cam_viewPortTop", CAM.getViewPortTop());
        BUFFER.setField("cam_viewPortBottom", CAM.getViewPortBottom());

        BUFFER.setField("cam_time", time);
        BUFFER.setField("cam_tpf", tpf);
    }

    public UniformBuffer getBuffer() {
        return BUFFER;
    }
}
