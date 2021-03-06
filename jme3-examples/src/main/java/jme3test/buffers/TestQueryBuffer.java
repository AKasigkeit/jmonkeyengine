/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.buffer.FieldBuffer;
import com.jme3.buffer.QueryBuffer;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.conditional.GpuQuery;
import com.jme3.material.Material;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.compute.MemoryBarrier;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.IntMap;

/**
 *
 * @author Alexander Kasigkeit
 *
 * ONLY WORK WITH LWJGL3, lwjgl2 forces to use the memory address of the
 * provided buffer instead of using it as offset
 *
 */
public class TestQueryBuffer extends SimpleApplication {

    public static void main(String[] args) {
        TestQueryBuffer t = new TestQueryBuffer();
        t.start();
    }

    private static final boolean USE_MAP = true;
    private static final int NUM_OBJECTS = 1024;

    private QueryBuffer queryBuffer = null;
    private IntMap<GpuQuery> queries = new IntMap<>();

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.QueryBuffer)) {
            throw new UnsupportedOperationException("Hardware doesnt support QueryBuffers");
        }
        UntypedBuffer buffer = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        buffer.initialize(NUM_OBJECTS * 4);
        queryBuffer = buffer.asQueryBuffer();
        ShaderStorageBuffer ssbo = buffer.asShaderStorageBuffer(FieldBuffer.FieldBufferWriter.NULL_WRITER);
        //ShaderStorageBuffer ssbo = ShaderStorageBuffer.createNewAutolayout();
        int[] arr = new int[NUM_OBJECTS];
        for (int i = 0; i < NUM_OBJECTS; i++) {
            arr[i] = i;
        }
        ssbo.setField("samplesPassed", arr);

        Mesh mesh = new Box(1f, 1f, 1f);

        for (int i = 0; i < NUM_OBJECTS; i++) {
            Geometry geo = new Geometry("geo_" + i, mesh);
            geo.setLocalTranslation((float) (Math.random() * 100 - 50), (float) (Math.random() * 100 - 50), (float) (Math.random() * 100 - 50));
            geo.setUserData("ID", Integer.valueOf(i));

            Material mat = new Material(assetManager, "jme3test/qbo/QueryBuffer.j3md");
            mat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg"));
            mat.setShaderStorageBuffer("QueryResults", ssbo);
            mat.setInt("NumQueries", NUM_OBJECTS);
            mat.setInt("QueryIndex", i);
            geo.setMaterial(mat);

            queries.put(i, new GpuQuery(GpuQuery.Type.SamplesPassed, renderer));
            rootNode.attachChild(geo);
        }
        viewPort.addProcessor(new QuerySceneProcessor());
        stateManager.attach(new DetailedProfilerState());
        flyCam.setMoveSpeed(20);
    }

    private class QuerySceneProcessor implements SceneProcessor {

        private MemoryBarrier mem;
        private IntMap<GpuQuery> queriesMap = new IntMap<>();
        private IntMap<GpuQuery> queriesMapLast = new IntMap<>();
        private boolean first = true;

        @Override
        public void preFrame(float tpf) {
        }

        @Override
        public void postQueue(RenderQueue rq) {
            IntMap<GpuQuery> putMap = first ? queriesMap : queriesMapLast;
            IntMap<GpuQuery> getMap = first ? queriesMapLast : queriesMap;

            GeometryList opaque = rq.getList(RenderQueue.Bucket.Opaque);
            putMap.clear();
            for (int i = 0; i < opaque.size(); i++) {
                Geometry geo = opaque.get(i);
                Integer id = geo.getUserData("ID");
                GpuQuery query = queries.get(id);

                renderer.startQuery(query);
                renderManager.renderGeometry(geo);
                renderer.stopQuery(query);
                if (USE_MAP) {
                    putMap.put(id, query);
                } else {
                    queryBuffer.storeResult(query, id * 4, false, true);
                }
            }
            if (USE_MAP) {
                for (IntMap.Entry<GpuQuery> e : getMap) {
                    queryBuffer.storeResult(e.getValue(), e.getKey() * 4, false, true);
                }
            }

            renderManager.getRenderer().placeMemoryBarrier(mem);
            opaque.clear();

            first = !first;
        }

        //DUMMY IMPLEMENTATION STUFF
        private boolean init = false;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            mem = rm.getRenderer().createMemoryBarrier(MemoryBarrier.Flag.All);
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
            init = false;
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
        }

    }

}
