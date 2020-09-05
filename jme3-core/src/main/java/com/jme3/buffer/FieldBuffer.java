/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.VarType;
import com.jme3.shader.layout.BlockLayout;
import com.jme3.util.SafeArrayList;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit
 */
public abstract class FieldBuffer extends TypedBuffer {

    public static interface FieldBufferWriter {

        public void writeField(ByteBuffer buffer, BlockField field);
        
        public int getBufferSize();
    }

    public static class BlockField {

        private final String NAME;
        private final VarType VAR_TYPE;
        private Object value = null;

        public BlockField(String name, VarType type) {
            NAME = name;
            VAR_TYPE = type;
        }

        public String getName() {
            return NAME;
        }

        public VarType getVarType() {
            return VAR_TYPE;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    protected static final Map<Class<?>, VarType> CLASS_TO_VAR_TYPE = new HashMap<>();

    static {
        CLASS_TO_VAR_TYPE.put(float[].class, VarType.FloatArray);
        CLASS_TO_VAR_TYPE.put(int[].class, VarType.IntArray);
        CLASS_TO_VAR_TYPE.put(Float.class, VarType.Float);
        CLASS_TO_VAR_TYPE.put(Integer.class, VarType.Int);
        CLASS_TO_VAR_TYPE.put(Boolean.class, VarType.Boolean);
        CLASS_TO_VAR_TYPE.put(Vector2f.class, VarType.Vector2);
        CLASS_TO_VAR_TYPE.put(Vector3f.class, VarType.Vector3);
        CLASS_TO_VAR_TYPE.put(ColorRGBA.class, VarType.Vector4);
        CLASS_TO_VAR_TYPE.put(Quaternion.class, VarType.Vector4);
        CLASS_TO_VAR_TYPE.put(Vector4f.class, VarType.Vector4);

        CLASS_TO_VAR_TYPE.put(Vector2f[].class, VarType.Vector2Array);
        CLASS_TO_VAR_TYPE.put(Vector3f[].class, VarType.Vector3Array);
        CLASS_TO_VAR_TYPE.put(Vector4f[].class, VarType.Vector4Array);
        CLASS_TO_VAR_TYPE.put(ColorRGBA[].class, VarType.Vector4Array);
        CLASS_TO_VAR_TYPE.put(Quaternion[].class, VarType.Vector4Array);

        CLASS_TO_VAR_TYPE.put(Matrix3f.class, VarType.Matrix3);
        CLASS_TO_VAR_TYPE.put(Matrix4f.class, VarType.Matrix4);
        CLASS_TO_VAR_TYPE.put(Matrix3f[].class, VarType.Matrix3Array);
        CLASS_TO_VAR_TYPE.put(Matrix4f[].class, VarType.Matrix4Array);
    }

    protected final SafeArrayList<BlockField> fields = new SafeArrayList<>(BlockField.class);
    protected final Map<String, BlockField> fieldsMap = new HashMap<>();
    private final FieldBufferWriter WRITER;

    protected BlockLayout layout = null; 
    private boolean fieldsChanged = false;

    protected FieldBuffer(UntypedBuffer buffer, Type type, FieldBufferWriter writer) {
        super(buffer, type);
        WRITER = writer;
    }

    /**
     * Declares a Field in this Buffer.
     *
     * @param name the name of the field as specified in the shader code
     * @param type the type of the variable, must match its glsl counterpart
     */
    public void declareField(String name, VarType type) {
        checkType(type);
        if (fieldsMap.containsKey(name)) {
            throw new IllegalArgumentException("field of name " + name + " was already declared");
        }
        BlockField field = new BlockField(name, type);
        fields.add(field);
        fieldsMap.put(name, field);
    }

    /**
     * Sets the value of the specified field. If the field did not exist yet, it
     * will be created
     *
     * @param name the name of the field
     * @param value the value to set it to
     */
    public void setField(String name, Object value) {
        BlockField field = fieldsMap.get(name);
        if (field == null) {
            VarType type = getVarTypeByValue(value);
            declareField(name, type);
            field = fieldsMap.get(name);
        }

        field.setValue(value);
        fieldsChanged = true;
    }

    /**
     * flushes the changes that have been made to the fields to the underlying
     * buffer. Wil be called automatically when the buffer is needed but can be
     * called manually after a series of changes also
     */
    public void flushFieldUpdates() {
        if (!fieldsChanged) {
            return;
        }
        if (layout == null) {
            throw new UnsupportedOperationException("Cannot flush field updates when the layout is unknown");
        }
        flushFieldUpdatesSpecific();
        fieldsChanged = false;
    }

    protected abstract void flushFieldUpdatesSpecific();

    /**
     * Returns true if this buffer is in autolayout mode
     *
     * @return true if this buffer is in autolayout mode
     */
    public boolean isAutoLayout() {
        return WRITER == null;
    }

    /**
     * In case this buffer is in autolayout mode and has its layout queried
     * already, will returns this buffers layout, otherwise will return null
     *
     * @return this buffers layout if present
     */
    public BlockLayout getLayout() {
        return layout;
    }

    /**
     * USED INTERNALLY. called by the renderer after its layout has been queried
     * from GL
     *
     * @param layout a description of this buffers layout on the GPU
     */
    public void setLayout(BlockLayout layout) {
        this.layout = layout;
    }

    protected abstract VarType getVarTypeByValue(Object value);

    protected static void checkType(VarType type) {
        if (type == null) {
            return;
        }
        switch (type) {
            case Boolean:
            case Int:
            case IntArray:
            case Float:
            case FloatArray:
            case Vector2:
            case Vector2Array:
            case Vector3:
            case Vector3Array:
            case Vector4:
            case Vector4Array:
            case Matrix3:
            case Matrix3Array:
            case Matrix4:
            case Matrix4Array:
                return;
            default:
                throw new IllegalArgumentException("VarType " + type + " is not allowed in UniformBuffers / ShaderStorageBuffers");
        }
    }
}
