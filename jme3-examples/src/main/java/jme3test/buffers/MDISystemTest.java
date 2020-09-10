/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.buffer.DrawIndirectBuffer.DrawIndirectMode;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.manager.MDISystem;
import com.jme3.scene.manager.MDISystem.MDIGeometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class MDISystemTest extends SimpleApplication {

    private static final int NUM_OBJECTS = 16_000;
    private static boolean USE_WORLDMATRIX = true;
    private static final int MULTI_BUFFERING = 2;

    public static void main(String[] args) {
        MDISystemTest t = new MDISystemTest();
        t.start();
    }

    private MDISystem mdiSys;
    private SafeArrayList<MDIGeometry> geos = new SafeArrayList<>(MDIGeometry.class);

    @Override
    public void simpleInitApp() {
        Material mat = new Material(assetManager, "jme3test/mdi/MultiDrawIndirect.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg"));
        mat.setBoolean("UseWorldMatrix", USE_WORLDMATRIX);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        mdiSys = new MDISystem(mat, renderManager, DrawIndirectMode.DrawIndices)
                .setDrawIndirectMultiBuffering(MULTI_BUFFERING)
                .setProvideInstanceId();
        if (USE_WORLDMATRIX) {
            mdiSys.setProvideWorldMatrix();
        }
        mdiSys.initialize(
                new Type[]{Type.Position, Type.TexCoord}, new Format[]{Format.Float, Format.Float}, new int[]{3, 2},
                new Type[]{}, new Format[]{}, new int[]{},
                6, 4, NUM_OBJECTS);

        ByteBuffer modelIndices = BufferUtils.createByteBuffer(6 * 4);
        modelIndices.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0)
                .flip();
        ByteBuffer modelVertices = BufferUtils.createByteBuffer(4 * (3 + 2) * 4);//4 vertices, each 3 (position) + 2(texcoord) floats, with 4 bytes each
        modelVertices.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f)
                .putFloat(1f).putFloat(0f).putFloat(0f).putFloat(1f).putFloat(0f)
                .putFloat(1f).putFloat(1f).putFloat(0f).putFloat(1f).putFloat(1f)
                .putFloat(0f).putFloat(1f).putFloat(0f).putFloat(0f).putFloat(1f)
                .flip();

        for (int i = 0; i < NUM_OBJECTS; i++) {
            MDIGeometry geo = mdiSys.createInstance(modelIndices, modelVertices, null, new BoundingBox(Vector3f.ZERO, 0.5f, 0.5f, 0.5f));
            geo.setLocalTranslation((float) (Math.random() * 100 - 50), (float) (Math.random() * 100 - 50), (float) (Math.random() * 100 - 50));
            geo.updateGeometricState();
            geos.add(geo);
        }

        viewPort.addProcessor(new MDISceneProcessor());
        stateManager.attach(new DetailedProfilerState());
        flyCam.setMoveSpeed(20f);
    }

    private class MDISceneProcessor implements SceneProcessor {

        @Override
        public void preFrame(float tpf) {
            long start = System.nanoTime();
            if (USE_WORLDMATRIX) {
                for (MDIGeometry geo : geos.getArray()) {
                    geo.rotate(tpf, FastMath.sin(geo.getInstanceID() * 0.5f) * tpf, (geo.getInstanceID() / (float) NUM_OBJECTS) * tpf);
                    //geo.rotate(tpf, 0.5f * tpf, 0f);
                    geo.updateGeometricState();
                }
            }
            long dur = System.nanoTime() - start;
            //System.out.println("updating geometries took: " + (dur / 1000000.0) + " ms");
        }

        @Override
        public void postQueue(RenderQueue rq) {
            long start = System.nanoTime();
            mdiSys.render(geos.getArray(), NUM_OBJECTS);
            long dur = System.nanoTime() - start;
            //System.out.println("rendering geometries took: " + (dur / 1000000.0) + " ms (streaming worldMatrices and draw commands)");
        }

        //DUMMY IMPLEMENTATION STUFF
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
        public void postFrame(FrameBuffer out) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
        }

    }

}
