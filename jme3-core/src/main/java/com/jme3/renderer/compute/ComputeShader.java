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
package com.jme3.renderer.compute;

import com.jme3.asset.AssetManager;
import com.jme3.buffer.AtomicCounterBuffer;
import com.jme3.buffer.DispatchIndirectBuffer;
import com.jme3.buffer.ShaderStorageBuffer;
import com.jme3.buffer.TypedBuffer;
import com.jme3.buffer.UniformBuffer;
import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.BlockLayout;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.Access;
import com.jme3.util.ListMap;
import com.jme3.util.SafeArrayList;
import java.util.Map.Entry;

/**
 * Compute Shader is a shader used to do computations, because that's definitely
 * not what other shader stages do. :)
 *
 * @author Alexander Kasigkeit
 */
public class ComputeShader {

    /**
     * Creates a new ComputeShader from the provided string.
     *
     * @param renderer Renderer used for internal operations
     * @param source the source code of the compute shader
     * @param language the language of the source code
     * @return a new ComputeShader
     */
    public static ComputeShader createFromString(Renderer renderer, String source, String language) {
        return new ComputeShader(renderer, source, language);
    }

    /**
     * Creates a new ComputeShader from a file. Note that glsl430 will be
     * assumed as the language.
     *
     * @param renderer Renderer used for internal operations
     * @param assetManager Needed to load the file
     * @param path path to the file
     * @return a new ComputeShader
     */
    public static ComputeShader createFromFile(Renderer renderer, AssetManager assetManager, String path) {
        return new ComputeShader(renderer, assetManager, path, "GLSL430");
    }

    /**
     * Creates a new ComputeShader from a file.
     *
     * @param renderer Renderer used for internal operations
     * @param assetManager Needed to load the file
     * @param path path to the file
     * @param language language of the source code
     * @return a new ComputeShader
     */
    public static ComputeShader createFromFile(Renderer renderer, AssetManager assetManager, String path, String language) {
        return new ComputeShader(renderer, assetManager, path, language);
    }

    private final Renderer RENDERER;
    private final String SOURCE, LANGUAGE;
    private final String SOURCE_LOC;

    private Shader shader = null;

    private final ListMap<String, TexImgParam> TEXTURES = new ListMap<>();
    private final ListMap<String, Define> DEFINES = new ListMap<>();
    private final ListMap<String, MatParam> UNIFORMS = new ListMap<>();
    private final ListMap<String, MatParam> BUFFERS = new ListMap<>();

    private final SafeArrayList<CompiledShader> COMPILED_SHADERS = new SafeArrayList<>(CompiledShader.class);
    private final StringBuilder SB = new StringBuilder(128);

    private boolean definesChanged = true;

    private final int[] localWorkGroupSize = new int[]{-1, 0, 0}; //-1 marks we didnt look it up yet

    public ComputeShader(Renderer renderer, AssetManager assetManager, String path, String language) {
        this(renderer, (String) assetManager.loadAsset(path), language, path);
    }

    public ComputeShader(Renderer renderer, String source, String language) {
        this(renderer, source, language, "InApp");
    }

