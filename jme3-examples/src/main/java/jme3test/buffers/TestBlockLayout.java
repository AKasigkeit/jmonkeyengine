/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory;
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.BlockLayout;
import com.jme3.shader.layout.BlockLayout.StructNode;
import com.jme3.shader.layout.Member;
import com.jme3.shader.layout.Struct;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestBlockLayout extends SimpleApplication {

    public static void main(String[] args) {
        TestBlockLayout t = new TestBlockLayout();
        t.start();
    }

    private ComputeShader shader = null;

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            System.out.println("Hardware does not support ComputeShaders");
        }
        
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        shader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");
        shader.setDefine("NUM_VERTS", VarType.Int, 4);

        shader.queryLayouts();
        StructNode tree = shader.getShaderStorageBufferLayout("Vertices").getTreeView();
        StructNode vertsNode = tree.getChild("vertexData");

        int stride = vertsNode.getStride();
        int posOffset = vertsNode.getField("pos").getLayout().getOffset();
        int uvOffset = vertsNode.getField("uv").getLayout().getOffset();
        int colorOffset = vertsNode.getField("color").getLayout().getOffset();

        Vertex[] vertices = new Vertex[4];
        vertices[0] = new Vertex(new Vector3f(0f, 0f, 0f), new Vector2f(0f, 0f), new ColorRGBA(1f, 0f, 0f, 0f));
        vertices[1] = new Vertex(new Vector3f(1f, 0f, 0f), new Vector2f(1f, 0f), new ColorRGBA(0f, 1f, 0f, 0f));
        vertices[2] = new Vertex(new Vector3f(1f, 1f, 0f), new Vector2f(1f, 1f), new ColorRGBA(0f, 0f, 1f, 0f));
        vertices[3] = new Vertex(new Vector3f(0f, 1f, 0f), new Vector2f(0f, 1f), new ColorRGBA(0.5f, 0.5f, 0.5f, 0f));

        UntypedBuffer rawBuffer = UntypedBuffer.createNewBufferDataDirect(UntypedBuffer.MemoryMode.GpuOnly, renderer, UntypedBuffer.BufferDataUsage.StaticDraw);
        rawBuffer.initialize(stride * 4);
        ShaderStorageBuffer vertsSSBO = rawBuffer.asShaderStorageBuffer(null);
        vertsSSBO.registerStruct(Vertex.class);
        vertsSSBO.setField("vertexData", vertices);
        shader.setShaderStorageBuffer("Vertices", vertsSSBO);

        VertexBuffer posVBO = rawBuffer.asVertexBuffer(VertexBuffer.Type.Position, VertexBuffer.Format.Float, 3, stride, posOffset);
        VertexBuffer texVBO = rawBuffer.asVertexBuffer(VertexBuffer.Type.TexCoord, VertexBuffer.Format.Float, 2, stride, uvOffset);
        VertexBuffer colorVBO = rawBuffer.asVertexBuffer(VertexBuffer.Type.Color, VertexBuffer.Format.Float, 4, stride, colorOffset);
        VertexBuffer indexIBO = new VertexBuffer(VertexBuffer.Type.Index);
        indexIBO.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(0, 1, 2, 2, 3, 0));

        Mesh mesh = new Mesh();
        mesh.setBuffer(indexIBO);
        mesh.setBuffer(posVBO);
        mesh.setBuffer(texVBO);
        mesh.setBuffer(colorVBO);

        Geometry geo = new Geometry("geo", mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        tex.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", tex);
        geo.setMaterial(mat);
        geo.setCullHint(Spatial.CullHint.Never);
        rootNode.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(geo);
    }

    @Override
    public void simpleUpdate(float tpf) {
        shader.setFloat("TPF", tpf);
        shader.run(4, 4, MemoryBarrierBits.ALL);
    }

    private static class Vertex implements Struct {

        @Member
        private Vector3f pos;
        @Member
        private Vector2f uv;
        @Member
        private ColorRGBA color;

        public Vertex(Vector3f p, Vector2f u, ColorRGBA c) {
            pos = p;
            uv = u;
            color = c;
        }
    }

    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = 4) in;\n"
            + ""
            + "struct Vertex {\n"
            + "     vec3 pos;\n"
            + "     vec2 uv;\n"
            + "     vec4 color;\n"
            + "};\n"
            + ""
            + "layout (shared) buffer m_Vertices {\n"
            + "     Vertex  vertexData[];\n"
            + "};\n"
            + ""
            + "uniform float m_TPF;\n"
            + ""
            + "void main(void) {\n"
            + "     uint x = uint(gl_GlobalInvocationID.x);\n"
            + "     if (x >= vertexData.length()) {\n"
            + "         return;\n"
            + "     }\n"
            + "     vertexData[x].uv.x += m_TPF * 2.0;\n"
            + "     vertexData[x].pos.x += m_TPF * 2.0;\n"
            + "     if (vertexData[x].pos.x >= 3.0) {\n"
            + "         vertexData[x].pos.x = -3.0;\n"
            + "     }\n"
            + "}\n";
}
