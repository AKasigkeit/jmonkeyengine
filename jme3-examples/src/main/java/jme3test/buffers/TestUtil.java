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

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.font.BitmapText;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestUtil {

    public static class AutoScreenshotApp extends SimpleApplication {

        private ScreenshotAppState screenshot = null;
        private float totalTime = 0f;
        private boolean done = false;
        private BitmapText text = null;
        private int frames = 0;

        @Override
        public void simpleInitApp() {
            screenshot = new ScreenshotAppState();
            screenshot.setIsNumbered(false);
            stateManager.attach(screenshot);
            
            text = new BitmapText(guiFont);
            text.setLocalTranslation(0, cam.getHeight(), 0);
            guiNode.attachChild(text);
        }

        @Override
        public void simpleUpdate(float tpf) {
            if (totalTime == 0f) {
                screenshot.setFileName(getClass().getSimpleName() + "_1_" + System.currentTimeMillis());
                screenshot.takeScreenshot();
            } else if (!done && totalTime >= 10f) {
                done = true;
                screenshot.setFileName(getClass().getSimpleName() + "_2_" + System.currentTimeMillis());
                screenshot.takeScreenshot();
            }
            totalTime += tpf;
            frames++;
            text.setText("Frames: "+frames);
        }

    }

    public static class NullProcessor implements SceneProcessor {

        private boolean init = false;
        protected AppProfiler profiler = null;

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
            this.profiler = profiler;
        }

    }
}
