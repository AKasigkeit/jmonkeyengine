/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.SimpleApplication;
import com.jme3.buffer.pmb.MultiBufferRingBuffer;
import com.jme3.buffer.pmb.RingBuffer;
import com.jme3.buffer.pmb.RingBuffer.RingBufferBlock;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.IntBuffer;

/**
 *
 *
 * @author Alexander Kasigkeit
 */
public class TestRingBuffer extends SimpleApplication {

    //  2_000    1   0.0004
    //  2_000    2   0.00025  =  x1.6
    //  2_000    3   0.00024  =  x1.6
    // 16_000    1   0.0017   
    // 16_000    2   0.0013   =  x1.3
    // 16_000    3   0.0013   =  x1.3
    // 64_000    1   0.008    
    // 64_000    2   0.005    =  x1.6
    // 64_000    3   0.005    =  x1.6
    // the 64_000 objects, 3 buffer blocks example is giving me a frame time of 0.005 seconds (= 200 FPS)
    // thos objects take 5_120_000 bytes (= 5.12 MB), which is 1 GB of data each second (200 fps * 5 mb data).
    //
    private static final int MULTI_BUFFERING = 3;
    private static final int NUM_OBJS = 64_000;
    private static final float SCREEN_RANGE = 50f;

    public static void main(String[] args) {
        TestRingBuffer t = new TestRingBuffer();
        t.start();
    }

    private MultiBufferRingBuffer ringBuffer;
    private Mesh mesh = null;
    private VertexBuffer[] posBuffer, texBuffer;
    private RingBufferBlock currentBlock = null;
    private float[] SEEDS = new float[NUM_OBJS];
    private float time = 0f;

    @Override
    public void simpleInitApp() {
        for (int i = 0; i < SEEDS.length; i++) {
            SEEDS[i] = (float) (Math.random() * SCREEN_RANGE);
        }

        int bytesPerVertex = (3 + 2) * 4; //3 position, 2 texCoord, with 4 bytes each
        int numVerts = 4;
        int bytesPerObject = numVerts * bytesPerVertex;
        int totalSize = NUM_OBJS * bytesPerObject;
        System.out.println("total bytes per block: " + totalSize);

        ringBuffer = new MultiBufferRingBuffer(renderer, totalSize, MULTI_BUFFERING);
        posBuffer = new VertexBuffer[MULTI_BUFFERING];
        texBuffer = new VertexBuffer[MULTI_BUFFERING];
        for (int i = 0; i < MULTI_BUFFERING; i++) {
            posBuffer[i] = ringBuffer.getBuffer(i).asVertexBuffer(VertexBuffer.Type.Position, VertexBuffer.Format.Float, 3, bytesPerVertex, 0);
            texBuffer[i] = ringBuffer.getBuffer(i).asVertexBuffer(VertexBuffer.Type.TexCoord, VertexBuffer.Format.Float, 2, bytesPerVertex, 3 * 4);
        }
        VertexBuffer indexBuffer = new VertexBuffer(VertexBuffer.Type.Index);
        IntBuffer indexData = BufferUtils.createIntBuffer(NUM_OBJS * 6);
        for (int i = 0; i < NUM_OBJS; i++) {
            int startIndex = i * 4;
            indexData.put(startIndex + 0).put(startIndex + 1).put(startIndex + 2).put(startIndex + 2).put(startIndex + 3).put(startIndex + 0);
        }
        indexBuffer.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.UnsignedInt, indexData);

        mesh = new Mesh();
        mesh.setBuffer(indexBuffer);
        mesh.setBuffer(posBuffer[0]);
        mesh.setBuffer(texBuffer[0]);

        Geometry geo = new Geometry("quad", mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg"));
        geo.setMaterial(mat);
        geo.setCullHint(Spatial.CullHint.Never);
        rootNode.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(geo);

        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(25.244621f, 28.558134f, 68.619896f));
        cam.setRotation(new Quaternion(-1.2349925E-4f, 0.99981344f, -0.018064793f, -0.006834867f));
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;

        //this needs some explanation:
        //usually it would be called after the draw calls that make use of the uploaded
        //data have been made. However to keep this example simple i do it here which basically
        //still seperates the previous draw calls that used the data (the ones from the previous frame)
        //from the upcoming ones using the block.
        //the only difference is, doing it this way would result in the sync object waiting for unneccessary commands to finish,
        //however in this example i dont render anything else, thus there are no draw calls after the ones making use
        //of the data and the sync object will sync basically the corrert GL calls
        if (currentBlock != null) {
            currentBlock.finish();
        }

        //grab next block and fill it with bunch of CPU calculated data
        currentBlock = ringBuffer.next();
        for (int i = 0; i < SEEDS.length; i++) {
            float x = FastMath.sin(SEEDS[i] + time) + SEEDS[i];
            float y = FastMath.cos(SEEDS[i] + time) + SEEDS[i];
            float z = SEEDS[i] / SCREEN_RANGE;
            //              X            Y            Z            U            V
            currentBlock.putFloat(x).putFloat(y).putFloat(z).putFloat(0f).putFloat(0f)
                    .putFloat(x + 1f).putFloat(y).putFloat(z).putFloat(1f).putFloat(0f)
                    .putFloat(x + 1f).putFloat(y + 1f).putFloat(z).putFloat(1f).putFloat(1f)
                    .putFloat(x).putFloat(y + 1f).putFloat(z).putFloat(0f).putFloat(1f);
        }
        mesh.clearBuffer(VertexBuffer.Type.Position);
        mesh.clearBuffer(VertexBuffer.Type.TexCoord);
        mesh.setBuffer(posBuffer[currentBlock.getIndex()]);
        mesh.setBuffer(texBuffer[currentBlock.getIndex()]);
    }

}
