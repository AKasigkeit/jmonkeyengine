/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.buffer.DrawIndirectBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.buffer.pmb.MultiBufferRingBuffer;
import com.jme3.buffer.pmb.RingBuffer.RingBufferBlock;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * Draws several geometries with a single DrawCall using MultiDrawIndirect,
 * uploads transformation via MultiBufferRingBuffer. For simplicity we will
 * render some Quads (but as they are generated in code, it could be any even
 * totally different shapes)
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class TestMultiDrawIndirect extends SimpleApplication {

    private static final int MULTI_BUFFERING = 2;
    private static final int NUM_QUADS = 16_000;

    public static void main(String[] args) {
        TestMultiDrawIndirect t = new TestMultiDrawIndirect();
        t.start();
    }

    private UntypedBuffer indexBuffer, vertexBuffer, drawCommandBuffer;
    private MultiBufferRingBuffer streamedData = null;
    private VertexBuffer[] streamedVBO = new VertexBuffer[MULTI_BUFFERING];
    private Mesh mesh = null;
    private final Vector3f[] POSITIONS = new Vector3f[NUM_QUADS];

    @Override
    public void simpleInitApp() {
        //create a buffer for the index data
        indexBuffer = UntypedBuffer.createNewStorageDirect(UntypedBuffer.MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        indexBuffer.initialize(NUM_QUADS * 6 * 4); //6 indices per quad with 2 bytes each (could be shared in this case where all shapes are the same but to not "cheat" here, i upload it once per quad)
        VertexBuffer indexIBO = indexBuffer.asIndexBuffer(VertexBuffer.Format.UnsignedInt);

        //create a buffer for the vertex data
        vertexBuffer = UntypedBuffer.createNewStorageDirect(UntypedBuffer.MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        vertexBuffer.initialize(NUM_QUADS * 4 * 6 * 4); //4 verts per quad, with 6 floats (3 position and 3 color) with 4 bytes each
        VertexBuffer positionVBO = vertexBuffer.asVertexBuffer(VertexBuffer.Type.Position, VertexBuffer.Format.Float, 3, 24, 0);
        VertexBuffer colorVBO = vertexBuffer.asVertexBuffer(VertexBuffer.Type.Color, VertexBuffer.Format.Float, 3, 24, 12);

        //now fill them with data
        generateQuads();

        //prepare the MultiBuffer and per-instance vertex attribues
        //for each quad the world translation is streamed, thats 3 floats per quad
        streamedData = new MultiBufferRingBuffer(renderer, NUM_QUADS * 3 * 4, MULTI_BUFFERING);
        for (int i = 0; i < MULTI_BUFFERING; i++) {
            streamedVBO[i] = streamedData.getBuffer(i).asVertexBuffer(VertexBuffer.Type.Translation, VertexBuffer.Format.Float, 3, 12, 0);
            streamedVBO[i].setInstanceSpan(1);
        }

        //create the DrawIndirectBuffer
        drawCommandBuffer = UntypedBuffer.createNewStorageDirect(UntypedBuffer.MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        drawCommandBuffer.initialize(NUM_QUADS * 5 * 4); //5 ints per draw command, 4 bytes each
        DrawIndirectBuffer dibo = drawCommandBuffer.asDrawIndirectBuffer(DrawIndirectBuffer.DrawIndirectMode.DrawIndices);
        //and generate the draw commands
        int ind = 0, vert = 0;
        ByteBuffer buffer = BufferUtils.createByteBuffer(NUM_QUADS * 5 * 4); //for simplicity, create and sent all at once
        for (int i = 0; i < NUM_QUADS; i++) {
            buffer.putInt(6) //numIndices 
                    .putInt(1) //numInstances
                    .putInt(ind) //indexOffset
                    .putInt(vert) //vertexOffset
                    .putInt(i); //instanceOffset
            ind += 6;
            vert += 4;
        }
        buffer.flip();
        drawCommandBuffer.updateData(buffer, 0);

        //now create the mesh
        mesh = new Mesh();
        mesh.setBuffer(indexIBO);
        mesh.setBuffer(positionVBO);
        mesh.setBuffer(colorVBO);
        mesh.setDrawIndirectBuffer(dibo);
        mesh.setDrawIndirectParameters(0, 0, NUM_QUADS);

        //and a Geometry 
        Geometry geo = new Geometry("all_the_quads", mesh);
        Material mat = new Material(assetManager, "jme3test/mdi/MultiDrawIndirect.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        geo.setMaterial(mat);
        geo.setCullHint(Spatial.CullHint.Never);
        rootNode.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(geo);

        //init the positions used to translate all the quads
        for (int i = 0; i < NUM_QUADS; i++) {
            POSITIONS[i] = new Vector3f((float) (Math.random()), (float) (Math.random()), (float) (Math.random())).multLocal(128).subtractLocal(64, 64, 64);
        }
        stateManager.attach(new DetailedProfilerState());
        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(0f, 40f, 200f));
        cam.lookAtDirection(new Vector3f(0f, -0.2f, -0.9f).normalizeLocal(), Vector3f.UNIT_Y);
    }

    private RingBufferBlock activeBlock;

    @Override
    public void simpleUpdate(float tpf) {
        //stream translations
        activeBlock = streamedData.next();
        for (int i = 0; i < NUM_QUADS; i++) {
            Vector3f pos = POSITIONS[i];
            pos.addLocal(tpf * FastMath.sin(i / 32f), tpf * 3, tpf * FastMath.cos(i / 128f));
             
            //ugly wrapping
            if (pos.x > 64) { pos.x = -64;
            } else if (pos.x < -64) { pos.x = 64; }
            if (pos.y > 64) { pos.y = -64;
            } else if (pos.y < -64) {  pos.y = 64; }
            if (pos.z > 64) { pos.z = -64;
            } else if (pos.z < -64) { pos.z = 64; }
            
            activeBlock.putFloat(pos.x).putFloat(pos.y).putFloat(pos.z);
        }

        //update per-instance vertex buffers 
        mesh.clearBuffer(VertexBuffer.Type.Translation);
        mesh.setBuffer(streamedVBO[activeBlock.getIndex()]);

    }

    public void postRender() {
        activeBlock.finish();
    }

    private void generateQuads() {
        //again for simplicity, generate and upload all at once instead of doing batches
        //create indices 
        ByteBuffer buffer = BufferUtils.createByteBuffer(NUM_QUADS * 4 * 6 * 4); //big enough for the vertices further down already
        for (int i = 0; i < NUM_QUADS; i++) {
            buffer.putInt(0).putInt(1).putInt(2).putInt(2).putInt(3).putInt(0);
        }
        buffer.flip();
        indexBuffer.updateData(buffer, 0);

        //create vertices
        buffer.clear();
        for (int i = 0; i < NUM_QUADS; i++) {
            //              X           Y           Z           R           G           B
            buffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat((float) Math.random()).putFloat((float) Math.random()).putFloat((float) Math.random()); //ugly bunch of Math random
            buffer.putFloat(1f).putFloat(0f).putFloat(0f).putFloat((float) Math.random()).putFloat((float) Math.random()).putFloat((float) Math.random());
            buffer.putFloat(1f).putFloat(1f).putFloat(0f).putFloat((float) Math.random()).putFloat((float) Math.random()).putFloat((float) Math.random());
            buffer.putFloat(0f).putFloat(1f).putFloat(0f).putFloat((float) Math.random()).putFloat((float) Math.random()).putFloat((float) Math.random());
        }
        buffer.flip();
        vertexBuffer.updateData(buffer, 0);
    }

}
