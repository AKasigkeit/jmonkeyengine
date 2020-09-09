/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene.manager;

import com.jme3.bounding.BoundingVolume;
import com.jme3.buffer.DrawIndirectBuffer;
import com.jme3.buffer.DrawIndirectBuffer.DrawCommand;
import com.jme3.buffer.DrawIndirectBuffer.DrawIndicesCommand;
import com.jme3.buffer.DrawIndirectBuffer.DrawIndirectMode;
import com.jme3.buffer.pmb.RingBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.buffer.pmb.MultiBufferRingBuffer;
import com.jme3.buffer.pmb.RingBuffer.RingBufferBlock;
import com.jme3.buffer.pmb.SingleBufferRingBuffer;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author Alexander Kasigkeit
 */
public class MDISystem {

    /* 
    EXAMPLE FOR 2 TRIANGLES with instanceID and WorldMatrix provided
    initialize() called with Position, Float, 3  and TexCoord, Float, 2
                             TexCoord2 Float, 1
    so Buffers:
                       TRI 1                                           TRI 2
    indexData   :      0, 1, 2,                                        0, 1, 2                                          (all ints for simplicity)
    vertexData  :      0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1,    0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 1      (all floats for simplicity)
    instanceData:      2, 0                                            5, 1                                             (texCoord2 float and instanceId int)
    worldMatrix :      1, 2, 3, 4, 2, 3, 4, 5, 3, 4, 5, 6, 4, 5, 6, 7, 1, 2, 3, 4, 2, 3, 4, 5, 3, 4, 5, 6, 4, 5, 6, 7   (all mat4 floats)
                                                                                                                        -> usually those are byte buffers
    and DrawIndicesCommands:
        count           3               3
        instanceCount   1               1
        firstIndex      0               3
        baseVertex      0               3 
        baseInstance    0               1
     
     */
    private final DrawIndirectMode MODE;
    private final RenderManager RENDER_MANAGER;
    private final Material MAT;
    private final MDIGeometryInstanceComparator INST_COMP = new MDIGeometryInstanceComparator();
    private final MDIGeometryDistanceComparator DIST_COMP = new MDIGeometryDistanceComparator();
    private boolean init = false;

    private Mesh.Mode meshMode = Mesh.Mode.Triangles;
    private int patchModeSize = 3;
    private boolean provideInstanceId = false;
    private boolean provideWorldMatrix = false;
    private int multiBuffering = 2;
    private boolean autoCalculateBounds = false;

    private UntypedBuffer indexData = null;     //for IBO
    private UntypedBuffer vertexData = null;    //for VBOs
    private UntypedBuffer instanceData = null;  //for static instance VBOs
    private RingBuffer worldMatrixData = null;  //for streamed instance VBO: WorldMatrix
    private RingBuffer drawIndirectData = null; //for MDI

    private int vertexStride = 0;               //stride of one vertex data block
    private int rawInstanceStride = 0;          //stride of one instance data block without WorldMatrix or InstanceID
    private int instanceStride = 0;             //stride of one instance data block
    private VertexBuffer indexBuffer = null;    //IBO 
    private VertexBuffer[] vertBuffers = null;  //VBOs
    private VertexBuffer[] instBuffers = null;  //instance VBOs incl WorldMatrix and InstanceId
    private DrawIndirectBuffer drawIndirectBuffer = null; //MDI
    private int drawBufferCapacity = 0;

    private int currentInstanceCount = 0;
    private int instanceId = 0; //only used if no instanceData, instanceId or WorldMatrix are in use
    private int lastIndexByte = 0;
    private int lastVertByte = 0;
    private int lastInstByte = 0;

    private int gapCounter = 0;
    private int[] gapsIndex = new int[16];      //offset-size-pair
    private int[] gapsVertex = new int[16];     //offset-size-pair
    private int[] gapsInstance = new int[16];   // 
    private int[] gapsWorldData = new int[16];  //

    private Mesh mesh = null;
    private Geometry dummyGeo = null;
    private ByteBuffer copyBuffer = null;
    private MDIGeometry[] sortGeos = new MDIGeometry[0];

