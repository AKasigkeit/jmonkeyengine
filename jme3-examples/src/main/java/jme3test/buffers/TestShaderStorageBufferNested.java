/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.compute.ComputeShader;
import com.jme3.renderer.compute.MemoryBarrier;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.Member;
import com.jme3.shader.layout.Struct;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestShaderStorageBufferNested extends SimpleApplication {

    public static void main(String[] args) {
        TestShaderStorageBufferNested t = new TestShaderStorageBufferNested();
        AppSettings s = new AppSettings(true);
        s.setRenderer(AppSettings.LWJGL_OPENGL33);
        t.setSettings(s);
        t.start();
    }

    private static final int NUM_SPHERES = 64;
    private Sphere[] spheres = null;
    private ShaderStorageBuffer ssbo = null;
    private ComputeShader raytracer = null;

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.ShaderStorageBufferObject)) {
            throw new UnsupportedOperationException("Hardware doesnt support ShaderStorageBuuferObjects");
        }
        spheres = new Sphere[NUM_SPHERES];
        for (int i = 0; i < NUM_SPHERES; i++) {
            float x = (((i / 8) / 8f) * 20 - 10);
            float y = (((i % 8) / 8f) * 20 - 10);
            float z = i / (float) NUM_SPHERES;
            float d = z * 0.3f + 0.3f;
            float r = z;
            float g = (NUM_SPHERES - i) / (float) NUM_SPHERES;
            float b = 0f;
            spheres[i] = new Sphere(x, y, z, d, r, g, b);
        }

        ssbo = ShaderStorageBuffer.createNewAutolayout();
        ssbo.registerStruct(ColorComponent.class);
        ssbo.registerStruct(Color.class);
        ssbo.registerStruct(Sphere.class);
        ssbo.setField("ambientColor", new Vector3f(0f, 0f, 0.0f));
        ssbo.setField("spheres", spheres);

        int width = cam.getWidth() / 4;
        int height = cam.getHeight() / 4;
        Texture2D tex = new Texture2D(width, height, Image.Format.RGBA16F);
        tex.setMagFilter(Texture.MagFilter.Bilinear);

        Geometry geo = new Geometry("fullscreen", new Quad(1f, 1f));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        geo.setMaterial(mat);
        geo.setLocalScale(cam.getWidth(), cam.getHeight(), 1);
        guiNode.attachChild(geo);

        raytracer = ComputeShader.createFromString(renderer, SHADER_SOURCE, "GLSL430");
        raytracer.setDefine("LOCAL_SIZE_X", VarType.Int, 32);
        raytracer.setDefine("LOCAL_SIZE_Y", VarType.Int, 32);
        raytracer.setDefine("NUM_SPHERES", VarType.Int, NUM_SPHERES);
        raytracer.setShaderStorageBuffer("Spheres", ssbo);
        raytracer.setImage("Output", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true);
        raytracer.queryLayouts();
        raytracer.getShaderStorageBufferLayout("Spheres").getTreeView().printDebug();

        viewPort.addProcessor(new Processor());
        stateManager.attach(new DetailedProfilerState());
    }

    private class Processor implements SceneProcessor {

        private MemoryBarrier barrier = null;
        private boolean init = false;
        private AppProfiler prof = null;
        float time = 0f;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            barrier = rm.getRenderer().createMemoryBarrier(MemoryBarrier.Flag.All);
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
            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "update color");
            }
            time += tpf;

            float t = time % 1f;
            for (int i = 0; i < NUM_SPHERES; i++) {
                spheres[i].color.values[2].c = t;
            }
            ssbo.setField("spheres", spheres);
            if (prof != null) {
                prof.spStep(SpStep.ProcPreFrame, "Processor", "ray tracing");
            }
            int width = cam.getWidth() / 4;
            int height = cam.getHeight() / 4;
            raytracer.run(width, height, 32, 32, barrier);

        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
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
            + "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n"
            + ""
            + "struct ColorComponent {\n"
            + "     float c;\n"
            + "};\n"
            + ""
            + "struct Color {\n"
            + "     ColorComponent components[3];\n"
            + "};\n"
            + ""
            + "struct Sphere {\n"
            + "     Color color;\n"
            + "     float pos[3];\n"
            + "     float radius;\n"
            + "};\n"
            + ""
            + "layout (shared) buffer m_Spheres {\n"
            + "     vec3    ambientColor;\n"
            + "     Sphere  spheres[];\n"
            + "};\n"
            + "layout (OUTPUT_FORMAT) uniform writeonly image2D m_Output;\n"
            + ""
            + "float sdfSphere(in vec3 pos, in vec3 center, in float radius) {\n"
            + "     return distance(pos, center) - radius;\n"
            + "};\n"
            + ""
            + "float sdfScene(in vec3 pos, out vec4 color) {\n"
            + "     float d = 1000.0;\n"
            + "     for (int i = 0; i < NUM_SPHERES; i++) {\n"
            + "         float v = sdfSphere(pos, vec3(spheres[i].pos[0], spheres[i].pos[1], spheres[i].pos[2]), spheres[i].radius);\n"
            + "         if (v < d) {\n"
            + "             color = vec4(spheres[i].color.components[0].c, spheres[i].color.components[1].c, spheres[i].color.components[2].c, 1.0);\n"
            + "         }\n"
            + "         d = min(d, v);\n"
            + "     }\n"
            + "     return d;\n"
            + "}\n"
            + ""
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     ivec2 imgSize = ivec2(OUTPUT_WIDTH, OUTPUT_HEIGHT);\n"
            + "     if (any(greaterThanEqual(x, imgSize))) {\n"
            + "         return;\n"
            + "     }\n"
            + "     vec2 uv = (vec2(x) + 0.5) / vec2(imgSize);\n"
            + "     vec3 dir = normalize(vec3((uv - 0.5) * 1.5, 1.0));\n"
            + "     vec3 pos = vec3(0.0, 0.0, -15.0);\n"
            + "     float d = 0.0;\n"
            + "     vec4 color = vec4(0.0);"
            + "     for (int i = 0; i < 256; i++) {\n"
            + "         float stepDist = sdfScene(pos, color);\n"
            + "         if (stepDist < 0.01) {\n"
            + "             break;"
            + "         }\n"
            + "         pos += dir * 0.1;\n"
            + "         d += 0.1;\n"
            + "     }\n"
            + "     color += vec4(ambientColor, 0.0);\n"
            + "     imageStore(m_Output, x, pow(color, vec4(2.2)));\n"
            + "}\n";

    private static class ColorComponent implements Struct {

        @Member
        private float c;

        private ColorComponent(float c) {
            this.c = c;
        }
    }

    private static class Color implements Struct {

        @Member(maps = "components")
        private ColorComponent[] values;

        private Color(float r, float g, float b) {
            values = new ColorComponent[]{new ColorComponent(r), new ColorComponent(g), new ColorComponent(b)};
        }
    }

    private static class Sphere implements Struct {

        @Member
        private Color color;
        @Member
        private float[] pos;
        @Member
        private Float radius;

        private Sphere(float x, float y, float z, float radius, float r, float g, float b) {
            this.pos = new float[]{x, y, z};
            this.radius = radius;
            this.color = new Color(r, g, b);
        }
    }
}
