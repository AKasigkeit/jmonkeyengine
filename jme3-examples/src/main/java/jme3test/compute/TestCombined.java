/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.compute;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.buffer.DispatchIndirectBuffer;
import com.jme3.buffer.DrawIndirectBuffer;
import com.jme3.buffer.DrawIndirectBuffer.DrawIndicesCommand;
import com.jme3.buffer.FieldBuffer;
import com.jme3.buffer.FieldBuffer.FieldBufferWriter;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory;
import com.jme3.compute.DispatchCommand;
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.conditional.GpuQuery;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.BlockLayout.StructNode;
import com.jme3.shader.layout.Member;
import com.jme3.shader.layout.Struct;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 *
 * Tests ComputeShaders, DispatchIndirectBuffers, CameraUniformBuffer (thus
 * UniformBuffers), ShaderStorageBuffers, MultiDrawIndirect (thus
 * DrawIndirectBuffers), Layout detection and GpuQueries
 *
 *
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestCombined extends SimpleApplication {

    private static final int NUM_INSTANCES = 1_000_000;
    private static final float WORLD_SIZE = 1500f;

    public static void main(String[] args) {
        TestCombined t = new TestCombined();
        AppSettings s = new AppSettings(true);
        s.setGraphicsDebug(true);
        //s.setRenderer(AppSettings.LWJGL_OPENGL43);
        t.setSettings(s);
        t.start();
    }

    private ComputeShader compShader;
    private DispatchIndirectBuffer dispatchBuffer;
    private Geometry geo;
    private BitmapText text;

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().containsAll(Arrays.asList(Caps.ComputeShader, Caps.MultiDrawIndirect))) {
            throw new UnsupportedOperationException("Hardware doesnt support required features");
        }
        //create compute shader
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        compShader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");
        compShader.setDefine("LOCAL_SIZE_X", VarType.Int, 1024);
        compShader.setDefine("HALF_WORLD", VarType.Float, WORLD_SIZE / 2f);
        dispatchBuffer = DispatchIndirectBuffer.createWithCommand(new DispatchCommand((int) (Math.ceil(NUM_INSTANCES / 1024.0)), 1, 1));

        //read layout data
        compShader.queryLayouts();
        StructNode instanceStruct = compShader.getShaderStorageBufferLayout("InstanceData").getTreeView().getChild("instances");
        instanceStruct.printDebug();
        int translationOffset = instanceStruct.getField("translation").getLayout().getOffset();
        int scaleOffset = instanceStruct.getField("scale").getLayout().getOffset();
        int instanceStride = instanceStruct.getStride();

        //create appropriate buffer and SSBO as well as VBO views (VBOs for instance data)
        UntypedBuffer instanceBuffer = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        instanceBuffer.initialize(NUM_INSTANCES * instanceStride);
        ShaderStorageBuffer ssbo = instanceBuffer.asShaderStorageBuffer(null);
        VertexBuffer vboTranslation = instanceBuffer.asVertexBuffer(VertexBuffer.Type.Translation, VertexBuffer.Format.Float, 3, instanceStride, translationOffset);
        vboTranslation.setInstanceSpan(1);
        VertexBuffer vboScale = instanceBuffer.asVertexBuffer(VertexBuffer.Type.Scale, VertexBuffer.Format.Float, 3, instanceStride, scaleOffset);
        vboScale.setInstanceSpan(1);

        //create VBOs for vertex data
        UntypedBuffer vertexBuffer = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        vertexBuffer.initialize((3 + 3) * 4 * 4);//3 pos, 3 color = 6 floats with 4 bytes each. And that for 4 vertices
        VertexBuffer vboPosition = vertexBuffer.asVertexBuffer(VertexBuffer.Type.Position, VertexBuffer.Format.Float, 3, 24, 0);
        VertexBuffer vboTexCoord = vertexBuffer.asVertexBuffer(VertexBuffer.Type.Color, VertexBuffer.Format.Float, 3, 24, 12);

        //create index buffer
        VertexBuffer iboIndices = new VertexBuffer(VertexBuffer.Type.Index);
        iboIndices.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(0, 1, 2, 2, 3, 0));

        //setup vertex data
        ByteBuffer copyBuffer = BufferUtils.createByteBuffer(6 * 4 * 4);
        //                X             Y           Z           R           G           B
        copyBuffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(1f).putFloat(0f).putFloat(0f)
                .putFloat(1f).putFloat(0f).putFloat(0f).putFloat(0f).putFloat(1f).putFloat(0f)
                .putFloat(1f).putFloat(1f).putFloat(0f).putFloat(0f).putFloat(0f).putFloat(1f)
                .putFloat(0f).putFloat(1f).putFloat(0f).putFloat(0f).putFloat(1f).putFloat(1f)
                .flip();
        vertexBuffer.updateData(copyBuffer, 0);

        //create mesh
        Mesh mesh = new Mesh();
        mesh.setBuffer(iboIndices);
        mesh.setBuffer(vboPosition);
        mesh.setBuffer(vboTexCoord);
        mesh.setBuffer(vboTranslation);
        mesh.setBuffer(vboScale);
        mesh.setBound(new BoundingBox(Vector3f.ZERO, WORLD_SIZE / 2f, WORLD_SIZE / 2f, WORLD_SIZE / 2f));
        //and geometry
        geo = new Geometry("geo", mesh);
        Material mat = new Material(assetManager, "jme3test/mdi/MultiDrawIndirect.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        mat.setBoolean("UseScale", true);
        geo.setMaterial(mat);
        //rootNode.attachChild(geo);

        //now create some initial instances
        Instance[] instances = new Instance[NUM_INSTANCES];
        for (int i = 0; i < instances.length; i++) {
            Instance inst = new Instance();
            inst.translation = new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random()).multLocal(WORLD_SIZE).subtractLocal(WORLD_SIZE / 2f, WORLD_SIZE / 2f, WORLD_SIZE / 2f);
            inst.scale = new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random());
            instances[i] = inst;
        }
        ssbo.registerStruct(Instance.class);
        ssbo.setField("instances", instances);
        compShader.setShaderStorageBuffer("InstanceData", ssbo);

        //create DrawIndirectBuffer with appropriate command
        DrawIndirectBuffer dibo = DrawIndirectBuffer.createWithCommands(DrawIndicesCommand.withValues(6, NUM_INSTANCES, 0, 0, 0));
        mesh.setDrawIndirectBuffer(dibo);
        mesh.setDrawIndirectParameters(0, 0, 1);

        viewPort.addProcessor(new Processor());
        //stateManager.attach(new DetailedProfilerState());
        cam.setFrustumPerspective(50f, cam.getWidth() / (float) cam.getHeight(), 0.1f, WORLD_SIZE * 1.5f);
        flyCam.setMoveSpeed(10f);

        text = new BitmapText(guiFont);
        text.setText("Samples Visible: 0");
        text.setLocalTranslation(0, 200, 1);
        guiNode.attachChild(text);
    }

    private static class Instance implements Struct {

        @Member Vector3f translation;
        @Member Vector3f scale;
    }

    private class Processor implements SceneProcessor {

        private boolean init = false;
        private AppProfiler prof;
        private float time = 0f;

        private Deque<GpuQuery> pending = new ArrayDeque<>();
        private Deque<GpuQuery> buffer = new ArrayDeque<>();

        private GpuQuery getQuery() {
            GpuQuery q = buffer.poll();
            if (q != null) {
                return q;
            }
            return new GpuQuery(GpuQuery.Type.SamplesPassed, renderer);
        }

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

            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "update instance data");
            }
            compShader.setFloat("TPF", tpf * 2f);
            compShader.setFloat("Time", time);
            compShader.run(dispatchBuffer, 0, MemoryBarrierBits.ALL);
        }

        private long samples = 0L;

        @Override
        public void postQueue(RenderQueue rq) {
            if (prof != null) {
                prof.spStep(SpStep.ProcPostQueue, "Processor", "read queries");
            }
            while (!pending.isEmpty() && pending.peek().isResultAvailable()) {
                GpuQuery query = pending.poll();
                samples = query.getResult();
                buffer.add(query);
            }
            text.setText("Samples passed: " + samples);

            if (prof != null) {
                prof.spStep(SpStep.ProcPostQueue, "Processor", "render quads");
            }
            GpuQuery query = getQuery();
            query.start();
            renderManager.renderGeometry(geo);
            query.stop();
            pending.add(query);
        }

        @Override
        public void postFrame(FrameBuffer out) { 
            //GL13.glActiveTexture(123123123); // remove comment to trigger gl error
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void setProfiler(AppProfiler profiler) {
            prof = profiler;
        }

    }

    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = LOCAL_SIZE_X) in;\n"
            + ""
            + "struct Instance {\n"
            + "     vec3 translation;\n"
            + "     vec3 scale;\n"
            + "};\n"
            + ""
            + "layout (shared) buffer m_InstanceData {\n"
            + "     Instance instances[];\n"
            + "};\n"
            + ""
            + "uniform float m_TPF;\n"
            + "uniform float m_Time;\n"
            + ""
            + "void main(void) {\n"
            + "     uint x = uint(gl_GlobalInvocationID.x);\n"
            + "     if (x >= instances.length()) {\n"
            + "         return;\n"
            + "     }\n"
            + ""
            + "     instances[x].translation = instances[x].translation + cos(float(x) + 2.0)* m_TPF;\n"
            + "     if (instances[x].translation.x < -HALF_WORLD) { instances[x].translation.x = HALF_WORLD; }\n"
            + "     else if (instances[x].translation.x > HALF_WORLD) { instances[x].translation.x = -HALF_WORLD; }\n"
            + "     if (instances[x].translation.y < -HALF_WORLD) { instances[x].translation.y = HALF_WORLD; }\n"
            + "     else if (instances[x].translation.y > HALF_WORLD) { instances[x].translation.y = -HALF_WORLD; }\n"
            + "     if (instances[x].translation.z < -HALF_WORLD) { instances[x].translation.z = HALF_WORLD; }\n"
            + "     else if (instances[x].translation.z > HALF_WORLD) { instances[x].translation.z = -HALF_WORLD; }\n"
            + "     instances[x].scale = vec3(sin(float(x) + m_Time));\n"
            + "}\n";
}