    public MDISystem(Material mat, RenderManager renderManager, DrawIndirectMode mode) {
        if (mat == null || mode == null || renderManager == null) {
            throw new IllegalArgumentException("mat, renderManager and mode cannot be null");
        }
        MAT = mat;
        RENDER_MANAGER = renderManager;
        MODE = mode;
    }

    /**
     * Call this prior to initialize() to change the mode the mesh will be in.
     *
     * @param mode the mode to use
     * @param patchSize the patch size in case mode is Mode.Patches. ignored
     * otherwise
     * @return this MDISystem for convenience
     */
    public MDISystem setMeshMode(Mesh.Mode mode, int patchSize) {
        if (init) {
            throw new UnsupportedOperationException("this method can only be called prior to initializing this MDISystem");
        }
        meshMode = mode;
        patchModeSize = patchSize;
        return this;
    }

    /**
     * Call this prior to initialize() to enable automatic calculation of model
     * bounds. ie, when creating a new instance with createInstance() the
     * argument for modelBounds can be null and it will automatically be
     * calculated from the provided vertex data. <b>This requires one of the
     * provided VertexBuffer.Types in the initialize() call to be of type
     * Position, that its VertexBuffer.Format needs to be Float and its count
     * needs to be 3</b>
     *
     * @return this MDISystem for convenience
     */
    public MDISystem setAutoCalculateBounds() {
        if (init) {
            throw new UnsupportedOperationException("this method can only be called prior to initializing this MDISystem");
        }
        autoCalculateBounds = true;
        return this;
    }

    /**
     * Can be used to provide an <code>in int inInstanceId;</code> to the
     * VertexShader that contains the id of the instance the vertex currently
     * beeing processed belongs to. Can be used to access additional data
     * provided via SSBOs or UBOs that are instance dependant
     *
     * @return this MDISystem for convenience
     */
    public MDISystem setProvideInstanceId() {
        if (init) {
            throw new UnsupportedOperationException("this method can only be called prior to initializing this MDISystem");
        }
        provideInstanceId = true;
        return this;
    }

    /**
     * Call this prior to initialize() to stream the WorldMatrix of each
     * instance to be available in the VertexShader via<br>
     * <code>in mat4 inWorldMatrix;</code><br>
     * and can be used to transform the Vertex into WorldSpace via<br>
     * <code>vec4 worldPos = inWorldMatrix * vec4(inPosition, 1.0);</code><br>
     * works well together with UniformBinding.CameraBuffer so the final
     * position can be calculated via:<br>
     * <code>gl_Position = cam_viewProjectionMatrix * worldPos;</code><br>
     * to reduce the number of uniform uploads to a minimum
     *
     * @return this MDISystem for convenience
     */
    public MDISystem setProvideWorldMatrix() {
        if (init) {
            throw new UnsupportedOperationException("this method can only be called prior to initializing this MDISystem");
        }
        provideWorldMatrix = true;
        return this;
    }

    /**
     * Set to 2 to enable double buffering for the indirect draw commands, set
     * to 1 to disable it and set to 3 to enable tripple buffering
     *
     * @param numBuffers number of buffers to use, must be in range 1, 3
     *
     * @return this MDISystem for convenience
     */
    public MDISystem setDrawIndirectMultiBuffering(int numBuffers) {
        if (init) {
            throw new UnsupportedOperationException("this method can only be called prior to initializing this MDISystem");
        }
        if (numBuffers < 1 || numBuffers > 3) {
            throw new IllegalArgumentException("numBuffers must be in range 1, 3");
        }
        multiBuffering = numBuffers;
        return this;
    }

