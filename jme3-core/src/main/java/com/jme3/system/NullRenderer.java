/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import com.jme3.buffer.DispatchIndirectBuffer;
import com.jme3.buffer.QueryBuffer;
import com.jme3.buffer.TypedBuffer;
import com.jme3.buffer.UntypedBuffer; 
import com.jme3.conditional.GpuQuery;
import com.jme3.conditional.SyncObject;
import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.Statistics;
import com.jme3.renderer.compute.ComputeShader;
import com.jme3.renderer.compute.MemoryBarrier;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.BufferObject;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.EnumSet;

public class NullRenderer implements Renderer {

    private final EnumSet<Caps> caps = EnumSet.allOf(Caps.class);
    private final EnumMap<Limits, Integer> limits = new EnumMap<>(Limits.class);
    private final Statistics stats = new Statistics();

    @Override
    public void initialize() {
        for (Limits limit : Limits.values()) {
            limits.put(limit, Integer.MAX_VALUE);
        }
    }

    @Override
    public EnumMap<Limits, Integer> getLimits() {
        return limits;
    }

    @Override
    public EnumSet<Caps> getCaps() {
        return caps;
    }

    @Override
    public Statistics getStatistics() {
        return stats;
    }

    @Override
    public void invalidateState(){
    }

    @Override
    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
    }

    @Override
    public void setBackgroundColor(ColorRGBA color) {
    }

    @Override
    public void applyRenderState(RenderState state) {
    }

    @Override
    public void setDepthRange(float start, float end) {
    }

    @Override
    public void postFrame() {
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
    }

    @Override
    public void setViewPort(int x, int y, int width, int height) {
    }

    @Override
    public void setClipRect(int x, int y, int width, int height) {
    }

    @Override
    public void clearClipRect() {
    }

    public void setLighting(LightList lights) {
    }

    @Override
    public void setShader(Shader shader) {
    }

    @Override
    public void deleteShader(Shader shader) {
    }

    @Override
    public void deleteShaderSource(ShaderSource source) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    @Override
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
    }

    @Override
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyColor, boolean copyDepth) {
    }
    
    
    @Override
    public void setMainFrameBufferOverride(FrameBuffer fb) {
    }
    
    @Override
    public void setFrameBuffer(FrameBuffer fb) {
    }

    @Override
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
    }

    @Override
    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    @Override
    public void setTexture(int unit, Texture tex) {
    }

    @Override
    public int setTexture(Texture tex) {
        return 0;
    }

    @Override
    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
    }

    @Override
    public void updateBufferData(VertexBuffer vb) {
    }

    @Override
    public void updateBufferData(BufferObject bo) {
    }
    @Override
    public void deleteBuffer(VertexBuffer vb) {
    }

    @Override
    public void deleteBuffer(BufferObject bo) {

    }

    @Override
    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
    }

    @Override
    public void resetGLObjects() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void deleteImage(Image image) {
    }

    @Override
    public void setAlphaToCoverage(boolean value) {
    }

    @Override
    public void setMainFrameBufferSrgb(boolean srgb) {     
    }

    @Override
    public void setLinearizeSrgbImages(boolean linearize) {    
    }

    @Override
    public int[] generateProfilingTasks(int numTasks) {
        return new int[0];
    }

    @Override
    public void startProfiling(int id) {

    }

    @Override
    public void stopProfiling() {

    }

    @Override
    public long getProfilingTime(int taskId) {
        return 0;
    }

    @Override
    public boolean isTaskResultAvailable(int taskId) {
        return false;
    }

    @Override
    public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format) {        
    }

    @Override
    public void setDefaultAnisotropicFilter(int level) {
    }

    @Override
    public boolean getAlphaToCoverage() {
        return false;
    }

    @Override
    public int getDefaultAnisotropicFilter() {
        return 0;
    }

    @Override
    public void deleteMesh(Mesh mesh) {
    }

    @Override
    public void runComputeShader(ComputeShader shader, int x, int y, int z) { 
    }
    
    @Override
    public void runComputeShader(ComputeShader shader, DispatchIndirectBuffer buffer, int offset) { 
    }

    @Override
    public void getLocalWorkGroupSize(ComputeShader shader, int[] store) { 
        store[0] = 0;
        store[1] = 0;
        store[2] = 0;
    }

    @Override
    public void placeMemoryBarrier(MemoryBarrier barrierBits) { 
    }
    
    @Override
    public MemoryBarrier createMemoryBarrier(MemoryBarrier.Flag... flags) {
        return null;
    }

    @Override
    public int setImage(Texture tex, int layer, int level, Texture.Access access) { 
        return 0;
    }

    @Override
    public void updateBuffer(UntypedBuffer buffer) { 
    }

    @Override
    public void deleteBuffer(UntypedBuffer buffer) { 
    }

    @Override
    public UntypedBuffer.BufferMappingHandle mapBuffer(UntypedBuffer buffer, int offset, int length, UntypedBuffer.MappingFlag... flags) {
        return null;
    }

    @Override
    public void flushMappedBuffer(UntypedBuffer.BufferMappingHandle mappingHandle, int offset, int length) { 
    }

    @Override
    public void unmapBuffer(UntypedBuffer.BufferMappingHandle mappingHandle) { 
    }

    @Override
    public int setBuffer(String name, TypedBuffer buffer) {
        return 0;
    }

    @Override
    public void queryBlockLayouts(ComputeShader shader) {
    }

    @Override
    public void deleteQuery(GpuQuery query) { 
    }

    @Override
    public long getQueryResult(GpuQuery query) {
        return 0L;
    }

    @Override
    public boolean isQueryResultAvailable(GpuQuery query) {
        return true;
    }
    
    @Override
    public void startQuery(GpuQuery query) { 
    }
    
    @Override
    public void stopQuery(GpuQuery query) { 
    }

    @Override
    public void getQueryResult(QueryBuffer buffer, GpuQuery query, int offset, boolean bits64, boolean wait) { 
    }

    @Override
    public void getQueryResultAvailability(QueryBuffer buffer, GpuQuery query, int offset) { 
    }

    @Override
    public void placeSyncObject(SyncObject sync) { 
    }

    @Override
    public SyncObject.Signal checkSyncObject(SyncObject sync, long timeoutNanos) { 
        return null;
    }

    @Override
    public void recycleSyncObject(SyncObject sync) { 
    }

    @Override
    public void generateMipMaps(Texture tex) { 
    }
}
