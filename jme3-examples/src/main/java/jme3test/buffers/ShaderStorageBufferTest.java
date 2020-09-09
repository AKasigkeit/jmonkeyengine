/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory;
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.Member;
import com.jme3.shader.layout.Struct;
import com.jme3.shader.layout.Type; 
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ShaderStorageBufferTest extends SimpleApplication {

    public static void main(String[] args) {
        ShaderStorageBufferTest t = new ShaderStorageBufferTest();
        t.start();
    }
    
    private static final int NUM_SPHERES = 128;
    
    @Override
    public void simpleInitApp() {
        Sphere[] spheres = new Sphere[NUM_SPHERES];
        Vector3f pos = new Vector3f();
        for (int i = 0; i < NUM_SPHERES; i++) {
            pos.setX((float)(Math.random() * 20) - 10);
            pos.setY((float)(Math.random() * 20) - 10);
            pos.setZ((float)(Math.random() * 5));
            spheres[i] = new Sphere(pos, 1f);
        }
        
        ShaderStorageBuffer ssbo = ShaderStorageBuffer.createNewAutolayout();
        ssbo.registerStruct(Sphere.class);
        ssbo.setField("s_data", spheres);
        
        int width = cam.getWidth();
        int height = cam.getHeight(); 
        Texture2D tex = new Texture2D(width, height, Image.Format.RGBA16F);
        
        Geometry geo = new Geometry("fullscreen", new Quad(1f, 1f));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        geo.setMaterial(mat);
        geo.setLocalScale(width, height, 1);
        guiNode.attachChild(geo);
        
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        ComputeShader raytracer = factory.createComputeShader(SHADER_SOURCE, "GLSL430");
        raytracer.setDefine("LOCAL_SIZE_X", VarType.Int, 32);
        raytracer.setDefine("LOCAL_SIZE_Y", VarType.Int, 32);
        raytracer.setDefine("NUM_SPHERES", VarType.Int, NUM_SPHERES);
        raytracer.setShaderStorageBuffer("Spheres", ssbo);
        raytracer.setImage("Output", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true);
        
        raytracer.run(width, height, 32, 32, MemoryBarrierBits.ALL);
    }
    
    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n"
            + ""
            + "struct Sphere {\n"
            + "     vec3    pos;\n"
            + "     float   radius;\n"
            + "};\n"
            + "layout (shared) buffer m_Spheres {\n"
            + "     Sphere  s_data[NUM_SPHERES];\n"
            + "};\n"
            + "layout (OUTPUT_FORMAT) uniform writeonly image2D m_Output;\n"
            + ""
            + "float sdfSphere(in vec3 pos, in vec3 center, in float radius) {\n"
            + "     return distance(pos, center) - radius;\n"
            + "};\n"
            + ""
            + "float sdfScene(in vec3 pos) {\n"
            + "     float d = 1000.0;\n"
            + "     for (int i = 0; i < NUM_SPHERES; i++) {\n"
            + "         d = min(d, sdfSphere(pos, s_data[i].pos, s_data[i].radius));\n"
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
            + "     for (int i = 0; i < 512; i++) {\n"
            + "         float stepDist = sdfScene(pos);\n"
            + "         if (stepDist < 0.01) {\n"
            + "             break;"  
            + "         }\n"
            + "         pos += dir * 0.1;\n"   
            + "         d += 0.1;\n"     
            + "     }\n"
            + "     imageStore(m_Output, x, vec4(1.0 - pow(d / 100.0, 1.0/2.2)));\n"
            + "}\n";
    
    
    private static class Sphere implements Struct {
        @Member(type = Type.Vec3)
        private Vector3f pos;
        @Member(type = Type.Float)
        private float radius;
        
        private Sphere(Vector3f pos, float radius) {
            this.pos = new Vector3f(pos);
            this.radius = radius;
        }
    }
    
}
