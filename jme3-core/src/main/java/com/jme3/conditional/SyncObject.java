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
        AlreadySignaled,
        TimeoutExpired,
        ConditionSatisfied,
        WaitFailed; 
    }

    private Object sync = null;
    private boolean placed = false;

    public SyncObject() {

    }

    //for destructable clone
    private SyncObject(Object ref) {
        super(0);
        sync = ref;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setSyncRef(Object obj) {
        sync = obj;
    }

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
        return new SyncObject(sync);
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_SYNC << 32) | ((long) sync.hashCode()); //not the best
    }

}
