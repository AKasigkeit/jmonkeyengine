/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.conditional;

import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;

/**
 *
 * @author Alexander Kasigkeit
 */
public class SyncObject extends NativeObject {

    public static enum Signal {
        /**
         * Indicates the SyncObject was signaled already before its state was
         * queried
         */
        AlreadySignaled,
        /**
         * Indicates the SyncObject didn't get signaled during the query
         */
        TimeoutExpired,
        /**
         * Indicates the SyncObject was signaled during the query
         */
        ConditionSatisfied,
        /**
         * Indicates an error occured during the query
         */
        WaitFailed;
    }

    private final Renderer RENDERER;
    private Object sync = null;
    private boolean placed = false;

    public SyncObject(Renderer renderer) {
        RENDERER = renderer;
    }

    //for destructable clone
    private SyncObject(Object ref, Renderer renderer) {
        super(0);
        RENDERER = renderer;
        sync = ref;
    }

    /**
     * Puts this SyncObject into the Gpu queue.
     */
    public void place() {
        RENDERER.placeSyncObject(this);
    }

    /**
     * Checks if the Gpu has already processed past this SyncObject
     *
     * @return the current signal
     */
    public Signal checkSignal() {
        return RENDERER.checkSyncObject(this, 0L);
    }
    
    public Signal checkSignal(long timeoutNanos) {
        return RENDERER.checkSyncObject(this, timeoutNanos);
    }

    /**
     * To reuse a SyncObject, call this method prior to placing it again.
     */
    public void recycle() {
        RENDERER.recycleSyncObject(this);
    }

    /**
     * USED INTERNALLY.
     *
     * @param placed
     */
    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    /**
     * Returns true in case this SyncObject has been placed already and not yet
     * been recycled, returns false otherwise.
     *
     * @return true if this SyncObject is placed
     */
    public boolean isPlaced() {
        return placed;
    }

    /**
     * USED INTERNALLY.
     *
     * @param obj
     */
    public void setSyncRef(Object obj) {
        sync = obj;
    }

    /**
     * USED INTERNALLY.
     */
    public Object getSyncRef() {
        return sync;
    }

    @Override
    public void resetObject() {
        this.sync = null;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This SyncObject can't be deleted from " + rendererObject);
        }
        ((Renderer) rendererObject).recycleSyncObject(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new SyncObject(sync, RENDERER);
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_SYNC << 32) | ((long) sync.hashCode()); //not the best
    }

}