    protected ComputeShader(Renderer renderer, String source, String language, String sourceLoc) {
        if (renderer == null || source == null || language == null || sourceLoc == null) {
            throw new IllegalArgumentException("none of the arguments can be null");
        } else if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            throw new UnsupportedOperationException("Hardware doesn't support ComputeShaders");
        }
        RENDERER = renderer;
        SOURCE = source;
        LANGUAGE = language;
        SOURCE_LOC = sourceLoc;
    }

    /**
     * Sets a uniform in the compute shader. All VarTypes except of Texture
     * Types are allowed.<br>
     * They can be defined in the shader as:<br>
     * <code>uniform vec3 m_someUniform;</code> or<br>
     * <code>uniform vec3 m_anotherUniform = vec3(1.0, 0.0, 0.0);</code> (for
     * initial value in case you dont set one via java code)<br>
     * and their values would be set with
     * <code>setUniform("someUniform", VarType.Vector3, new Vector3f(1f, 1f, 1f));</code>
     * and
     * <code>setUniform("anotherUniform", VarType.Vector3, new Vector3f(1f, 1f, 1f));</code>
     * respectively
     *
     * @param name the name of the uniform without leading "m_". in the shader
     * code however the name need to start with "m_"
     * @param type varType of the value to set
     * @param value the actual value
     */
    public void setUniform(String name, VarType type, Object value) {
        if (type.isTextureType()) {
            throw new IllegalArgumentException("provided varType is a texture type. use setTexture() instead.");
        }
        MatParam param = UNIFORMS.get(name);
        if (param == null) {
            param = new MatParam(type, name, value);
            UNIFORMS.put(name, param);
        } else {
            param.setValue(value);
        }
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setVector2(String name, Vector2f value) {
        setUniform(name, VarType.Vector2, value);
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setVector4(String name, Vector4f value) {
        setUniform(name, VarType.Vector4, value);
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setVector3(String name, Vector3f value) {
        setUniform(name, VarType.Vector3, value);
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setBoolean(String name, Boolean value) {
        setUniform(name, VarType.Boolean, value);
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setFloat(String name, Float value) {
        setUniform(name, VarType.Float, value);
    }

    /**
     * Convenience version of -> setUniform(name, type, value).
     *
     * @param name name of the uniform to set
     * @param value value of the uniform to set
     */
    public void setInt(String name, Integer value) {
        setUniform(name, VarType.Int, value);
    }

    /**
     * Removes the specified uniform from the Shader. Useful if the uniform is
     * defined in the shader including an initial value.
     *
     * @param name name of the uniform to remove, without the leading "m_"
     */
    public void removeUniform(String name) {
        UNIFORMS.remove(name);
    }

    /**
     * Sets a define in the shader. If a define changed between two shader
     * invocations, this will cause the shader to be recompiled before the
     * second execution. Supported types are Int, Float, Boolean, Vector2,
     * Vector3 and Vector4 (Vector4 supports Vector4f, ColorRGBA and Quaternion
     * objects, as well as null to indicate the define is a String). However be
     * aware of the fact that a define causes the preprocessor to replace all
     * occurances of the define with the specified value. This will result in
     * several vector "creations" if you use a vector define several times.<br>
     * Imagine following scenario:<br>
     * Setting a define via java code: <br>
     * <code>setDefine("SIZE", VarType.Vector3, new Vector3f(1f, 2f, 4f));</code><br>
     * Using the define in the shader like:<br>
     * <code>float totalSizeX = SIZE.x * someVariable;<br>
     * float totalSizeY = SIZE.y * anotherVariable;</code><br>
     * which will result in code like:<br>
     * <code>float totalSizeX = vec3(1.0, 2.0, 4.0).x * someVariable;<br>
     * float totalSizeY = vec3(1.0, 2.0, 4.0).y * anotherVariable;</code><br>
     * so a better usage would be to use 3 defines, 1 for each component of the
     * vector.<br>
     * Also you can write a shader like:<br>
     * <code>float value = imageLoad(m_Input, x).COMPONENT;</code><br>
     * And specify a define with:<br>
     * computeShader.setDefine("COMPONENT", null, "r"); or<br>
     * computeShader.setDefine("COMPONENT", null, "g");<br>
     * to conveniently swizzle input channels
     *
     * @param name name of the define, usually all-uppercase
     * @param type type of the define, or null to indicate a String
     * @param value value of the define
     */
    public void setDefine(String name, VarType type, Object value) {
        if (type != null && type.isTextureType()) {
            throw new IllegalArgumentException("provided varType is a texture type. cannot use textures as define");
        }
        boolean changed = true;
        Define def = DEFINES.get(name);
        if (def == null) {
            def = new Define(type, name, value);
            DEFINES.put(name, def);
        } else {
            if (type != def.type) {
                throw new IllegalArgumentException("provided varType is different from previous define with same name");
            }
            if (value.equals(def.value)) {
                changed = false;
                //System.out.println("defines are the same, no need to update");
            } else {
                def.value = value;
            }
        }
        definesChanged |= changed;
    }

    /**
     * Removes the specified define. Causes the shader to recompile in case the
     * define was previously set.
     *
     * @param name name of the define to remove
     */
    public void removeDefine(String name) {
        if (DEFINES.remove(name) != null) {
            definesChanged = true;
        }
    }

    /**
     * see @setTexture(String name, VarType type, Object value, Access access,
     * int level, int layer, boolean setDefines)<br>
     * called method mentioned above with level = 0 (level meaning mipmap level
     * here) and layer = -1 (no specific layer, ie in case of TextureArray,
     * Texture3D or TextureCubeMap, all layers will be bound) and setDefines =
     * false
     *
     * @param name name of the texture
     * @param type type of the texture
     * @param value actual texture
     * @param access access pattern
     */
    public void setImage(String name, VarType type, Texture value, Access access) {
        setTextureOrImage(name, type, value, access, 0, -1, false);
    }

    /**
     * see @setTexture(String name, VarType type, Object value, Access access,
     * int level, int layer, boolean setDefines)<br>
     * called method mentioned above with setDefines = false
     *
     * @param name name of the texture
     * @param type type of the texture
     * @param value actual texture
     * @param access access pattern
     * @param level the mipmap level to bind
     * @param layer the layer to bind or -1 to bind all layers
     */
    public void setImage(String name, VarType type, Texture value, Access access, int level, int layer) {
        setTextureOrImage(name, type, value, access, level, layer, false);
    }

    /**
     * Binds the specified texture to this ComputeShader as an Image. The data
     * can be accessed in the shader via
     * <code>vec4 val = imageLoad(m_Name, ivecX(...));</code> or written to via
     * <code>imageStore(m_Name, ivecX(...), vec4(value));</code>.<br>
     * The <b>type</b> specifies the Type of the Texture, the access indicated
     * how the image will be used (only written to, only read from or both),<br>
     * the <b>level</b> specifies the mipmap level to bind (can only bind a
     * single mipmap level at a time).<br>
     * The <b>layer</b> specifies the layer of the texture to bind, in case of
     * TextureArray, TextureCubeMap or Texture3D a layer of -1 will bind all
     * layers, a value other than -1 will bind the specified layer.<br>
     * If <b>setDefines</b> is true, the defines NAME_WIDTH, NAME_HEIGHT,
     * NAME_FORMAT and in case width and height are equal (provided texture is a
     * square texture) also NAME_RES to use in the shader.<br>
     * Example: java code:<br>
     * <code>computeShader.setImage("Albedo", VarType.Texture2D, someTex, Access.ReadOnly, 0, -1, true);</code><br>
     * and on the compute shader:<br>
     * <code>layout (ALBEDO_FORMAT) uniform readonly image2D m_Albedo;</code>
     *
     *
     * @param name name of the image to set, without leading "m_"
     * @param type type of the texture
     * @param value the actual texture
     * @param access indicates how the shader will access this image
     * @param level the mipmap level of the texture
     * @param layer the layer of the texture or -1 to bind all
     * @param setDefines true to set defines for width, height and format, false
     * to skip
     */
    public void setImage(String name, VarType type, Texture value, Access access, int level, int layer, boolean setDefines) {
        setTextureOrImage(name, type, value, access, level, layer, false);
        if (setDefines) {
            String upperName = name.toUpperCase();
            int w = value.getImage().getWidth();
            int h = value.getImage().getHeight();
            int d = value.getImage().getDepth();
            setDefine(upperName + "_WIDTH", VarType.Int, w);
            setDefine(upperName + "_HEIGHT", VarType.Int, h);
            setDefine(upperName + "_DEPTH", VarType.Int, d);
            if (w == h) { //if its a square, also apply RES define
                setDefine(upperName + "_RES", VarType.Int, w);
            }
            setDefine(upperName + "_FORMAT", null, getGlslFormat(value.getImage().getFormat()));
        }
    }

    private String getGlslFormat(Format format) {
        //for now seems like all constants are named in a way converting to lower case yields the correct glsl value
        return format.name().toLowerCase();
    }

    /**
     * Sets the provided texture to be accessible via
     * <code>texture(m_Texture, vecX(...);</code>-calls
     *
     * @param name the name of the texture without leading "m_"
     * @param type the texture of the texture
     * @param value the actual texture
     */
    public void setTexture(String name, VarType type, Texture value) {
        setTextureOrImage(name, type, value, Access.ReadOnly, 0, -1, true);
    }

    private void setTextureOrImage(String name, VarType type, Texture value, Access access, int level, int layer, boolean isTexture) {
        if (!type.isTextureType()) {
            throw new IllegalArgumentException("provided vartype is no texture type: " + type);
        }
        TexImgParam p = TEXTURES.get(name);
        if (p == null) {
            p = new TexImgParam(type, name, value, access, level, layer, isTexture);
            TEXTURES.put(name, p);
        } else {
            p.update(access, level, layer, isTexture, value);
        }
    }

    /**
     * Removes a texture from the shader
     *
     * @param name name of texture to remove
     */
    public void removeTexture(String name) {
        TEXTURES.remove(name);
    }

    /**
     * Removes an image from the shader
     *
     * @param name name of image to remove
     */
    public void removeImage(String name) {
        TEXTURES.remove(name);
    }

    public void setShaderStorageBuffer(String name, ShaderStorageBuffer buffer) {
        setBuffer(name, buffer);
    }

    public void setUniformBuffer(String name, UniformBuffer buffer) {
        setBuffer(name, buffer);
    }

    public void setAtomicCouterBuffer(String name, AtomicCounterBuffer buffer) {
        setBuffer(name, buffer);
    }

    private void setBuffer(String name, TypedBuffer buffer) {
        MatParam p = BUFFERS.get(name);
        if (p == null) {
            p = new MatParam(VarType.BufferObject, name, buffer);
            BUFFERS.put(name, p);
        } else {
            p.setValue(buffer);
        }
    }

    /**
     * Queries the underlying system for the layout of the buffers declared in
     * the shader. Note that those might change if you change the defines and
     * your code depends on that.
     */
    public void queryLayouts() {
        createIfNeeded();
        RENDERER.queryBlockLayouts(this);
    }

    /**
     * Returns the ShaderStorageBuffer LayoutBlock related to the specified
     * name.
     *
     * @param name name of the Block to query Layout of
     * @return blocklayout
     */
    public BlockLayout getShaderStorageBufferLayout(String name) {
        return shader.getShaderStorageBlockLayout("m_" + name);
    }

    /**
     * Returns the UniformBuffer LayoutBlock related to the specified name.
     *
     * @param name name of the Block to query Layout of
     * @return blocklayout
     */
    public BlockLayout getUniformBufferLayout(String name) {
        return shader.getUniformBlockLayout("m_" + name);
    }

    /**
     * Returns the number of variations that exist on the GPU that were compiled
     * from this shader
     *
     * @return number of different variations of this shader on the GPU
     */
    public int getVariationCount() {
        return COMPILED_SHADERS.size();
    }

    /**
     * Runs this ComputeShader with the work group counts calculated by ceiling
     * the result of calculating total over local sizes.That guarantees there is
     * always enough invocations to cover the specified total problem size,
     * although some invocations might be "out of bounds" if the total size %
     * local size is not 0
     *
     * @param totalX total number of invotations in x dimension
     * @param totalY total number of invocations in y dimension
     * @param localX local size of the compute shader in x dimension
     * @param localY local size of the compute shader in y dimension
     * @param memBarrier memory barriers to set
     */
    public void run(int totalX, int totalY, int localX, int localY, MemoryBarrier memBarrier) {
        int x = (int) Math.ceil(totalX / (double) localX);
        int y = (int) Math.ceil(totalY / (double) localY);
        run(x, y, 1, memBarrier);
    }

    /**
     * Runs this ComputeShader with the work group counts calculated by ceiling
     * the result of calculating total over local sizes. That guarantees there
     * is always enough invocations to cover the specified total problem size,
     * although some invocations might be "out of bounds" if the total size %
     * local size is not 0
     *
     * @param totalX total number of invotations in x dimension
     * @param localX local size of the compute shader in x dimension
     * @param memBarrier memory barriers to set
     */
    public void run(int totalX, int localX, MemoryBarrier memBarrier) {
        int x = (int) Math.ceil(totalX / (double) localX);
        run(x, 1, 1, memBarrier);
    }

    /**
     * Runs this ComputeShader with the work group counts calculated by ceiling
     * the result of calculating total over local sizes.That guarantees there is
     * always enough invocations to cover the specified total problem size,
     * although some invocations might be "out of bounds" if the total size %
     * local size is not 0
     *
     * @param totalX total number of invotations in x dimension
     * @param totalY total number of invocations in y dimension
     * @param totalZ total number of invocations in z dimension
     * @param localX local size of the compute shader in x dimension
     * @param localY local size of the compute shader in y dimension
     * @param localZ local size of the compute shader in z dimension
     * @param memBarrier memory barriers to set
     */
    public void run(int totalX, int totalY, int totalZ, int localX, int localY, int localZ, MemoryBarrier memBarrier) {
        int x = (int) Math.ceil(totalX / (double) localX);
        int y = (int) Math.ceil(totalY / (double) localY);
        int z = (int) Math.ceil(totalZ / (double) localZ);
        run(x, y, z, memBarrier);
    }

    /**
     * Dispatches this ComputeShader using the specified DispatchCommand
     *
     * @param command the DispatchCommand containing the work group counts
     * @param memBarrier the memory barrier to set
     */
    public void run(DispatchCommand command, MemoryBarrier memBarrier) {
        run(command.getNumGroupsX(), command.getNumGroupsY(), command.getNumGroupsZ(), memBarrier);
    }

    public void run(DispatchIndirectBuffer buffer, int offset, MemoryBarrier memBarrier) {
        createIfNeeded();
        RENDERER.runComputeShader(this, buffer, offset);
        RENDERER.placeMemoryBarrier(memBarrier);
    }

    /**
     * Runs this ComputeShader with the specified number of work groups.None of
     * the values can be smaller than 1. Take into account this is the number of
     * work groups, not the number of single invocations. If the ComputeShader
     * specifies a local size of x = 16 and y = 16, then running a total of
     * 256x256 invocations requires a call to this method with values run(256 /
     * 16, 256 / 16, 1);<br>
     * Sets the specified memory barrier bits after running the shader
     *
     * @param x number of <b>work groups</b> in x dimension
     * @param y number of <b>work groups</b> in y dimension
     * @param z number of <b>work groups</b> in z dimension
     * @param memBarrier barrier bits to set
     */
    public void run(int x, int y, int z, MemoryBarrier memBarrier) {
        createIfNeeded();
        RENDERER.runComputeShader(this, x, y, z);
        RENDERER.placeMemoryBarrier(memBarrier);
    }

    private void createIfNeeded() {
        if (shader == null || definesChanged) {//first time or defines changed, update shader  
            int hash = 31 * DEFINES.hashCode();
            Shader newShader = null;
            String def = null;
            for (CompiledShader cs : COMPILED_SHADERS.getArray()) {
                if (cs.hash != hash) {
                    continue; //if hashes dont equal, skip right away
                }
                if (def == null) {
                    def = createDefines();
                }
                if (!def.equals(cs.definitions)) {
                    continue;
                }
                newShader = cs.shader;
                break;
            }

            if (newShader == null) {
                if (def == null) {
                    def = createDefines();
                }
                String name = SOURCE_LOC == null ? "InApp" : SOURCE_LOC;

                String source = SOURCE;
                newShader = new Shader();
                newShader.addSource(Shader.ShaderType.Compute, name, source, def, LANGUAGE);
                CompiledShader cs = new CompiledShader();
                cs.hash = hash;
                cs.definitions = def;
                cs.shader = newShader;
                COMPILED_SHADERS.add(cs);
            }
            definesChanged = false;
            shader = newShader;
        }
    }

    /**
     * returns the local work group size of this compute shader. The value will
     * be cached, however the first call to this function will query OpenGL for
     * the values which requires the shader to already exist. That means, when
     * calling this function, the shader with its current defines will be
     * compiled in order to be able to query for the values. So if you use
     * defines, you should set the defines before the first call to this
     * function so the shader does not have to be compiled needlessly
     *
     * @param store array to store the values in (at least 3 ints long)
     * @return the array provided or a new one if none was provided
     */
    public int[] getLocalWorkGroupSize(int[] store) {
        if (store == null || store.length < 3) {
            store = new int[3];
        }
        if (localWorkGroupSize[0] == -1) {
            createIfNeeded();
            if (shader.getId() < 0) {
                RENDERER.setShader(getShader());
            }
            RENDERER.getLocalWorkGroupSize(this, localWorkGroupSize);
        }
        store[0] = localWorkGroupSize[0];
        store[1] = localWorkGroupSize[1];
        store[2] = localWorkGroupSize[2];
        return store;
    }

    /**
     * USED INTERNALLY. Returns a ListMap containing all Textures and Images
     * currently attached to this ComputeShader
     *
     * @return ListMap of Textures and Images
     */
    public ListMap<String, TexImgParam> getTextures() {
        return TEXTURES;
    }

    /**
     * USED INTERNALLY. Returns a ListMap containing all Buffers currently
     * attached to this ComputeShader
     *
     * @return ListMap of Buffers
     */
    public ListMap<String, MatParam> getBuffers() {
        return BUFFERS;
    }

    /**
     * USED INTERNALLY. Returns a ListMap containing all Uniforms currently
     * attached to this ComputeShader
     *
     * @return ListMap of Uniforms
     */
    public ListMap<String, MatParam> getUniforms() {
        return UNIFORMS;
    }

    /**
     * USED INTERNALLY. Returns the underlying shader instance
     *
     * @return
     */
    public Shader getShader() {
        return shader;
    }

    /**
     * Returns the name of this ComputeShader (ie the string that was used to
     * load it)
     *
     * @return the name of this ComputeShader
     */
    public String getName() {
        return SOURCE_LOC;
    }

    private String createDefines() {
        SB.setLength(0);
        for (Entry<String, Define> def : DEFINES.entrySet()) {
            SB.append("#define ").append(def.getValue().name).append(" ");
            if (def.getValue().type == null) {
                SB.append(def.getValue().value);
            } else {
                toString(SB, def.getValue().type, def.getValue().value);
            }
            SB.append("\n");
        }
        return SB.toString();
    }

    private StringBuilder toString(StringBuilder sb, VarType type, Object value) {
        switch (type) {
            case Float:
                return sb.append((Float) value);
            case Vector2:
                Vector2f v2 = (Vector2f) value;
                return sb.append("vec2(").append(v2.x).append(", ").append(v2.y).append(")");
            case Vector3:
                Vector3f v3 = (Vector3f) value;
                return sb.append("vec3(").append(v3.x).append(", ").append(v3.y).append(", ").append(v3.z).append(")");
            case Vector4:
                if (value instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) value;
                    return sb.append("vec4(").append(c.r).append(", ").append(c.g).append(", ").append(c.b).append(", ").append(c.a).append(")");
                } else if (value instanceof Vector4f) {
                    Vector4f c = (Vector4f) value;
                    return sb.append("vec4(").append(c.x).append(", ").append(c.y).append(", ").append(c.z).append(", ").append(c.w).append(")");
                } else {
                    Quaternion c = (Quaternion) value;
                    return sb.append("vec4(").append(c.getX()).append(", ").append(c.getY()).append(", ").append(c.getZ()).append(", ").append(c.getW()).append(")");
                }
            case Boolean:
                Boolean b = (Boolean) value;
                return sb.append(b.booleanValue() ? "1" : "0");
            case Int:
                Integer i = (Integer) value;
                return sb.append(Integer.valueOf(i));
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + type);
        }
    }

    private static class CompiledShader {

        private Shader shader = null;
        private String definitions = null;
        private int hash = -1;

    }

    protected static class Define {

        protected Define(VarType type, String name, Object value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        private String name;
        private VarType type;
        private Object value;

    }

}
