/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.compute;

import com.jme3.app.SimpleApplication;
import com.jme3.compute.ComputeShader;
import com.jme3.compute.ComputeShaderFactory; 
import com.jme3.compute.MemoryBarrierBits;
import com.jme3.material.Material;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ComputeShaderTestBuffer extends SimpleApplication {

    public static void main(String[] args) {
        ComputeShaderTestBuffer test = new ComputeShaderTestBuffer();
        AppSettings settings = new AppSettings(true);
        //settings.setRenderer(AppSettings.LWJGL_OPENGL43);
        test.setSettings(settings);
        test.start();
    }
    
    private ComputeShader shader;
    int width, height, localSizeX, localSizeY;

    @Override
    public void simpleInitApp() {
        System.out.println("RENDERER: "+(settings.getRenderer()));
        System.out.println("OpenGL 2.0: "+renderer.getCaps().contains(Caps.OpenGL20));
        System.out.println("OpenGL 3.0: "+renderer.getCaps().contains(Caps.OpenGL30));
        System.out.println("OpenGL 4.0: "+renderer.getCaps().contains(Caps.OpenGL40));
        System.out.println("OpenGL 4.5: "+renderer.getCaps().contains(Caps.OpenGL45));
        System.out.println("ComputeShader: "+renderer.getCaps().contains(Caps.ComputeShader));
        
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            System.out.println("Hardware doesnt support ComputeShaders");
            stop();
            return;
        }
        
        ComputeShaderFactory factory = ComputeShaderFactory.create(renderer);
        width = 512;
        height = 512;
        Texture tex = new Texture2D(width, height, Image.Format.RGBA16F); 
        
        //create a box just to show the texture on the screen
        Mesh box = new Box(1f, 1f, 1f); 
        Geometry geoOrig = new Geometry("orig", box);
        Material matOrig = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matOrig.setTexture("ColorMap", tex);
        geoOrig.setMaterial(matOrig);
        geoOrig.setLocalTranslation(-2f, 0f, 5f);
        rootNode.attachChild(geoOrig); 
        
        //get maximum local sizes for dimensions x and y, considering this is a 2D problem
        localSizeX = factory.getMaxLocalSize2D()[0];
        localSizeY = factory.getMaxLocalSize2D()[1];
        //setup the shader
        shader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");
        shader.setDefine("LOCAL_SIZE_X", VarType.Int, localSizeX);
        shader.setDefine("LOCAL_SIZE_Y", VarType.Int, localSizeY); 
        shader.setImage("Output", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true); 
        
        shader.run(width, height, localSizeX, localSizeY, MemoryBarrierBits.ALL);
        
        shader.getShader().printLayoutInformation();
    }
    
    @Override
    public void simpleUpdate(float tpf) { 
    }

    //To keep the example a single file, the shader source is kept here as a string
    //in most cases, this would be put into a .comp file similar to other shaders
    //and loaded using the ComputeShaderFactory's other create() method
    private static final String SHADER_SOURCE
            = "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n" //LOCAL_SIZE_X and LOCAL_SIZE_Y are defines set via java after querying GL for the implementation dependant maxima
            + "layout (shared) uniform m_Light {\n"
            + "     mat3    light_mat3;\n"
            + "     mat4    light_mat4;\n"
            + "     vec4    light_color;\n"
            + "     vec3    light_direction;\n"
            + "     float   light_intensity;\n"
            + "     vec2    light_vec2;\n"
            + "     int     light_int;\n"
            + "     uint    light_uint;\n"
            + "};\n"
            + "layout (shared) buffer m_Boxes {\n"
            + "     mat3    boxes_mat3;\n"
            + "     mat4    boxes_mat4;\n"
            + "     vec4    boxes_color;\n"
            + "     vec3    boxes_direction;\n"
            + "     float   boxes_intensity;\n"
            + "     vec2    boxes_vec2;\n"
            + "     int     boxes_int;\n"
            + "     uint    boxes_uint;\n"
            + "};\n"                                                                    //OUTPUT_FORMAT is a define you get by setting the last parameter in setImage() to true
            + "layout (OUTPUT_FORMAT) uniform writeonly image2D m_Output;\n"            //its the specified name in uppercase, followed by "_FORMAT"
            + "uniform vec4 m_Shift;"                                                   //you also get a define SOMENAME_WIDTH and SOMENAME_HEIGHT for any image set with name "SomeName" for example
            + ""                                                                        
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     ivec2 imgSize = ivec2(OUTPUT_WIDTH, OUTPUT_HEIGHT);\n"              //here the NAME_WIDTH and NAME_HEIGHT defines are used to avoid using GLSL imageSize() function that is similar to a uniform lookup usually
            + "     if (any(greaterThanEqual(x, imgSize))) {\n"
            + "         return;\n"
            + "     }\n"
            + "     vec4 v = vec4(1.0 - float(boxes_int));\n"
            + "     imageStore(m_Output, x, vec4(v));\n" //since we run exactly one invocation per output pixel (at least the out of bounds invocations returned already) just store data at pixel that relates to this invocation
            + "}\n";
}
