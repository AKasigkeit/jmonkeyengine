/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.DispatchIndirectBuffer;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory;
import com.jme3.compute.DispatchCommand;
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.material.Material;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Alexander Kasigkeit
 */
public class TestDispatchIndirectBuffer extends SimpleApplication {

    public static void main(String[] args) {
        TestDispatchIndirectBuffer t = new TestDispatchIndirectBuffer();
        t.start();
    }

    private float time = 0f;
    private ComputeShader shader = null;
    private DispatchIndirectBuffer dispatchBuffer = null;

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            System.out.println("Hardware does not support ComputeShaders");
        }

        //create texture to write to
        int width = 512, height = 512;
        Texture tex = new Texture2D(width, height, Image.Format.RGBA16F);

        //create a box just to show the texture on the screen
        rootNode.attachChild(createTestGeo(tex));

        //create shader
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        shader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");

        int localSizeX = factory.getMaxLocalSize2D()[0];
        int localSizeY = factory.getMaxLocalSize2D()[1];
        shader.setDefine("LOCAL_SIZE_X", VarType.Int, localSizeX);
        shader.setDefine("LOCAL_SIZE_Y", VarType.Int, localSizeY);
        shader.setImage("Image", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true);

        int groupsX = (int) (Math.ceil(width / (double) localSizeX));
        int groupsY = (int) (Math.ceil(height / (double) localSizeY));
        DispatchCommand command = new DispatchCommand(groupsX, groupsY, 1);
        dispatchBuffer = DispatchIndirectBuffer.createWithCommand(command);
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        shader.setUniform("Time", VarType.Float, time);
        shader.run(dispatchBuffer, 0, MemoryBarrierBits.SHADER_IMAGE_ACCESS);
    }

    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n"
            + ""
            + "layout (IMAGE_FORMAT) uniform writeonly image2D m_Image;\n"
            + "uniform float m_Time;\n"
            + ""
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     ivec2 imgSize = ivec2(IMAGE_WIDTH, IMAGE_HEIGHT);\n"
            + "     if (any(greaterThanEqual(x, imgSize))) {\n"
            + "         return;\n"
            + "     }\n"
            + "     imageStore(m_Image, x, vec4(fract(m_Time)));\n"
            + "}\n";

    private Geometry createTestGeo(Texture tex) {
        Mesh box = new Box(1f, 1f, 1f);
        Geometry geo = new Geometry("orig", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        geo.setMaterial(mat);
        return geo;
    }
}
