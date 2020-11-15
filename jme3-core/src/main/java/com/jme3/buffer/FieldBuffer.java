/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.shader.layout.BlockLayout;
import com.jme3.shader.layout.Struct;
import com.jme3.shader.layout.BlockVarType;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit
 */
public abstract class FieldBuffer extends TypedBuffer {

    public static interface FieldBufferWriter {

        public static final FieldBufferWriter NULL_WRITER = new FieldBufferWriter() {
            @Override
            public void writeField(ByteBuffer buffer, BlockField field) {

            }

            @Override
            public int getBufferSize() {
                return 0;
            }
        };

        public void writeField(ByteBuffer buffer, BlockField field);

        public int getBufferSize();
    }

    public static class BlockField {

        private final String NAME;
        private final BlockVarType TYPE;
        private Object value = null;

        public BlockField(String name, BlockVarType type) {
            NAME = name;
            TYPE = type;
        }

        public String getName() {
            return NAME;
        }

        public BlockVarType getBlockVarType() {
            return TYPE;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
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
    public void declareField(String name, BlockVarType type) {
        if (name == null || type == null) {
            throw new IllegalArgumentException("none of the arguments can be null");
        }
        if (fieldsMap.containsKey(name)) {
            throw new IllegalArgumentException("field of name " + name + " was already declared");
        }
        BlockField field = new BlockField(name, type);
        fields.add(field);
        fieldsMap.put(name, field);
        fieldsChanged = true;
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
            BlockVarType type = BlockVarType.fromClass(value.getClass());
            if (value.getClass().isArray() && Struct.class.isAssignableFrom(value.getClass().getComponentType())) {
                type = BlockVarType.StructArray;
            } else if ((value instanceof Collection<?>) && !((Collection<?>) value).isEmpty()
                    && Struct.class.isAssignableFrom(((Collection<?>) value).iterator().next().getClass())) {
                type = BlockVarType.StructArray;
            } else if (Struct.class.isAssignableFrom(value.getClass())) {
                type = BlockVarType.Struct;
            }
            if (type == null) {
                throw new IllegalArgumentException("cannot automatically detect type of " + value);
            }
            declareField(name, type);
            field = fieldsMap.get(name);
        }

        field.setValue(value);
        fieldsChanged = true;
    }

    /**
     * Will mark the fields of this buffer as changed, ie the data will get
     * resent upon next usage.
     */
    public void markFieldsChanged() {
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
        if (WRITER != null) {
            //TODO improve memorywise
            if (WRITER.getBufferSize() > 0) {
                ByteBuffer buf = BufferUtils.createByteBuffer(WRITER.getBufferSize());
                for (BlockField field : fields.getArray()) {
                    WRITER.writeField(buf, field);
                }
                buf.position(0).limit(WRITER.getBufferSize());
                BUFFER.updateData(buf, 0);
            }
        } else {
            if (layout == null) {
                throw new UnsupportedOperationException("Cannot flush field updates when the layout is unknown");
            }
            flushFieldUpdatesSpecific();
        }
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
}
