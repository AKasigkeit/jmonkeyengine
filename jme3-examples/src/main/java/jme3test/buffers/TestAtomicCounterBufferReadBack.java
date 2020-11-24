/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.AtomicCounterBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag; 
import com.jme3.material.Material;
import com.jme3.renderer.compute.ComputeShader;
import com.jme3.renderer.compute.MemoryBarrier;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * Test to demonstate usage of AtomicCounterBuffers in ComputeShaders. This test
 * visualizes the order in which ComputeShader invocations are run. Take a look
 * at
 * https://www.geeks3d.com/20120309/opengl-4-2-atomic-counter-demo-rendering-order-of-fragments/
 * except this test uses ComputeShaders instead of FragmentShaders
 *
 * @author Alexander Kasigkeit
 */
public class TestAtomicCounterBufferReadBack extends TestUtil.AutoScreenshotApp {

    public static void main(String[] args) {
        TestAtomicCounterBufferReadBack t = new TestAtomicCounterBufferReadBack();
        t.start();
    }

    @Override
    public void simpleInitApp() {
        super.simpleInitApp();
        UntypedBuffer buffer = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        buffer.initialize(4); // 4 bytes = 1 uint
        AtomicCounterBuffer acBuffer = buffer.asAtomicCounterBuffer(0);
        acBuffer.setValues(0);
        
        Texture2D tex = new Texture2D(512, 512, Image.Format.RGBA16F);

        Geometry geo = new Geometry("testgeo", new Box(1f, 1f, 1f));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", tex);
        geo.setMaterial(mat);
        rootNode.attachChild(geo);
 
        ComputeShader shader = ComputeShader.createFromString(renderer, SHADER_SOURCE, "GLSL430");
        shader.setAtomicCouterBuffer("Counter", acBuffer);
        shader.setImage("Output", VarType.Texture2D, tex, Texture.Access.WriteOnly, 0, -1, true);
        
        MemoryBarrier barrier = renderer.createMemoryBarrier(MemoryBarrier.Flag.TextureFetch, MemoryBarrier.Flag.AtomicCounter);
        shader.run(512, 512, 32, 32, barrier);

        int[] res = new int[1];
        acBuffer.getValues(res);
        System.out.println("COUNTER: "+res[0]+" / "+(512 * 512));
        
        flyCam.setMoveSpeed(10f);
    }

    private static final String SHADER_SOURCE = ""
            + "layout (local_size_x = 32, local_size_y = 32) in;\n"
            + ""
            + "layout (binding = 0) uniform atomic_uint m_Counter;\n"
            + "layout (OUTPUT_FORMAT) uniform writeonly image2D m_Output;\n"
            + ""
            + "void main(void) {\n"
            + "     ivec2 x = ivec2(gl_GlobalInvocationID.xy);\n"
            + "     uint val = atomicCounterIncrement(m_Counter);\n"
            + "     imageStore(m_Output, x, vec4(float(val) / (512.0 * 512.0)));\n"
            + "}\n";
}
