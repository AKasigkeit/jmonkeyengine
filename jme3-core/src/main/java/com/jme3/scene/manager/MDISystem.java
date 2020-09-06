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
import com.jme3.buffer.RingBuffer;
import com.jme3.buffer.UntypedBuffer;
import com.jme3.buffer.UntypedBuffer.MemoryMode;
import com.jme3.buffer.UntypedBuffer.StorageFlag;
import com.jme3.material.Material;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.nio.ByteBuffer;
import java.util.List;

/**
 *
 * @author Alexander Kasigkeit
 */
public class MDISystem {

    private final DrawIndirectMode MODE;
    private final Renderer RENDERER;
    private final Material MAT;
    private final int BLOCKS;
    private boolean init = false;

    private Mesh.Mode meshMode = Mesh.Mode.Triangles;
    private int patchModeSize = 3;
    private boolean provideWorldMatrix = false;

    private UntypedBuffer indexData = null;
    private UntypedBuffer vertexData = null;
    private UntypedBuffer instanceData = null;
    private RingBuffer drawIndirectData = null;

    private VertexBuffer indexBuffer = null;
    private VertexBuffer instIdBuffer = null;
    private VertexBuffer[] vertBuffers = null;
    private VertexBuffer[] instBuffers = null;
    private DrawIndirectBuffer drawBuffer = null;
    private int drawBufferCapacity = 0;

    private int currentInstanceCount = 0;
    private int[] gaps = new int[16];
    private int gapCounter = 0;
    private int lastIndexByte = 0;
    private int lastVertByte = 0;
    private int lastInstByte = 0;

    private Mesh mesh = null;

    public MDISystem(Material mat, Renderer renderer, DrawIndirectMode mode) {
        this(mat, renderer, mode, 2);
    }

    public MDISystem(Material mat, Renderer renderer, DrawIndirectMode mode, int bufferBlocks) {
        if (mat == null || mode == null || renderer == null) {
            throw new IllegalArgumentException("mat, renderer and mode cannot be null");
        }
        if (bufferBlocks < 1 || bufferBlocks > 3) {
            throw new IllegalArgumentException("bufferBlocks has to be in range 1, 3");
        }
        BLOCKS = bufferBlocks;
        MAT = mat;
        RENDERER = renderer;
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
     * Call this prior to initialize() to stream the WorldMatrix of each
     * instance to be available in the VertexShader via<br>
     * <code>in mat4 inWorldMatrix;</code><br>
     * and can be used to transform the Vertex into WorldSpace via<br>
     * <code>vec4 worldPos = inWorldMatrix * vec4(inPosition, 1.0);</code>
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

    public MDISystem initialize(
            VertexBuffer.Type[] vertexTypes, VertexBuffer.Format[] vertexFormats, int[] vertexCounts,
            VertexBuffer.Type[] instanceTypes, VertexBuffer.Format[] instanceFormats, int[] instanceCounts,
            VertexBuffer.Type drawIdBuffer,
            int avgIndices, int avgVertices, int initInstances) {
        if (init) {
            throw new UnsupportedOperationException("this MDISystem is already initialized");
        } else if (vertexTypes.length != vertexFormats.length || vertexFormats.length != vertexCounts.length || vertexCounts.length < 1) {
            throw new IllegalArgumentException("vertexTypes, vertexFormats and vertexCounts must be equal in length and greater than 0");
        } else if (instanceTypes.length != instanceFormats.length || instanceFormats.length != instanceCounts.length) {
            throw new IllegalArgumentException("instanceTypes, instanceFormats and instanceCounts must be equal in length");
        } else if (MODE == DrawIndirectMode.Draw && avgIndices > 0) {
            throw new IllegalArgumentException("This MDISystem is in DrawIndirectMode.Draw and cannot use indices");
        }

        int vertStrideBytes = calculateStride(vertexFormats, vertexCounts);
        int instStrideBytes = calculateStride(instanceFormats, instanceCounts);
        instStrideBytes += drawIdBuffer != null ? 4 : 0;

        if (MODE == DrawIndirectMode.DrawIndices) {
            indexData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, RENDERER, StorageFlag.Dynamic);
            indexData.initialize(avgIndices * 4);
        }
        vertexData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, RENDERER, StorageFlag.Dynamic);
        vertexData.initialize(vertStrideBytes * avgVertices * initInstances);
        instanceData = UntypedBuffer.createNewStorageDirect(MemoryMode.GpuOnly, RENDERER, StorageFlag.Dynamic);
        instanceData.initialize(instStrideBytes * initInstances);