    public MDISystem initialize(
            VertexBuffer.Type[] vertexTypes, VertexBuffer.Format[] vertexFormats, int[] vertexCounts,
            VertexBuffer.Type[] instanceTypes, VertexBuffer.Format[] instanceFormats, int[] instanceCounts,
            int avgIndices, int avgVertices, int initInstances) {
        if (init) {
            throw new UnsupportedOperationException("this MDISystem is already initialized");
        } else if (vertexTypes.length != vertexFormats.length || vertexFormats.length != vertexCounts.length || vertexCounts.length < 1) {
            throw new IllegalArgumentException("vertexTypes, vertexFormats and vertexCounts must be equal in length and greater than 0");
        } else if (instanceTypes.length != instanceFormats.length || instanceFormats.length != instanceCounts.length) {
            throw new IllegalArgumentException("instanceTypes, instanceFormats and instanceCounts must be equal in length");
        } else if (MODE == DrawIndirectMode.Draw && avgIndices > 0) {
            throw new IllegalArgumentException("This MDISystem is in DrawIndirectMode.Draw and cannot use indices");
        } else if (MODE == DrawIndirectMode.DrawIndices && avgIndices < 1) {
            throw new IllegalArgumentException("This MDISystem is in DrawIndirectMode.DrawIndices, so avgIndices needs to be at least 1");
        } else if (avgVertices < 1 || initInstances < 1) {
            throw new IllegalArgumentException("avgVertices and avgIndices need to be at least 1");
        }
        if (autoCalculateBounds) {
            boolean found = false;
            for (int i = 0; i < vertexTypes.length; i++) {
                if (vertexTypes[i] == VertexBuffer.Type.Position
                        && vertexFormats[i] == VertexBuffer.Format.Float
                        && vertexCounts[i] == 3) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("when automatic bounds calculation is turned on, one of the provided vertex attributes has to be of type Position, format Float and counts 3");
            }
        }

        //calculate strides
        vertexStride = calculateStride(vertexFormats, vertexCounts);
        rawInstanceStride = calculateStride(instanceFormats, instanceCounts);
        instanceStride = rawInstanceStride + (provideInstanceId ? 4 : 0);

        //setup data buffers
        Renderer renderer = RENDER_MANAGER.getRenderer();
        if (MODE == DrawIndirectMode.DrawIndices) {
            indexData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
            indexData.initialize(4 * avgIndices * initInstances);
        }
        vertexData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        vertexData.initialize(vertexStride * avgVertices * initInstances);
        instanceData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, renderer, StorageFlag.Dynamic);
        instanceData.initialize(instanceStride * initInstances);
        if (provideWorldMatrix) {
            worldMatrixData = new MultiBufferRingBuffer(renderer, 64 * initInstances, multiBuffering); //64 bytes per mat4: 4x4 floats = 16x4 bytes
        }

        //setup IBO if present
        if (MODE == DrawIndirectMode.DrawIndices) {
            indexBuffer = indexData.asIndexBuffer(VertexBuffer.Format.UnsignedInt);
        }
        //setup VBO
        vertBuffers = new VertexBuffer[vertexTypes.length];
        int vertOffsetBytes = 0;
        for (int i = 0; i < vertBuffers.length; i++) {
            vertBuffers[i] = vertexData.asVertexBuffer(vertexTypes[i], vertexFormats[i], vertexCounts[i], vertexStride, vertOffsetBytes);
            vertOffsetBytes += vertexFormats[i].getComponentSize() * vertexCounts[i];
        }
        //setup instance VBOs if present
        instBuffers = new VertexBuffer[instanceTypes.length + (provideInstanceId ? 1 : 0) + (provideWorldMatrix ? 1 : 0)];
        int instOffsetBytes = 0;
        for (int i = 0; i < instanceTypes.length; i++) {
            instBuffers[i] = instanceData.asVertexBuffer(instanceTypes[i], instanceFormats[i], instanceCounts[i], instanceStride, instOffsetBytes);
            instBuffers[i].setInstanceSpan(1);
            instOffsetBytes += instanceFormats[i].getComponentSize() * instanceCounts[i];
        }
        if (provideInstanceId) {
            instBuffers[instanceTypes.length] = instanceData.asVertexBuffer(VertexBuffer.Type.InstanceID, VertexBuffer.Format.Float, 1, instanceStride, instOffsetBytes);
            instBuffers[instanceTypes.length].setInstanceSpan(1);
        }
        if (provideWorldMatrix) {
            instBuffers[instBuffers.length - 1] = worldMatrixData.getBuffer().asVertexBuffer(VertexBuffer.Type.WorldMatrix, VertexBuffer.Format.Float, 16, 16, 0);
            instBuffers[instBuffers.length - 1].setInstanceSpan(1);
        }

