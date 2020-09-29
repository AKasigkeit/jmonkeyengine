/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.UniformBuffer;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory;
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
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
public class TestUniformBuffer extends SimpleApplication {

    public static void main(String[] args) {
        TestUniformBuffer t = new TestUniformBuffer();
        t.start();
    }

    private ComputeShader shader = null;
    private float time = 0f;
    
    private UniformBuffer buffer = null;
    private Vector2f lightPos = new Vector2f(); 
    
    int width, height, localX, localY;

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            System.out.println("Hardware does not support ComputeShaders");
        }

        //create texture to write to
        width = 512;
        height = 512;
        Texture tex = new Texture2D(width, height, Image.Format.RGBA16F);

        //create a box just to show the texture on the screen
        rootNode.attachChild(createTestGeo(tex));

        //create shader
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        shader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");

        localX = factory.getMaxLocalSize2D()[0];
        localY = factory.getMaxLocalSize2D()[1];
        shader.setDefine("LOCAL_SIZE_X", VarType.Int, localX);
        shader.setDefine("LOCAL_SIZE_Y", VarType.Int, localY);
        shader.setImage("Image", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true);
    
        buffer = UniformBuffer.createNewAutolayout();
        buffer.setField("light_pos", lightPos);
        buffer.setField("light_color", ColorRGBA.Yellow);
        buffer.setField("light_threshold", 0.4f);
        shader.setUniformBuffer("Light", buffer);
        
        flyCam.setMoveSpeed(20);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        time += tpf * 0.1f;
        
        lightPos.setX((float)(Math.sin(System.currentTimeMillis() / 5000.0) + 1.0f) * 0.5f);
        lightPos.setY((float)(Math.cos(System.currentTimeMillis() / 5000.0) + 1.0f) * 0.5f);
        buffer.setField("light_pos", lightPos);
        renderer.memoryBarrier(MemoryBarrierBits.ALL);
        shader.run(width, height, localX, localY, MemoryBarrierBits.ALL);
    }
    
    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n"
            + ""
            + "layout (shared) uniform m_Light {\n"
            + "     vec2 light_pos;\n"
            + "     vec4 light_color;\n"
            + "     float light_threshold;\n"
            + "};\n"
            + "layout (IMAGE_FORMAT) uniform writeonly image2D m_Image;\n"
            + ""
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     ivec2 imgSize = ivec2(IMAGE_WIDTH, IMAGE_HEIGHT);\n"
            + "     if (any(greaterThanEqual(x, imgSize))) {\n"
            + "         return;\n"
            + "     }\n"
            + "     vec2 uv = (vec2(x) + 0.5) / vec2(imgSize);\n"
            + "     vec2 delta = abs(light_pos - uv);\n"
            + "     float toggle = step(light_threshold, length(delta));\n"
            + "     vec4 col = (light_color + vec4(delta, 0.0, 0.0)) * toggle;\n"
            + "     imageStore(m_Image, x, vec4(col));\n"
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
