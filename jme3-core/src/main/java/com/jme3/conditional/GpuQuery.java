/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.conditional;

import com.jme3.buffer.QueryBuffer;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.util.NativeObject;

/**
 *
 * @author Alexander Kasigkeit
 */
public class GpuQuery extends NativeObject {

    public static final long NO_RESULT = Long.MIN_VALUE;

    public static enum Type {
        /**
         * Queries the amount of samples that passed depth test. Available on
         * all platforms that run jme3.
         */
        SAMPLES_PASSED(GL.GL_SAMPLES_PASSED),
        /**
         * Queries if any of the samples passed depth test. REQUIRES OpenGL 3.3
         */
        ANY_SAMPLES_PASSED(GL3.GL_ANY_SAMPLES_PASSED),
        /**
         * Basically same as ANY_SAMPLES_PASSED, only the implementation might
         * be less accurate, but faster, resulting in more false positives.
         * REQUIRES OpenGL 4.3
         */
        ANY_SAMPLES_PASSED_CONSERVATIVE(GL4.GL_ANY_SAMPLES_PASSED_CONSERVATIVE),
        /**
         * Queries the amount of primitives written to a GeometryShader (stream
         * 0 if no GeometryShader is present). REQUIRES OpenGL 3.0
         */
        PRIMITIVES_GENERATED(GL3.GL_PRIMITIVES_GENERATED),
        /**
         * Queries the amount of primitives written by a GeometryShader to a
         * TransformFeedback object (stream 0 if no GeometryShader is present).
         * Requires OpenGL 3.0 <b>(CURRENTLY TRANSFORM FEEDBACK IS NOT
         * SUPPORTED)</b>
         */
        TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN(GL3.GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN),
        /**
         * Queries the time that elapsed on the GPU. REQUIRES OpenGL 3.3
         */
        TIME_ELAPSED(GL3.GL_TIME_ELAPSED),
        /**
         * Queries the current timestamp on the GPU. REQUIRES OpenGL 3.3
         */
        TIMESTAMP(GL3.GL_TIMESTAMP);

        private final int glvalue;

        private Type(int val) {
            glvalue = val;
        }

        public int getGLValue() {
            return glvalue;
        }
    }

    private final Type TYPE;
    private Renderer renderer = null;
    private boolean running = false;

    private long result = NO_RESULT;
    private boolean resultAvailable = false;

    public GpuQuery(Type type) {
        this(type, null);
    }

    public GpuQuery(Type type, Renderer renderer) {
        TYPE = type;
        this.renderer = renderer;
    }

    private GpuQuery(int id, Type type) {
        super(id);
        TYPE = type;
    }

    /**
     * Starts this GpuQuery. Can only be used if a renderer was provided in the
     * constructor
     */
    public void start() {
        if (renderer == null) {
            throw new UnsupportedOperationException("using this method is only supported if you provide a renderer in the constructor");
        }
        renderer.startQuery(this);
    }

    /**
     * Stops this GpuQuery.
     */
    public void stop() {
        renderer.stopQuery(this);
    }

    /**
     * Checks if this GpuQuery's result is available. When getResult() has
     * already been called this will return true right away, otherwise it will
     * ask GL for the result's availability which will flush the GL pipeline.
     * When reusing a GpuQuery, when calling startQuery() this method will still
     * relate to the result of the previous usage and first upon stopping the
     * GpuQuery again the previous result will be lost and this method will
     * relate to the current result, thus turning it into a call to GL again
     *
     * @return true in case the result is available (that is getResult() can be
     * called without having to wait due to synchronization between GPU and CPU)
     */
    public boolean isResultAvailable() {
        if (resultAvailable || result != NO_RESULT) {
            return true;
        }
        resultAvailable |= renderer.isQueryResultAvailable(this);
        return resultAvailable;
    }

    /**
     * Returns this GpuQuery's result. Will stall the CPU in case the result is
     * not yet available
     *
     * @return the result of this GpuQuery
     */
    public long getResult() {
        if (result != NO_RESULT) {
            return result;
        }
        result = renderer.getQueryResult(this);
        return result;
    }

    /**
     * Stores the results availability in the provided QueryBuffer at the
     * provided offset in bytes. The data written will be 1 unsigned int, ie 4
     * bytes. It stores a 0 in case the result is not available and a 1 in case
     * it is
     *
     * @param buffer the buffer to store the results availability in
     * @param offset the offset in bytes to store it at
     */
    public void storeResultAvailability(QueryBuffer buffer, int offset) {
        if (renderer == null) {
            throw new UnsupportedOperationException("query has not yet been started");
        }
        renderer.getQueryResultAvailability(buffer, this, offset);
    }

    /**
     * Stores the result in the provided QueryBuffer at the provided offset in
     * bytes. The data written will either be an unsigned int or an unsigned
     * long dependanton the provided bits64. If wait is set to true, the GPU
     * will wait until the result is available, however if it is set to false,
     * the result will only be written in case it is available, otherwise the
     * content of the buffer will not be changed
     *
     * @param buffer the buffer to store the results availablity in
     * @param offset the offset in bytes to store it at
     * @param bits64 true to write an unsigned long, false for unsigned int
     * @param wait true to wait for the results avaiabilty, false to not change
     * the buffer in case the result is not available
     */
    public void storeResult(QueryBuffer buffer, int offset, boolean bits64, boolean wait) {
        if (renderer == null) {
            throw new UnsupportedOperationException("query has not yet been started");
        }
        renderer.getQueryResult(buffer, this, offset, bits64, wait);
    }

    /**
     * Returns the type of this GpuQuery
     *
     * @return type of this query
     */
    public Type getType() {
        return TYPE;
    }

    /**
     * Returns true if and only if this GpuQuery is currently running.
     *
     * @return true if currently running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns true if this GpuQuery has already been started. Will still return
     * true after the GpuQuery was stopped
     *
     * @return true if it was started already, even if it stopped already too
     */
    public boolean isStarted() {
        return id != -1;
    }

    /**
     * USED INTERNALLY. Called by the renderer upon starting a query
     *
     * @param renderer
     */
    public void setStarted(Renderer renderer) {
        this.renderer = renderer;
        this.running = true;
    }

    /**
     * USED INTERNALLY. Called by the renderer upon stopping a query
     */
    public void setStopped() {
        this.running = false;
        this.result = NO_RESULT;
        this.resultAvailable = false;
    }

    @Override
    public void resetObject() {
        this.id = -1;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This GpuQuery can't be deleted from " + rendererObject);
        }
        ((Renderer) rendererObject).deleteQuery(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        GpuQuery c = new GpuQuery(getId(), TYPE);
        c.renderer = renderer;
        return c;
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_QUERY << 32) | ((long) id);
    }
}
