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
public class ComputeShaderTestInversion extends SimpleApplication {

    public static void main(String[] args) {
        ComputeShaderTestInversion test = new ComputeShaderTestInversion();
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

        Texture orig = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        orig.setAnisotropicFilter(0);
        orig.setMagFilter(Texture.MagFilter.Bilinear);
        orig.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        
        //create image with same settings but without data
        width = orig.getImage().getWidth();
        height = orig.getImage().getHeight();
        Texture edit = new Texture2D(width, height, Image.Format.RGBA16F);
        edit.setAnisotropicFilter(0);
        edit.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        edit.setMagFilter(Texture.MagFilter.Bilinear);
        
        //create 2 boxes just to show the textures on the screen
        Mesh box = new Box(1f, 1f, 1f);
        
        Geometry geoOrig = new Geometry("orig", box);
        Material matOrig = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matOrig.setTexture("ColorMap", orig);
        geoOrig.setMaterial(matOrig);
        geoOrig.setLocalTranslation(-2f, 0f, 5f);
        rootNode.attachChild(geoOrig);
        
        Geometry geoEdit = new Geometry("orig", box);
        Material matEdit = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matEdit.setTexture("ColorMap", edit);
        geoEdit.setMaterial(matEdit);
        geoEdit.setLocalTranslation(2f, 0f, 5f);
        rootNode.attachChild(geoEdit);
        
        //get maximum local sizes for dimensions x and y, considering this is a 2D problem
        localSizeX = factory.getMaxLocalSize2D()[0];
        localSizeY = factory.getMaxLocalSize2D()[1];
        //setup the shader
        shader = factory.createComputeShader(SHADER_SOURCE, "GLSL430");
        shader.setDefine("LOCAL_SIZE_X", VarType.Int, localSizeX);
        shader.setDefine("LOCAL_SIZE_Y", VarType.Int, localSizeY);
        shader.setTexture("Input", VarType.Texture2D, orig);
        shader.setImage("Output", VarType.Texture2D, edit, Texture.Access.WriteOnly, 0, -1, true);
        shader.setVector4("Shift", new Vector4f(-0.5f, -0.5f, -0.5f, -0.5f));
        
        shader.run(width, height, localSizeX, localSizeY, MemoryBarrierBits.ALL);
    }

    //To keep the example a single file, the shader source is kept here as a string
    //in most cases, this would be put into a .comp file similar to other shaders
    //and loaded using the ComputeShaderFactory's other create() method
    private static final String SHADER_SOURCE
            = "layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y) in;\n" //LOCAL_SIZE_X and LOCAL_SIZE_Y are defines set via java after querying GL for the implementation dependant maxima
            + "uniform sampler2D m_Input;\n"                                            //OUTPUT_FORMAT is a define you get by setting the last parameter in setImage() to true
            + "layout (OUTPUT_FORMAT) uniform writeonly image2D m_Output;\n"            //its the specified name in uppercase, followed by "_FORMAT"
            + "uniform vec4 m_Shift;"                                                   //you also get a define SOMENAME_WIDTH and SOMENAME_HEIGHT for any image set with name "SomeName" for example
            + ""                                                                        
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     ivec2 imgSize = ivec2(OUTPUT_WIDTH, OUTPUT_HEIGHT);\n"              //here the NAME_WIDTH and NAME_HEIGHT defines are used to avoid using GLSL imageSize() function that is similar to a uniform lookup usually
            + "     if (any(greaterThanEqual(x, imgSize))) {\n"
            + "         return;\n"
            + "     }\n"
            + "     vec2 uv = (vec2(x) + 0.5) / vec2(imgSize);\n" //since uvs are in the center of a fragment, offset invocation by 0.5 and divide by image size to get uv in range 0-1
            + "     vec4 pix = texture(m_Input, uv);\n" 
            //+ "     float v = dot(pix.xyz, vec3(0.333));\n"
            + "     vec4 v = 1.0 - pix + m_Shift;\n"
            + "     imageStore(m_Output, x, vec4(v));\n" //since we run exactly one invocation per output pixel (at least the out of bounds invocations returned already) just store data at pixel that relates to this invocation
            + "}\n";
}
