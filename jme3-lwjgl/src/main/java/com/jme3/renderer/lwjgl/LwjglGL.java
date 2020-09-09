package com.jme3.renderer.lwjgl;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.GL;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.util.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.*;

public final class LwjglGL implements GL, GL2, GL3, GL4 {
 
    IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);
    
    private static void checkLimit(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }
    
    @Override
    public void resetStats() {
    }
    
    @Override
    public void glActiveTexture(int param1) {
        GL13.glActiveTexture(param1);
    }

    @Override
    public void glAlphaFunc(int param1, float param2) {
        GL11.glAlphaFunc(param1, param2);
    }

    @Override
    public void glAttachShader(int param1, int param2) {
        GL20.glAttachShader(param1, param2);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        GL15.glBeginQuery(target, query);
    }

    @Override
    public void glBindBuffer(int param1, int param2) {
        GL15.glBindBuffer(param1, param2);
    }

    @Override
    public void glBindTexture(int param1, int param2) {
        GL11.glBindTexture(param1, param2);
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode){
        GL20.glBlendEquationSeparate(colorMode,alphaMode);
    }

    @Override
    public void glBlendFunc(int param1, int param2) {
        GL11.glBlendFunc(param1, param2);
    }

    @Override
    public void glBlendFuncSeparate(int param1, int param2, int param3, int param4) {
        GL14.glBlendFuncSeparate(param1, param2, param3, param4);
    }

    @Override
    public void glBufferData(int param1, long param2, int param3) {
        GL15.glBufferData(param1, param2, param3);
    }
    
    @Override
    public void glBufferData(int param1, FloatBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    @Override
    public void glBufferData(int param1, ShortBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    @Override
    public void glBufferData(int param1, ByteBuffer param2, int param3) {
        checkLimit(param2);
        GL15.glBufferData(param1, param2, param3);
    }

    @Override
    public void glBufferSubData(int param1, long param2, FloatBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    @Override
    public void glBufferSubData(int param1, long param2, ShortBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    @Override
    public void glBufferSubData(int param1, long param2, ByteBuffer param3) {
        checkLimit(param3);
        GL15.glBufferSubData(param1, param2, param3);
    }

    @Override
    public void glClear(int param1) {
        GL11.glClear(param1);
    }

    @Override
    public void glClearColor(float param1, float param2, float param3, float param4) {
        GL11.glClearColor(param1, param2, param3, param4);
    }

    @Override
    public void glColorMask(boolean param1, boolean param2, boolean param3, boolean param4) {
        GL11.glColorMask(param1, param2, param3, param4);
    }

    @Override
    public void glCompileShader(int param1) {
        GL20.glCompileShader(param1);
    }

    @Override
    public void glCompressedTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GL13.glCompressedTexImage2D(param1, param2, param3, param4, param5, param6, param7);
    }

    @Override
    public void glCompressedTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GL13.glCompressedTexImage3D(param1, param2, param3, param4, param5, param6, param7, param8);
    }

    @Override
    public void glCompressedTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, ByteBuffer param8) {
        checkLimit(param8);
        GL13.glCompressedTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, param8);
    }

    @Override
    public void glCompressedTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GL13.glCompressedTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public int glCreateShader(int param1) {
        return GL20.glCreateShader(param1);
    }

    @Override
    public void glCullFace(int param1) {
        GL11.glCullFace(param1);
    }

    @Override
    public void glDeleteBuffers(IntBuffer param1) {
        checkLimit(param1);
        GL15.glDeleteBuffers(param1);
    }

    @Override
    public void glDeleteProgram(int param1) {
        GL20.glDeleteProgram(param1);
    }

    @Override
    public void glDeleteShader(int param1) {
        GL20.glDeleteShader(param1);
    }

    @Override
    public void glDeleteTextures(IntBuffer param1) {
        checkLimit(param1);
        GL11.glDeleteTextures(param1);
    }

    @Override
    public void glDepthFunc(int param1) {
        GL11.glDepthFunc(param1);
    }

    @Override
    public void glDepthMask(boolean param1) {
        GL11.glDepthMask(param1);
    }

    @Override
    public void glDepthRange(double param1, double param2) {
        GL11.glDepthRange(param1, param2);
    }

    @Override
    public void glDetachShader(int param1, int param2) {
        GL20.glDetachShader(param1, param2);
    }

    @Override
    public void glDisable(int param1) {
        GL11.glDisable(param1);
    }

    @Override
    public void glDisableVertexAttribArray(int param1) {
        GL20.glDisableVertexAttribArray(param1);
    }

    @Override
    public void glDrawArrays(int param1, int param2, int param3) {
        GL11.glDrawArrays(param1, param2, param3);
    }

    @Override
    public void glDrawBuffer(int param1) {
        GL11.glDrawBuffer(param1);
    }
    
    @Override
    public void glDrawRangeElements(int param1, int param2, int param3, int param4, int param5, long param6) {
        GL12.glDrawRangeElements(param1, param2, param3, param4, param5, param6);
    }

    @Override
    public void glEnable(int param1) {
        GL11.glEnable(param1);
    }

    @Override
    public void glEnableVertexAttribArray(int param1) {
        GL20.glEnableVertexAttribArray(param1);
    }

    @Override
    public void glEndQuery(int target) {
        GL15.glEndQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer param1) {
        checkLimit(param1);
        GL15.glGenBuffers(param1);
    }

    @Override
    public void glGenQueries(int num, IntBuffer ids) {
        GL15.glGenQueries(ids);
    }

    @Override
    public void glGenTextures(IntBuffer param1) {
        checkLimit(param1);
        GL11.glGenTextures(param1);
    }

    @Override
    public void glGetBoolean(int param1, ByteBuffer param2) {
        checkLimit(param2);
        GL11.glGetBoolean(param1, param2);
    }
    
    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        checkLimit(data);
        GL15.glGetBufferSubData(target, offset, data);
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }
    
    @Override
    public void glGetInteger(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL11.glGetInteger(param1, param2);
    }

    @Override
    public void glGetProgram(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glGetProgram(param1, param2, param3);
    }

    @Override
    public void glGetShader(int param1, int param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glGetShader(param1, param2, param3);
    }

    @Override
    public String glGetString(int param1) {
        return GL11.glGetString(param1);
    }
    
    @Override
    public String glGetString(int param1, int param2) {
        return GL30.glGetStringi(param1, param2);
    }

    @Override
    public boolean glIsEnabled(int param1) {
        return GL11.glIsEnabled(param1);
    }

    @Override
    public void glLineWidth(float param1) {
        GL11.glLineWidth(param1);
    }

    @Override
    public void glLinkProgram(int param1) {
        GL20.glLinkProgram(param1);
    }

    @Override
    public void glPixelStorei(int param1, int param2) {
        GL11.glPixelStorei(param1, param2);
    }

    @Override
    public void glPointSize(float param1) {
        GL11.glPointSize(param1);
    }

    @Override
    public void glPolygonMode(int param1, int param2) {
        GL11.glPolygonMode(param1, param2);
    }

    @Override
    public void glPolygonOffset(float param1, float param2) {
        GL11.glPolygonOffset(param1, param2);
    }

    @Override
    public void glReadBuffer(int param1) {
        GL11.glReadBuffer(param1);
    }

    @Override
    public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, ByteBuffer param7) {
        checkLimit(param7);
        GL11.glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }
    
    @Override
    public void glReadPixels(int param1, int param2, int param3, int param4, int param5, int param6, long param7) {
        GL11.glReadPixels(param1, param2, param3, param4, param5, param6, param7);
    }

    @Override
    public void glScissor(int param1, int param2, int param3, int param4) {
        GL11.glScissor(param1, param2, param3, param4);
    }

    @Override
    public void glStencilFuncSeparate(int param1, int param2, int param3, int param4) {
        GL20.glStencilFuncSeparate(param1, param2, param3, param4);
    }

    @Override
    public void glStencilOpSeparate(int param1, int param2, int param3, int param4) {
        GL20.glStencilOpSeparate(param1, param2, param3, param4);
    }

    @Override
    public void glTexImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GL11.glTexImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    @Override
    public void glTexImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, ByteBuffer param10) {
        checkLimit(param10);
        GL12.glTexImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10);
    }

    @Override
    public void glTexParameterf(int param1, int param2, float param3) {
        GL11.glTexParameterf(param1, param2, param3);
    }

    @Override
    public void glTexParameteri(int param1, int param2, int param3) {
        GL11.glTexParameteri(param1, param2, param3);
    }

    @Override
    public void glTexSubImage2D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, ByteBuffer param9) {
        checkLimit(param9);
        GL11.glTexSubImage2D(param1, param2, param3, param4, param5, param6, param7, param8, param9);
    }

    @Override
    public void glTexSubImage3D(int param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, int param10, ByteBuffer param11) {
        checkLimit(param11);
        GL12.glTexSubImage3D(param1, param2, param3, param4, param5, param6, param7, param8, param9, param10, param11);
    }

    @Override
    public void glUniform1(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform1(param1, param2);
    }

    @Override
    public void glUniform1(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform1(param1, param2);
    }

    @Override
    public void glUniform1f(int param1, float param2) {
        GL20.glUniform1f(param1, param2);
    }

    @Override
    public void glUniform1i(int param1, int param2) {
        GL20.glUniform1i(param1, param2);
    }

    @Override
    public void glUniform2(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform2(param1, param2);
    }

    @Override
    public void glUniform2(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform2(param1, param2);
    }

    @Override
    public void glUniform2f(int param1, float param2, float param3) {
        GL20.glUniform2f(param1, param2, param3);
    }

    @Override
    public void glUniform3(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform3(param1, param2);
    }

    @Override
    public void glUniform3(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform3(param1, param2);
    }

    @Override
    public void glUniform3f(int param1, float param2, float param3, float param4) {
        GL20.glUniform3f(param1, param2, param3, param4);
    }

    @Override
    public void glUniform4(int param1, FloatBuffer param2) {
        checkLimit(param2);
        GL20.glUniform4(param1, param2);
    }

    @Override
    public void glUniform4(int param1, IntBuffer param2) {
        checkLimit(param2);
        GL20.glUniform4(param1, param2);
    }

    @Override
    public void glUniform4f(int param1, float param2, float param3, float param4, float param5) {
        GL20.glUniform4f(param1, param2, param3, param4, param5);
    }

    @Override
    public void glUniformMatrix3(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GL20.glUniformMatrix3(param1, param2, param3);
    }

    @Override
    public void glUniformMatrix4(int param1, boolean param2, FloatBuffer param3) {
        checkLimit(param3);
        GL20.glUniformMatrix4(param1, param2, param3);
    }

    @Override
    public void glUseProgram(int param1) {
        GL20.glUseProgram(param1);
    }

    @Override
    public void glVertexAttribPointer(int param1, int param2, int param3, boolean param4, int param5, long param6) {
        GL20.glVertexAttribPointer(param1, param2, param3, param4, param5, param6);
    }

    @Override
    public void glViewport(int param1, int param2, int param3, int param4) {
        GL11.glViewport(param1, param2, param3, param4);
    }

    @Override
    public int glGetAttribLocation(int param1, String param2) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetAttribLocation(param1, param2 + "\0");
    }

    @Override
    public int glGetUniformLocation(int param1, String param2) {
        // NOTE: LWJGL requires null-terminated strings
        return GL20.glGetUniformLocation(param1, param2 + "\0");
    }

    @Override
    public void glShaderSource(int param1, String[] param2, IntBuffer param3) {
        checkLimit(param3);
        GL20.glShaderSource(param1, param2);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxSize) {
        return GL20.glGetProgramInfoLog(program, maxSize);
    }

    @Override
    public long glGetQueryObjectui64(int query, int target) {
        return ARBTimerQuery.glGetQueryObjectui64(query, target);
    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        return GL15.glGetQueryObjecti(query, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxSize) {
        return GL20.glGetShaderInfoLog(shader, maxSize);
    }

    @Override
    public void glBindFragDataLocation(int param1, int param2, String param3) {
        GL30.glBindFragDataLocation(param1, param2, param3);
    }

    @Override
    public void glBindVertexArray(int param1) {
        GL30.glBindVertexArray(param1);
    }

    @Override
    public void glGenVertexArrays(IntBuffer param1) {
        checkLimit(param1);
        GL30.glGenVertexArrays(param1);
    }

    @Override
    public void glPatchParameter(int count) {
        GL40.glPatchParameteri(GL40.GL_PATCH_VERTICES,count);
    }

    @Override
    public int glGetProgramResourceIndex(final int program, final int programInterface, final String name) {
        return GL43.glGetProgramResourceIndex(program, programInterface, name);
    }

    @Override
    public void glShaderStorageBlockBinding(final int program, final int storageBlockIndex, final int storageBlockBinding) {
        GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
    }

    @Override
    public void glDeleteVertexArrays(IntBuffer arrays) {
        checkLimit(arrays);
        ARBVertexArrayObject.glDeleteVertexArrays(arrays);
    }

    @Override
    public int glGetUniformBlockIndex(final int program, final String uniformBlockName) {
        return GL31.glGetUniformBlockIndex(program, uniformBlockName);
    }

    @Override
    public void glBindBufferBase(final int target, final int index, final int buffer) {
        GL30.glBindBufferBase(target, index, buffer);
    }

    @Override
    public void glUniformBlockBinding(final int program, final int uniformBlockIndex, final int uniformBlockBinding) {
        GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
    }

    @Override
    public void glDispatchCompute(int x, int y, int z) {
        GL43.glDispatchCompute(x, y, z);
    }

    @Override
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    }

    @Override
    public void glMemoryBarrier(int bits) {
        GL42.glMemoryBarrier(bits);
    }

    @Override
    public void glGetInteger(int pname, int index, IntBuffer params) {
        GL30.glGetInteger(pname, index, params);
    }

    @Override
    public boolean glUnmapBuffer(int target) {
        return GL15.glUnmapBuffer(target);
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer) {
        return GL30.glMapBufferRange(target, offset, length, access, oldBuffer);
    }

    @Override
    public void glFlushMappedBufferRange(int target, long offset, long length) {
        GL30.glFlushMappedBufferRange(target, offset, length);
    }

    @Override
    public void glGetProgramResource(int program, int programInterface, int index, IntBuffer props, IntBuffer length, IntBuffer params) {
        GL43.glGetProgramResource(program, programInterface, index, props, length, params);
    }

    @Override
    public String glGetProgramResourceName(int program, int programInterface, int index) {
        return GL43.glGetProgramResourceName(program, programInterface, index, 1024);
    }

    @Override
    public int glGetProgramInterface(int program, int programInterface, int pname) {
        return GL43.glGetProgramInterfacei(program, programInterface, pname);
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public String glGetActiveUniformName(int program, int uniformIndex) {
        return GL31.glGetActiveUniformName(program, uniformIndex, 1024);
    }

    @Override
    public String glGetActiveUniformBlockName(int program, int uniformBlockIndex) {
        return GL31.glGetActiveUniformBlockName(program, uniformBlockIndex, 1024);
    }

    @Override
    public void glGetActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {
        GL31.glGetActiveUniformBlock(program, uniformBlockIndex, pname, params);
    }

    @Override
    public int glGetActiveUniformsi(int program, int uniformIndex, int pname) {
        return GL31.glGetActiveUniformsi(program, uniformIndex, pname);
    }

    @Override
    public void glBufferStorage(int target, long size, int flags) {
        GL44.glBufferStorage(target, size, flags);
    }

    @Override
    public void glBufferStorage(int target, ByteBuffer data, int flags) {
        GL44.glBufferStorage(target, data, flags);
    }

    @Override
    public void glDeleteQuery(int i) {
        GL15.glDeleteQueries(i);
    }
    
    @Override
    public void glDispatchComputeIndirect(long offset) { 
        GL43.glDispatchComputeIndirect(offset);
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        return GL32.glFenceSync(condition, flags);
        
    }

    @Override
    public void glDeleteSync(Object sync) {
        GL32.glDeleteSync((GLSync)sync);
    }

    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        return GL32.glClientWaitSync((GLSync)sync, flags, timeout);
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, IntBuffer params) { 
        GL15.glGetQueryObjectu(id, pname, params);   
    }

    @Override
    public void glGetQueryObjectui64v(int id, int pname, LongBuffer params) {
        GL33.glGetQueryObjectu(id, pname, params);
    }

    @Override
    public void glMultiDrawArraysIndirect(int mode, long offset, int count, int stride) {
        GL43.glMultiDrawArraysIndirect(mode, offset, count, stride);
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long offset, int count, int stride) {
        GL43.glMultiDrawElementsIndirect(mode, type, offset, count, stride);
    }

    @Override
    public void glGetQueryObjectuiv(int id, int pname, int offset) {  
        throw new UnsupportedOperationException("cannot find a way to store query results in GL_QUERY_BUFFER");
    }

    @Override
    public void glGetQueryObjectui64v(int id, int pname, int offset) {  
        throw new UnsupportedOperationException("cannot find a way to store query results in GL_QUERY_BUFFER");
    }
}
