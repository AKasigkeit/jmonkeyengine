/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.conditional;

import com.jme3.buffer.QueryBuffer;
import com.jme3.renderer.Renderer;
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
        SamplesPassed,
        /**
         * Queries if any of the samples passed depth test. REQUIRES OpenGL 3.3
         */
        AnySamplesPassed,
        /**
         * Basically same as ANY_SAMPLES_PASSED, only the implementation might
         * be less accurate, but faster, resulting in more false positives.
         * REQUIRES OpenGL 4.3
         */
        AnySamplesPassedConservative,
        /**
         * Queries the amount of primitives written to a GeometryShader (stream
         * 0 if no GeometryShader is present). REQUIRES OpenGL 3.0
         */
        PrimitivesGenerated,
        /**
         * Queries the amount of primitives written by a GeometryShader to a
         * TransformFeedback object (stream 0 if no GeometryShader is present).
         * Requires OpenGL 3.0 <b>(CURRENTLY TRANSFORM FEEDBACK IS NOT
         * SUPPORTED)</b>
         */
        TransformFeedbackPrimitivesWritten,
        /**
         * Queries the time that elapsed on the GPU. REQUIRES OpenGL 3.3
         */
        TimeElapsed,
        /**
         * Queries the current timestamp on the GPU. REQUIRES OpenGL 3.3
         */
        Timestamp;
    }

    private final Type TYPE;
    private final Renderer RENDERER;
    private boolean running = false;

    private long result = NO_RESULT;
    private boolean resultAvailable = false; 

    public GpuQuery(Type type, Renderer renderer) {
        if (type == null || renderer == null) {
            throw new IllegalArgumentException("none of the arguments can be null");
        }
        TYPE = type;
        RENDERER = renderer;
    }

    private GpuQuery(int id, Type type, Renderer renderer) {
        super(id);
        TYPE = type;
        RENDERER = renderer;
    }

    /**
     * Starts this GpuQuery.
     */
    public void start() {
        RENDERER.startQuery(this);
    }

    /**
     * Stops this GpuQuery.
     */
    public void stop() {
        RENDERER.stopQuery(this);
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
        resultAvailable |= RENDERER.isQueryResultAvailable(this);
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
        result = RENDERER.getQueryResult(this);
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
        RENDERER.getQueryResultAvailability(buffer, this, offset);
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
        RENDERER.getQueryResult(buffer, this, offset, bits64, wait);
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
     */
    public void setStarted() { 
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
        GpuQuery c = new GpuQuery(getId(), TYPE, RENDERER); 
        return c;
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_QUERY << 32) | ((long) id);
    }
}
