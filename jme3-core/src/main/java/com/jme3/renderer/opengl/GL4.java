/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * GL functions only available on vanilla desktop OpenGL 4.0.
 * 
 * @author Kirill Vainer
 */
public interface GL4 extends GL3 {
    
    public static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 36202;
    public static final int GL_QUERY_RESULT_NO_WAIT = 37268;
    
    //additional byffer typed
    public static final int GL_QUERY_BUFFER = 37266; 
    public static final int GL_DRAW_INDIRECT_BUFFER = 36671;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 37102;
    
    //storage / mapping flags
    public static final int GL_MAP_READ_BIT = 1;
    public static final int GL_MAP_WRITE_BIT = 2;
    public static final int GL_MAP_PERSISTENT_BIT = 64;
    public static final int GL_MAP_COHERENT_BIT = 128;
    public static final int GL_DYNAMIC_STORAGE_BIT = 256;
    
    //shader information
    public static final int GL_BUFFER_VARIABLE = 37605;
    
    public static final int GL_ACTIVE_RESOURCES = 37621;
    public static final int GL_ACTIVE_VARIABLES = 37637;
    public static final int GL_NUM_ACTIVE_VARIABLES = 37636;
    public static final int GL_BUFFER_DATA_SIZE = 37635; 
    public static final int GL_OFFSET = 37628;
    public static final int GL_TYPE = 37626;
    public static final int GL_BLOCK_INDEX = 37629;
    public static final int GL_ARRAY_STRIDE = 37630;
    public static final int GL_TOP_LEVEL_ARRAY_SIZE = 37644;
    public static final int GL_TOP_LEVEL_ARRAY_STRIDE = 37645;
    public static final int GL_ARRAY_SIZE = 37627;
    public static final int GL_MATRIX_STRIDE = 37631;
    public static final int GL_IS_ROW_MAJOR = 37632;
    
    //memory barrier bits
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 1;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 2;
    public static final int GL_UNIFORM_BARRIER_BIT = 4;
    public static final int GL_TEXTURE_FETCH_BARRIER_BIT = 8;
    public static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 32;
    public static final int GL_COMMAND_BARRIER_BIT = 64;
    public static final int GL_PIXEL_BUFFER_BARRIER_BIT = 128;
    public static final int GL_TEXTURE_UPDATE_BARRIER_BIT = 256;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 512;
    public static final int GL_FRAMEBUFFER_BARRIER_BIT = 1024;
    public static final int GL_TRANSFORM_FEEDBACK_BARRIER_BIT = 2048;
    public static final int GL_ATOMIC_COUNTER_BARRIER_BIT = 4096;
    public static final int GL_ALL_BARRIER_BITS = -1;
    public static final int GL_QUERY_BUFFER_BARRIER_BIT = 32768;
    public static final int GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT = 16384;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 8192;
     
    //compute shader constants
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    public static final int GL_COMPUTE_WORK_GROUP_SIZE = 0x8267; 
    public static final int GL_MAX_COMPUTE_UNIFORM_BLOCKS = 37307;
    public static final int GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS = 37308;
    public static final int GL_MAX_COMPUTE_IMAGE_UNIFORMS = 37309;
    public static final int GL_MAX_COMPUTE_SHARED_MEMORY_SIZE = 33378;
    public static final int GL_MAX_COMPUTE_UNIFORM_COMPONENTS = 33379;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS = 33380;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTERS = 33381;
    public static final int GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS = 33382;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = 37099;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = 37310;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = 37311;
    
    public static final int GL_MAX_IMAGE_UNITS = 36664;
    
    public static final int GL_MAX_TESS_EVALUATION_TEXTURE_IMAGE_UNITS = 36482;
    public static final int GL_MAX_TESS_CONTROL_TEXTURE_IMAGE_UNITS = 36481;

    public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
    public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;
    public static final int GL_PATCHES = 0xE;

    /**
     * Accepted by the {@code target} parameter of BindBufferBase and BindBufferRange.
     */
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    public static final int GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = 37596;

    /**
     * Accepted by the {@code target} parameters of BindBuffer, BufferData, BufferSubData, MapBuffer, UnmapBuffer, GetBufferSubData, and GetBufferPointerv.
     */
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_SHADER_STORAGE_BLOCK = 0x92E6;

    /**
     *  Accepted by the &lt;pname&gt; parameter of GetIntegerv, GetBooleanv,
     *  GetInteger64v, GetFloatv, and GetDoublev:
     */
    public static final int GL_MAX_VERTEX_SHADER_STORAGE_BLOCKS = 0x90D6;
    public static final int GL_MAX_GEOMETRY_SHADER_STORAGE_BLOCKS = 0x90D7;
    public static final int GL_MAX_TESS_CONTROL_SHADER_STORAGE_BLOCKS = 0x90D8;
    public static final int GL_MAX_TESS_EVALUATION_SHADER_STORAGE_BLOCKS = 0x90D9;
    public static final int GL_MAX_FRAGMENT_SHADER_STORAGE_BLOCKS = 0x90DA;
    public static final int GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS = 0x90DB;
    public static final int GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS = 0x90DC;
    public static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 0x90DD;
    public static final int GL_MAX_SHADER_STORAGE_BLOCK_SIZE = 0x90DE;
    public static final int GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT = 0x90DF;

    /**
     * <p><a target="_blank" href="http://docs.gl/gl4/glPatchParameteri">Reference Page</a></p>
     * <p>
     * Specifies the integer value of the specified parameter for patch primitives.
     *
     * @param count the new value for the parameter given by {@code pname}
     */
    public void glPatchParameter(int count);

