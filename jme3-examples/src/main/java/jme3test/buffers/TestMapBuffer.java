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
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.BufferDataUsage;
import com.jme3.buffer.UntypedBuffer.BufferMappingHandle;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestMapBuffer extends SimpleApplication {

    public static void main(String[] args) {
        TestMapBuffer t = new TestMapBuffer();
        t.start();
    }

    private UntypedBuffer buffer;
    private float time = 0f;

    @Override
    public void simpleInitApp() {
        buffer = UntypedBuffer.createNewBufferDataDirect(MemoryMode.GpuOnly, renderer, BufferDataUsage.StreamDraw);
        buffer.initialize(4 * 3 * 4);

        VertexBuffer posVBO = buffer.asVertexBuffer(VertexBuffer.Type.Position, VertexBuffer.Format.Float, 3, 12, 0);
        
        UntypedBuffer indexBuffer = UntypedBuffer.createNewBufferDataLazy(MemoryMode.CpuGpu, BufferDataUsage.StaticDraw);
        indexBuffer.initialize(BufferUtils.createByteBuffer((byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0));
        VertexBuffer indexVBO = indexBuffer.asIndexBuffer(VertexBuffer.Format.UnsignedByte);
        //VertexBuffer indexVBO = new VertexBuffer(VertexBuffer.Type.Index);
        //indexVBO.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.UnsignedByte,
        //        BufferUtils.createByteBuffer((byte) 0, (byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 0));

        //usual mesh material geometry procedure
        Mesh mesh = new Mesh();
        mesh.setBuffer(posVBO);
        mesh.setBuffer(indexVBO);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        Geometry geo = new Geometry("geo", mesh);
        geo.setMaterial(mat);
        rootNode.attachChild(geo);

        viewPort.addProcessor(new Processor());
        stateManager.attach(new DetailedProfilerState());
    }

    private class Processor implements SceneProcessor {

        private AppProfiler prof = null;
        private boolean init = false;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            init = true;
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
        }

        @Override
        public boolean isInitialized() {
            return init;
        }

        @Override
        public void preFrame(float tpf) {
            time += tpf;
            if (time > 3f) {
                time -= 3f;
            }
            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "map buffer");
            }
            BufferMappingHandle map = buffer.mapBuffer(false, true);
            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "write data");
            }
            map.getRawData()
                    .putFloat(time + 0f).putFloat(0f).putFloat(time)
                    .putFloat(time + 1f).putFloat(0f).putFloat(time)
                    .putFloat(time + 1f).putFloat(1f).putFloat(time)
                    .putFloat(time + 0f).putFloat(1f).putFloat(time);
            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "unmap buffer");
            }
            map.unmap();
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
        }

        @Override
        public void cleanup() {
            init = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
            prof = profiler;
        }

    }

}