        //setup draw indirect buffer
        int bytesPerDrawCommand = MODE == DrawIndirectMode.Draw ? 16 : 20; //4 or 5 ints dependant on the command type
        drawIndirectData = new SingleBufferRingBuffer(renderer, bytesPerDrawCommand * initInstances, multiBuffering);
        drawIndirectBuffer = drawIndirectData.getBuffer().asDrawIndirectBuffer(MODE);
        drawBufferCapacity = initInstances;

        //create VAO to put it all together
        mesh = new Mesh();
        mesh.setMode(meshMode);
        mesh.setPatchVertexCount(patchModeSize);

        if (indexBuffer != null) {
            mesh.setBuffer(indexBuffer);
        }
        for (VertexBuffer vertB : vertBuffers) {
            mesh.setBuffer(vertB);
        }
        for (VertexBuffer instB : instBuffers) {
            mesh.setBuffer(instB);
        }
        mesh.setDrawIndirectBuffer(drawIndirectBuffer);
        dummyGeo = new Geometry("MDISystemGeo", mesh);
        dummyGeo.setMaterial(MAT);
        dummyGeo.setIgnoreTransform(true);
        init = true;
        return this;
    }

    /**
     * Creates a new instance with the provided BoundingVolume, indexBuffer,
     * vertexData and instanceData. The only data that will be taken into
     * account is the data between a buffers current position and its limit.
     * each buffers remaining() has to be a multiple of its corresponding data
     * stride, ie for indexBuffer the remaining bytes have to be evenly
     * divisible by 4, for vertexData and instanceData the stride depends on the
     * arguments provided in the initialize() method
     *
     * @param indexBuffer index data to use if this MDISystem is in
     * DrawIndirectMode.DrawIndices, null otherwise
     * @param vertexBuffer the vertexData to use for this instance
     * @param instanceBuffer the instanceData to use for this instance
     * @param modelBounds the bounding volume of this instance, has to be in
     * model space
     *
     * @return a new MDIGeometry
     */
    public MDIGeometry createInstance(ByteBuffer indexBuffer, ByteBuffer vertexBuffer, ByteBuffer instanceBuffer, BoundingVolume modelBounds) {
        if (MODE == DrawIndirectMode.DrawIndices && indexBuffer.remaining() % 4 != 0) {
            throw new IllegalArgumentException("provided indexBuffer.remaining % 4 is != 0, however must be evenly divisible by 4 because it must contain ints");
        } else if (vertexBuffer.remaining() % vertexStride != 0) {
            throw new IllegalArgumentException("provided vertexBuffer.remaining % vertexStride is != 0, however must be evenly divisible");
        } else if (instanceBuffer != null && instanceBuffer.remaining() != rawInstanceStride) {
            throw new IllegalArgumentException("provided instanceBuffer.remaining must be equal to rawInstanceStride");
        } else if (rawInstanceStride > 0 && instanceBuffer == null) {
            throw new IllegalArgumentException("instanceBuffer cannot be null when instanced data has been setup in the initialize() method");
        }
        int indexSize = indexBuffer != null ? indexBuffer.remaining() : 0;
        int vertexSize = vertexBuffer.remaining();
        int indexOffset = -1;
        int vertexOffset = -1;
        int instanceOffset = -1;
        int worldMatrixOffset = -1;

        //now check gaps if we can upload data there
        //index buffer first
        if (MODE == DrawIndirectMode.DrawIndices) {
            indexOffset = findOffset(gapsIndex, indexSize, lastIndexByte);
            indexData.updateData(indexBuffer, indexOffset); //will grow automatically if needed
            if (indexOffset == lastIndexByte) {
                lastIndexByte += indexSize;
            }
        }
        //now vertex data
        vertexOffset = findOffset(gapsVertex, vertexBuffer.remaining(), lastVertByte);
        vertexData.updateData(vertexBuffer, vertexOffset); //will grow automatically if needed
        if (vertexOffset == lastVertByte) {
            lastVertByte += vertexBuffer.remaining();
        }
        //now instanceData 
        //if we have either instanceData or instanceId we can calculate new instId from that
        //else if we have WorldMatrix, we can calculate new instId from that
        //else we could keep track of deleted MDIGeometries instIds and reuse them
        //but we might as well just always increment because if we dont provide an instanceId
        //there is no way to index into instance specific data anyway and we can easily provide
        //an unique instId for the CPU to use

        int instId = -1;
        if (instanceStride > 0) {
            instanceOffset = findOffset(gapsInstance, instanceStride, lastInstByte);
            if (instanceOffset % instanceStride != 0) {
                throw new RuntimeException("some bug deep down, have fun searching!");
            }
            instId = instanceOffset / instanceStride;
            ByteBuffer instanceDataBuffer = instanceBuffer;
            if (provideInstanceId) {
                copyBuffer = ensureCapacity(copyBuffer, instanceStride);
                if (instanceBuffer != null) {
                    copyBuffer.put(instanceBuffer);
                }
                copyBuffer.putFloat(instId);
                copyBuffer.flip();
                instanceDataBuffer = copyBuffer;
            }
            instanceData.updateData(instanceDataBuffer, instanceOffset);
            if (instanceOffset == lastInstByte) {
                lastInstByte += instanceStride;
            }
        } else if (provideWorldMatrix) {
            //ie only add to gapsWorldData if instanceStride == 0
            worldMatrixOffset = findOffset(gapsWorldData, 64, (instanceId * 64));
            instId = worldMatrixOffset / 64;
        } else {
            instId = instanceId;
        }

        MDIGeometry geo;
        if (MODE == DrawIndirectMode.Draw) {
            DrawCommand dc = new DrawCommand(
                    vertexSize / vertexStride, //count
                    1, //instanceCount
                    vertexOffset / vertexStride,//first
                    instId);                    //baseInstance
            geo = new MDIGeometry(this, instId, indexOffset, indexSize, vertexOffset, vertexSize, dc, modelBounds);
        } else {
            DrawIndicesCommand dc = new DrawIndicesCommand(
                    indexSize / 4, //count 
                    1, //instanceCount
                    indexOffset / 4, //firstIndex
                    vertexOffset / vertexStride,//baseVertex
                    instId);                    //baseInstance
            geo = new MDIGeometry(this, instId, indexOffset, indexSize, vertexOffset, vertexSize, dc, modelBounds);
        }
        currentInstanceCount++;
        instanceId++;
        return geo;
    }

    /**
     * Does defragmentation on the GPU that can be caused when deleting
     * instances. Usually the implementation tries to reuse empty blocks of
     * memory in the buffers, however if the sizes of the instances that are
     * created and destroyed vary too much in size or are usually only
     * increasing, fragmentation will increase over time. The implementation
     * also keeps track of the ratio of fragmentation and does defragmentation
     * automatically, however this method can be called manually to provide more
     * flexibility.
     */
    public void gpuDefragmentation() {

    }

    /**
     * Deletes the provided instance from the GPU, cannot be used for rendering
     * anymore afterwards.
     *
     * @param geo the MDIGeometry to delete
     */
    public void deleteInstance(MDIGeometry geo) {

        currentInstanceCount--;
    }

    public void render() {

    }

    /**
     * Renders the provided MDIGeometries
     *
     * @param geos array containing the MDIGeometries to render
     * @param count the number to render
     */
    public void render(MDIGeometry[] geos, int count) {
        if (count > geos.length) {
            throw new IllegalArgumentException("provided array doesnt contain specified count of MDIGeometries to render");
        }

        if (count > drawBufferCapacity) {
            //resize draw indirect buffer
            drawIndirectData.unmap();
            int newDrawBufferCapacity = Math.max(count, drawBufferCapacity * 2);
            int newBytesCapacity = newDrawBufferCapacity * (MODE == DrawIndirectMode.Draw ? 16 : 20);
            drawIndirectData = new SingleBufferRingBuffer(RENDER_MANAGER.getRenderer(), newBytesCapacity, multiBuffering);
            drawIndirectBuffer = drawIndirectData.getBuffer().asDrawIndirectBuffer(MODE);
            drawBufferCapacity = newDrawBufferCapacity;
            mesh.setDrawIndirectBuffer(drawIndirectBuffer);
        }

        if (count > sortGeos.length) {
            sortGeos = new MDIGeometry[Math.max(count, sortGeos.length << 1)];
        } else {
            Arrays.fill(sortGeos, count, sortGeos.length, null);
        }
        System.arraycopy(geos, 0, sortGeos, 0, count);
        RingBufferBlock wmBlock = null;
        if (provideWorldMatrix) {
            //sort array by instance id for contiguous writes to GL
            Arrays.sort(sortGeos, INST_COMP);
            long start = System.nanoTime();
            wmBlock = worldMatrixData.next();
            //System.out.println("waiting for fence took: " + ((System.nanoTime() - start) / 1000000.0) + " ms");
            Matrix4f worldMatrix;
            long writeStart = System.nanoTime();
            for (MDIGeometry geo : sortGeos) {
                if (geo == null) {
                    break; //nulls sorted to the end
                }
                //geo.computeWorldMatrix();
                worldMatrix = geo.getWorldMatrix();
                wmBlock.setPosition(geo.ID * 64);
                wmBlock.putFloat(worldMatrix.m00).putFloat(worldMatrix.m10).putFloat(worldMatrix.m20).putFloat(worldMatrix.m30)
                        .putFloat(worldMatrix.m01).putFloat(worldMatrix.m11).putFloat(worldMatrix.m21).putFloat(worldMatrix.m31)
                        .putFloat(worldMatrix.m02).putFloat(worldMatrix.m12).putFloat(worldMatrix.m22).putFloat(worldMatrix.m32)
                        .putFloat(worldMatrix.m03).putFloat(worldMatrix.m13).putFloat(worldMatrix.m23).putFloat(worldMatrix.m33);
            }
            //System.out.println("writing took: " + ((System.nanoTime() - writeStart) / 1000000.0) + " ms for " + ((count * 64) / 1000.0 / 1000.0) + " MB");
            //because WorldMatrixData is a MultiBufferRingBuffer, make sure the VBO sources its data from the correct buffer
            mesh.getBuffer(VertexBuffer.Type.WorldMatrix).toViewOn(worldMatrixData.getBuffer());
        }
        //now sort by distance
        Arrays.sort(sortGeos, DIST_COMP);

        //and render 
        if (MODE == DrawIndirectMode.Draw) {
            renderDraw(count);
        } else {
            renderDrawIndices(count);
        }

        //if worldMatrix was streamed, put fence after the draw commands
        if (wmBlock != null) {
            wmBlock.finish();
        }
    }

    private void renderDraw(int count) {
        throw new UnsupportedOperationException("currently only rendering in DrawIndirectMode.DrawIndices is supported");
    }

    private void renderDrawIndices(int count) {
        RingBufferBlock drawCommandBlock = drawIndirectData.next();
        int dcPut = 0;
        for (int i = 0; i < count; i++) {
            MDIGeometry geo = sortGeos[i];
            DrawIndicesCommand drawCmd = geo.DRAW_INDICES_CMD;
            if (drawCmd == null) {
                throw new IllegalArgumentException("provided MDIGeometry is not in DrawIndirectMode.DrawIndices");
            }
            //flush draw commands 
            int offsetBytes = geo.ID * 5 * 4; //5 ints, 4 bytes each
            drawCommandBlock.setPosition(offsetBytes);
            drawCommandBlock.putInt(drawCmd.getCount()).putInt(drawCmd.getInstanceCount())
                    .putInt(drawCmd.getFirstIndex()).putInt(drawCmd.getBaseVertex()).putInt(drawCmd.getBaseInstance());
            dcPut++;
        }
        mesh.setDrawIndirectParameters(drawCommandBlock.getOffset(), 0, count);
        RENDER_MANAGER.renderGeometry(dummyGeo);
        drawCommandBlock.finish();
    }

    private int findOffset(int[] gaps, int neededBytes, int defaultValue) {
        int maxRange = (int) (neededBytes * 1.1f);
        int gapIndex = -1;
        for (int i = 0; i < gapCounter; i++) {
            int idx = i << 1;
            int range = gaps[idx + 1];
            if (range >= neededBytes && range <= maxRange) {
                gapIndex = idx;
                break;
            }
        }
        if (gapIndex == -1) {
            return defaultValue;
        }
        int offset = gaps[gapIndex];
        //shrink gap
        gaps[gapIndex] += neededBytes;
        gaps[gapIndex + 1] -= neededBytes;
        return offset;
    }

    private int calculateStride(VertexBuffer.Format[] formats, int[] counts) {
        int stride = 0;
        for (int i = 0; i < formats.length; i++) {
            int bytes = formats[i].getComponentSize() * counts[i];
            stride += bytes;
        }
        return stride;
    }

    public static class MDIGeometry extends Geometry {

        private final MDISystem SYSTEM;
        private final int ID;
        private final int INDEX_OFFSET, INDEX_SIZE,
                VERTEX_OFFSET, VERTEX_SIZE;
        private final DrawCommand DRAW_CMD;
        private final DrawIndicesCommand DRAW_INDICES_CMD;
        private final BoundingVolume MODEL_BOUND;
        protected float sortDistance;

        private MDIGeometry(MDISystem system, int index, int indexOffset, int indexSize, int vertexOffset, int vertexSize, DrawCommand drawCmd, BoundingVolume bounds) {
            SYSTEM = system;
            ID = index;
            INDEX_OFFSET = indexOffset;
            INDEX_SIZE = indexSize;
            VERTEX_OFFSET = vertexOffset;
            VERTEX_SIZE = vertexSize;
            DRAW_CMD = drawCmd;
            DRAW_INDICES_CMD = null;
            MODEL_BOUND = bounds;
        }

        private MDIGeometry(MDISystem system, int index, int indexOffset, int indexSize, int vertexOffset, int vertexSize, DrawIndicesCommand drawIndicesCmd, BoundingVolume bounds) {
            SYSTEM = system;
            ID = index;
            INDEX_OFFSET = indexOffset;
            INDEX_SIZE = indexSize;
            VERTEX_OFFSET = vertexOffset;
            VERTEX_SIZE = vertexSize;
            DRAW_CMD = null;
            DRAW_INDICES_CMD = drawIndicesCmd;
            MODEL_BOUND = bounds;
        }

        public DrawIndirectMode getMode() {
            return SYSTEM.MODE;
        }

        @Override
        public void setMaterial(Material mat) {
            throw new UnsupportedOperationException("MDIGeometry cannot change their Material, it is inherited from the MDISystem that instanciated this MDIGeometry");
        }

        @Override
        public void setMesh(Mesh mesh) {
            throw new UnsupportedOperationException("MDIGeometry cannot change their Mesh, it is part of the MDISystem that instanciated this MDIGeometry");
        }

        @Override
        protected void updateWorldBound() {
            refreshFlags &= ~RF_BOUND;
            if (MODEL_BOUND != null) {
                if (ignoreTransform) {
                    worldBound = MODEL_BOUND.clone(worldBound);
                } else {
                    worldBound = MODEL_BOUND.transform(worldTransform, worldBound);
                }
            }
        }

        public BoundingVolume getLocalBound() {
            return MODEL_BOUND;
        }

        public MDISystem getMDISystem() {
            return SYSTEM;
        }

        public int getInstanceID() {
            return ID;
        }

    }

    private static ByteBuffer ensureCapacity(ByteBuffer old, int bytes) {
        if (old == null) {
            return BufferUtils.createByteBuffer(bytes);
        } else if (old.capacity() < bytes) {
            BufferUtils.destroyDirectBuffer(old);
            return BufferUtils.createByteBuffer(bytes);
        }
        old.clear();
        return old;
    }

    private static class MDIGeometryInstanceComparator implements Comparator<MDIGeometry> {

        @Override
        public int compare(MDIGeometry t, MDIGeometry t1) {
            return (t != null ? t.ID : Integer.MAX_VALUE) - (t1 != null ? t1.ID : Integer.MAX_VALUE);
        }

    }

    private static class MDIGeometryDistanceComparator implements Comparator<MDIGeometry> {

        protected Vector3f compareDistance;

        private float getDistance(MDIGeometry geo) {
            if (geo == null) {
                return Float.MAX_VALUE;
            } else if (geo.sortDistance != Float.POSITIVE_INFINITY) {
                return geo.sortDistance;
            }
            float dist = compareDistance.distanceSquared(geo.getWorldTranslation());
            geo.sortDistance = dist;
            return dist;
        }

        @Override
        public int compare(MDIGeometry t, MDIGeometry t1) {
            float d = getDistance(t);
            float d1 = getDistance(t1);
            if (d < d1) {
                return 1;
            } else if (d1 < d) {
                return -1;
            } else {
                return 0;
            }
        }

    }

}