    /**
     * Returns the unsigned integer index assigned to a resource named name in the interface type programInterface of
     * program object program.
     *
     * @param program          the name of a program object whose resources to query.
     * @param programInterface a token identifying the interface within program containing the resource named name.
     * @param name             the name of the resource to query the index of.
     * @return the index of a named resource within a program.
     */
    public int glGetProgramResourceIndex(int program, int programInterface, String name);

    /**
     * Cchanges the active shader storage block with an assigned index of storageBlockIndex in program object program.
     * storageBlockIndex must be an active shader storage block index in program. storageBlockBinding must be less
     * than the value of {@code #GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS}. If successful, glShaderStorageBlockBinding specifies
     * that program will use the data store of the buffer object bound to the binding point storageBlockBinding to
     * read and write the values of the buffer variables in the shader storage block identified by storageBlockIndex.
     *
     * @param program             the name of a program object whose resources to query.
     * @param storageBlockIndex   The index storage block within the program.
     * @param storageBlockBinding The index storage block binding to associate with the specified storage block.
     */
    public void glShaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding);
    
    /**
     * Dispatches the currently bound ComputeShader with the specified number of work groups
     * @param x 
     * @param y
     * @param z 
     */
    public void glDispatchCompute(int x, int y, int z);
    
    /**
     * Dispatches the currently bound ComputeShsader with a DispatchCommand read
     * from the buffer currently bound to dispatchIndirect at the provided offset
     * 
     * @param offset offset into dispatch buffer
     */
    public void glDispatchComputeIndirect(long offset);
    
    /**
     * Binds an Image to an image unit
     * @param unit the unit to bind it to
     * @param texture the id of the texture to bind
     * @param level the mipmaps level to bind
     * @param layered true to bind all layers
     * @param layer if not layered, specify layer to bind (TextureArray, Texture3D, TextureCubeMap)
     * @param access the accesflags, writeonly, readonly or readwrite
     * @param format the format of the image that is to be bound
     */
    public void glBindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format);
    
    /**
     * Sets a memory barrier. -> https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glMemoryBarrier.xhtml
     * @param bits the bits specifying which barriers to set
     */
    public void glMemoryBarrier(int bits);
    
    /**
     * Returns requested properties of program resources
     * 
     * @param program the shader program to query information of
     * @param programInterface the interface to query
     * @param index the index to query
     * @param props the actual property/ies to query
     * @param length the amount of properties to query
     * @param params the buffer to store the results
     */
    public void glGetProgramResource(int program, int programInterface, int index, IntBuffer props, IntBuffer length, IntBuffer params);
    
    /**
     * Returns the name of a program resource as specified in the shader.
     * 
     * @param program the shader program to query information of
     * @param programInterface the interface to query
     * @param index the index of within the interface to query
     * @return the name of the queried resource
     */
    public String glGetProgramResourceName(int program, int programInterface, int index); 
    
    /**
     * Queries GL for properties of the specified interface in the specified shader
     * 
     * @param program the shader program to query information of
     * @param programInterface the interface to query
     * @param pName the property to query
     * @return the value of the queried property
     */
    public int glGetProgramInterface(int program, int programInterface, int pName);
    
    /**
     * Allocates the requested amount of bytes for the buffer currently bound to target with the specified flags
     * @param target the target the buffer is bound to
     * @param size the size to allocate in bytes
     * @param flags flags for the allocation
     */
    public void glBufferStorage(int target, long size, int flags);
    
    /**
     * Sets the initial data for the buffer currently bound to target with the specified flags
     * @param target the target the buffer is bound to
     * @param buffer the data to set
     * @param flags flags for the allocation
     */
    public void glBufferStorage(int target, ByteBuffer buffer, int flags);
    
    /**
     * Reads the specified amount of DrawArraysIndirectCommands from the buffer currently
     * bound to DRAW_INDIRECT_BUFFER. 
     * @param mode the mode of the mesh
     * @param offset the offset into the draw commands buffer
     * @param drawCount the number of draw commands to read
     * @param stride the stride of between the start of one command and the next. or 0 for tightly packed
     */
    public void glMultiDrawArraysIndirect(int mode, long offset, int drawCount, int stride);
    
    
    /**
     * Reads the specified amount of DrawElementsIndirectCommands from the buffer currently
     * bound to DRAW_INDIRECT_BUFFER. 
     * @param mode the mode of the mesh
     * @param type the type of the mesh
     * @param offset the offset into the draw commands buffer
     * @param drawCount the number of draw commands to read
     * @param stride the stride of between the start of one command and the next. or 0 for tightly packed
     */
    public void glMultiDrawElementsIndirect(int mode, int type, long offset, int drawCount, int stride);
    
    /**
     * VERY IMPORTANT: if no buffer is bound to GL_QUERY_BUFFER, this will crash the application. 
     * Stores the value of pname of the provided query at the provided offset
     * in the buffer currently bound to GL_QUERY_BUFFER.
     *  
     * @param id id of the query
     * @param pname what to query
     * @param offset offset into buffer
     */
    public void glGetQueryObjectuiv(int id, int pname, int offset);
    
    /**
     * VERY IMPORTANT: if no buffer is bound to GL_QUERY_BUFFER, this will crash the application. 
     * Stores the value of pname of the provided query at the provided offset
     * in the buffer currently bound to GL_QUERY_BUFFER, stores 64 bits of data.
     *  
     * @param id id of the query
     * @param pname what to query
     * @param offset offset into buffer
     */
    public void glGetQueryObjectui64v(int id, int pname, int offset);
}
