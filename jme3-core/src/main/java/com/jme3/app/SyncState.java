/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app;

import com.jme3.app.state.BaseAppState;
import com.jme3.conditional.SyncObject;
import com.jme3.renderer.Renderer;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class SyncState extends BaseAppState {

    private final Queue<SyncObject> waiting = new ArrayDeque<>();
    private final Queue<SyncObject> buffer = new ArrayDeque<>();

    private int cpuCounter = 0;
    private int gpuCounter = 0;

    private Renderer renderer = null;

    @Override
    protected void initialize(Application app) {
        renderer = app.getRenderer();
        waiting.clear();
        gpuCounter = 0;
        cpuCounter = 0;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    public void update(float tpf) {
        //at the beginning of the frame, check if the GPU has finished any
        SyncObject sync;
        SyncObject.Signal signal;
        while ((sync = waiting.peek()) != null) {
            signal = sync.checkSignal();
            if (signal == SyncObject.Signal.AlreadySignaled || signal == SyncObject.Signal.ConditionSatisfied) {
                waiting.remove();
                buffer.add(sync);
                gpuCounter++;
            } else {
                if (signal == SyncObject.Signal.WaitFailed) {
                    System.out.println("wait failed for some reason");
                }
                break;
            }
        }
    }

    @Override
    public void postRender() {
        //at the end of a frame place a new fence
        SyncObject sync = buffer.poll();
        if (sync == null) {
            sync = new SyncObject(renderer);
        } else {
            sync.recycle(); 
        }
        sync.place();
        waiting.add(sync);

        //and consider this frame processed by the CPU
        cpuCounter++;
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void cleanup(Application app) {
    }

    /**
     * returns the number of frames that have been finished on the CPU
     *
     * @return num frames cpu finished
     */
    public int getCpuFrames() {
        return cpuCounter;
    }

    /**
     * returns the number of frames that have been finished on the GPU
     *
     * @return num frames gpu finished
     */
    public int getGpuFrames() {
        return gpuCounter;
    }

    /**
     * returns the amount of frames the GPU is currently behind the CPU
     *
     * @return num frames gpu is behind
     */
    public int getNumGpuFramesBehind() {
        return cpuCounter - gpuCounter;
    }

    /**
     * checks if the GPU has finished processing the specified frame
     *
     * @param frame frame to check
     * @return true if the gpu finished processing that frame, false otherwise
     */
    public boolean isGpuFinishedFrame(int frame) {
        return gpuCounter >= frame;
    }

    /**
     * checks of the cpu has finished processing the specified frame
     *
     * @param frame frame to check
     * @return true if the cpu has finished processing that frame, false
     * otherwise
     */
    public boolean isCpuFinishedFrame(int frame) {
        return cpuCounter >= frame;
    }

}