        //setup vertex buffers
        vertBuffers = new VertexBuffer[vertexTypes.length];
        int vertOffsetBytes = 0;
        for (int i = 0; i < vertBuffers.length; i++) {
            vertBuffers[i] = vertexData.asVertexBuffer(vertexTypes[i], vertexFormats[i], vertexCounts[i], vertStrideBytes, vertOffsetBytes);
            vertOffsetBytes += vertexFormats[i].getComponentSize() * vertexCounts[i];
        }
        //setup instance buffers if present
        instBuffers = new VertexBuffer[instanceTypes.length];
        int instOffsetBytes = 0;
        for (int i = 0; i < instBuffers.length; i++) {
            instBuffers[i] = instanceData.asVertexBuffer(instanceTypes[i], instanceFormats[i], instanceCounts[i], instStrideBytes, instOffsetBytes);
            instBuffers[i].setInstanceSpan(1);
            instOffsetBytes += instanceFormats[i].getComponentSize() * instanceCounts[i];
        }
        if (drawIdBuffer != null) {
            instIdBuffer = instanceData.asVertexBuffer(drawIdBuffer, VertexBuffer.Format.UnsignedInt, 1, instStrideBytes, instOffsetBytes);
            instIdBuffer.setInstanceSpan(1);
        }
        //setup index buffer if present
        if (MODE == DrawIndirectMode.DrawIndices) {
            indexBuffer = indexData.asIndexBuffer(VertexBuffer.Format.UnsignedInt);
        }

        //setup draw indirect buffer
        int bytesPerDrawCommand = MODE == DrawIndirectMode.Draw ? 16 : 20; //4 or 5 ints dependant on the command type
        drawIndirectData = new RingBuffer(RENDERER, bytesPerDrawCommand * initInstances, BLOCKS);
        drawBuffer = drawIndirectData.getBuffer().asDrawIndirectBuffer(MODE);
        drawBufferCapacity = initInstances;

        //create VAO to hold it
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
        if (instIdBuffer != null) {
            mesh.setBuffer(instIdBuffer);
        }
        mesh.setDrawIndirectBuffer(drawBuffer);
        init = true;
        return this;
    }

    private int calculateStride(VertexBuffer.Format[] formats, int[] counts) {
        int stride = 0;
        for (int i = 0; i < vertBuffers.length; i++) {
            int bytes = formats[i].getComponentSize() * counts[i];
            stride += bytes;
        }
        return stride;
    }

    public MDIGeometry createInstance(ByteBuffer indexBuffer, ByteBuffer vertexData, ByteBuffer instanceData, BoundingVolume modelBounds) {
        if (currentInstanceCount == drawBufferCapacity) {
            //resize draw indirect buffer

        }

        currentInstanceCount++;
        return null;
    }

    public void deleteInstance(MDIGeometry geo) {

    }

    public void render(List<MDIGeometry> geos) {
        if (MODE == DrawIndirectMode.Draw) {
            renderDraw(geos);
        } else {
            renderDrawIndices(geos);
        }
    }

    private void renderDraw(List<MDIGeometry> geos) {

    }

    private void renderDrawIndices(List<MDIGeometry> geos) {

    }

    public static class MDIGeometry extends Geometry {

        private final MDISystem SYSTEM;
        private final int INDEX;
        private final DrawCommand DRAW_CMD;
        private final DrawIndicesCommand DRAW_INDICES_CMD;
        private final BoundingVolume MODEL_BOUND;

        private MDIGeometry(MDISystem system, int index, DrawCommand drawCmd, BoundingVolume bounds) {
            SYSTEM = system;
            INDEX = index;
            DRAW_CMD = drawCmd;
            DRAW_INDICES_CMD = null;
            MODEL_BOUND = bounds;
        }

        private MDIGeometry(MDISystem system, int index, DrawIndicesCommand drawIndicesCmd, BoundingVolume bounds) {
            SYSTEM = system;
            INDEX = index;
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
            super.updateWorldBound();
            if (mesh.getBound() != null) {
                if (ignoreTransform) {
                    worldBound = MODEL_BOUND.clone(worldBound);
                } else {
                    worldBound = MODEL_BOUND.transform(worldTransform, worldBound);
                }
            }
        }

    }

}
