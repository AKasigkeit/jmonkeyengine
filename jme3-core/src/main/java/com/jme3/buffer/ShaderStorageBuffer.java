/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.shader.layout.BlockFieldLayout;
import com.jme3.shader.layout.BlockLayout;
import com.jme3.shader.layout.BlockLayout.StructNode;
import com.jme3.shader.layout.BlockVarType;
import com.jme3.shader.layout.BufferWriterUtils;
import com.jme3.shader.layout.ReflectionLayoutGenerator;
import com.jme3.shader.layout.ReflectionLayoutGenerator.FieldInfo;
import com.jme3.shader.layout.Struct;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ShaderStorageBuffer extends FieldBuffer {

    /**
     * Creates a new ShaderStorageBuffer. It will automatically layout the data
     * accoring to the layout specified in the shader as long as the names and
     * varTypes match
     *
     * @return a new ShaderStorageBuffer in autolayout mode
     */
    public static ShaderStorageBuffer createNewAutolayout() {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticRead);
        buffer.initialize(BufferUtils.createByteBuffer(0)); //empty for now, will adjust automatically
        return buffer.asShaderStorageBuffer(null);
    }

    /**
     * Creates a new ShaderStorageBuffer. It will use the provided
     * FieldBufferWriter to write the fields into the buffer, GL is not queried
     * for the layout and it is up to the user to make sure it matches the
     * layout in the shader
     *
     * @param writer the writer to use
     * @return a new ShaderStorageBuffer
     */
    public static ShaderStorageBuffer createNewManualLayout(FieldBufferWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("for manual layouts, writer cannot be null");
        }
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticRead);
        buffer.initialize(BufferUtils.createByteBuffer(Math.max(0, writer.getBufferSize()))); //empty for now, will adjust automatically
        return buffer.asShaderStorageBuffer(writer);
    }

    private ReflectionLayoutGenerator layoutGenerator = null;
    private AutoLayoutWriter layoutWriter = null;

    protected ShaderStorageBuffer(UntypedBuffer buffer, FieldBufferWriter writer) {
        super(buffer, Type.ShaderStorageBuffer, writer);
    }

    /**
     * Registers a struct for automatic layout
     *
     * @param <T> class of the struct
     * @param clazz class object
     */
    public <T extends Struct> void registerStruct(Class<T> clazz) {
        if (layoutGenerator == null) {
            layoutGenerator = new ReflectionLayoutGenerator();
        }
        layoutGenerator.registerStruct(clazz);
    }

    @Override
    public void flushFieldUpdatesSpecific() {
        //layout.getTreeView().printDebug();

        //check which buffer to write to 
        boolean isCpuGpu = BUFFER.getMemoryMode() == UntypedBuffer.MemoryMode.CpuGpu;
        boolean isTempBuffer = false;
        ByteBuffer data;
        int requiredSize = layout.getSize();
        for (BlockField field : fields.getArray()) {
            if (field.getBlockVarType().isArray()) {
                StructNode node = layout.getTreeView().getChild(field.getName());
                if (node == null) {
                    throw new UnsupportedOperationException("field " + field.getName() + " is not declared in block " + layout.getName());
                }
                if (node.getArrayLength() != 0) {
                    continue;
                }
                int count = 1;
                if (field.getValue().getClass().isArray()) {
                    count = Array.getLength(field.getValue());
                } else if (field.getValue() instanceof Collection<?>) {
                    count = ((Collection<?>) field.getValue()).size();
                }
                int stride = node.getStride();
                int newRequiredSize = (requiredSize - stride) + (count * stride);
                //System.out.println("recalculated size from " + requiredSize + " to " + newRequiredSize);
                //System.out.println(" =>  (" + requiredSize + " - " + stride + ") + (" + count + " * " + stride + ")");
                //System.out.println(" =>  (" + (requiredSize - stride) + ") + (" + (count * stride) + ")");
                requiredSize = newRequiredSize;
            }
        }

        if (isCpuGpu) {
            if (requiredSize > BUFFER.getSizeOnCpu()) {
                data = ByteBuffer.allocate(requiredSize).order(ByteOrder.nativeOrder()); //will be destroyed further down again 
                isTempBuffer = true;
            } else {
                data = BUFFER.getCpuData();
                data.clear();
            }
        } else {
            data = getByteBuffer(requiredSize);
        }

        //now flush all fields  
        if (layoutWriter == null) {
            if (layoutGenerator == null) {
                layoutGenerator = new ReflectionLayoutGenerator();
            }
            layoutWriter = new AutoLayoutWriter(layoutGenerator, layout);
        }
        layoutWriter.write(data, fields);
        data.flip();

        //and send it to GPU  
        if (isCpuGpu) {
            if (isTempBuffer) {
                data.position(0).limit(requiredSize);
                BUFFER.updateData(data, 0);
            } else {
                BUFFER.markUpdate(0, requiredSize);
            }
        } else {
            data.position(0).limit(requiredSize);
            BUFFER.updateData(data, 0);
        }
    }

    private static class AutoLayoutWriter {

        private final ReflectionLayoutGenerator REF_LAYOUT;
        private final BlockLayout GL_LAYOUT;

        private StringBuilder sb1 = new StringBuilder();
        private StringBuilder sb2 = new StringBuilder();

        private AutoLayoutWriter(ReflectionLayoutGenerator ref, BlockLayout gl) {
            REF_LAYOUT = ref;
            GL_LAYOUT = gl;
        }

        private void write(ByteBuffer buffer, SafeArrayList<BlockField> fieldValues) {
            for (BlockField field : fieldValues.getArray()) {
                writeField(buffer, true, 0, 0, field.getName(), field.getName(), field.getBlockVarType(), field.getValue());
            }
        }

        private void writeField(ByteBuffer buffer, boolean isTopLevel, int topIndex, int index,
                String rawFieldName, String fieldName, BlockVarType fieldType, Object fieldValue) {
            //System.out.println(" - writing field: " + fieldName + " (" + rawFieldName + "), isTopLevel: " + isTopLevel + ", topIndex: " + topIndex + ", index: " + index + ", with value " + fieldValue + " of type " + fieldValue.getClass().getSimpleName());
            switch (fieldType) {
                case Struct:
                    SafeArrayList<FieldInfo> structLayout = REF_LAYOUT.getLayoutInformationList(fieldValue.getClass().getSimpleName());
                    if (structLayout == null) {
                        throw new IllegalArgumentException("cannot find reflection layout for value: " + fieldValue + ". Please use registerStruct() method");
                    }
                    writeStruct(buffer, false, isTopLevel, topIndex, index, structLayout, rawFieldName, fieldName, fieldValue);
                    break;
                case StructArray:
                    writeStructArray(buffer, isTopLevel, topIndex, rawFieldName, fieldName, fieldValue);
                    break;
                default:
                    String query = rawFieldName;
                    if (fieldType.isArray()) {
                        query += "[0]";
                    }
                    BlockFieldLayout fieldLayout = GL_LAYOUT.getFieldLayout(query);
                    if (fieldLayout == null) {
                        throw new IllegalArgumentException("cannot find layout for field " + fieldName + ", with value: " + fieldValue + ". Make sure it is defined in the shader");
                    }

                    int offset = fieldLayout.getTopLevelArrayStride() * topIndex + fieldLayout.getArrayStride() * index + fieldLayout.getOffset();
                    //System.out.println("        - " + offset + ": " + fieldName + ", at [" + topIndex + ", " + index + "] with value: " + fieldValue + " of type: " + fieldLayout.getType());
                    buffer.position(offset);
                    BufferWriterUtils.writeField(buffer, fieldLayout.getArrayStride(), fieldLayout.getMatrixStride(), fieldLayout.getType(), fieldValue);
                    break;
            }

        }

        private void writeStructArray(ByteBuffer buffer, boolean isTopLevel, int topIndex, String rawFieldName, String fieldName, Object fieldValue) {
            //System.out.println(" - writing struct array: " + fieldName + ", isTopLevel: " + isTopLevel + ", topLevelIndex; " + topIndex + ", with value: " + fieldValue);
            SafeArrayList<FieldInfo> refFields;
            if (fieldValue.getClass().isArray()) {
                int length = Array.getLength(fieldValue);
                for (int i = 0; i < length; i++) {
                    Object element = Array.get(fieldValue, i);
                    refFields = REF_LAYOUT.getLayoutInformationList(element.getClass().getSimpleName());
                    if (refFields == null) {
                        throw new IllegalArgumentException("cannot find layout for field " + fieldName + ", with value: " + fieldValue + ". Please use registerStruct() method");
                    }
                    writeStruct(buffer, true, isTopLevel, isTopLevel ? i : topIndex, isTopLevel ? 0 : i, refFields, rawFieldName, fieldName, element);
                }
            } else if (fieldValue instanceof Collection<?>) {
                int counter = 0;
                for (Object element : ((Collection<?>) fieldValue)) {
                    refFields = REF_LAYOUT.getLayoutInformationList(element.getClass().getSimpleName());
                    if (refFields == null) {
                        throw new IllegalArgumentException("cannot find layout for field " + fieldName + ", with value: " + fieldValue + ". Please use registerStruct() method");
                    }
                    int c = counter++;
                    writeStruct(buffer, true, isTopLevel, isTopLevel ? c : topIndex, isTopLevel ? 0 : c, refFields, rawFieldName, fieldName, element);
                }
            } else {
                throw new UnsupportedOperationException("field is supposed to be struct array but cannot be converted: " + fieldName + ", with value " + fieldValue);
            }
        }

        private void writeStruct(ByteBuffer buffer, boolean inArray, boolean isTopLevel, int topIndex, int index, SafeArrayList<FieldInfo> refFieldLayouts, String rawFieldName, String fieldName, Object value) {
            //System.out.println(" - writing struct: " + fieldName + ", isTopLevel: " + isTopLevel + ", index: " + index + ", with value: " + value);
            for (FieldInfo refLayout : refFieldLayouts) {

                try {
                    Object fieldValue = refLayout.getField().get(value);
                    sb1.setLength(0);
                    sb2.setLength(0);

                    sb1.append(fieldName);
                    sb2.append(rawFieldName);
                    if (inArray) {
                        if (isTopLevel) {
                            sb1.append("[").append(topIndex).append("]");
                            sb2.append("[0]");
                        } else {
                            sb1.append("[").append(index).append("]");
                            sb2.append("[").append(index).append("]");
                        }
                    }
                    String name = sb1.append(".").append(refLayout.getNameInShader()).toString();
                    String raw = sb2.append(".").append(refLayout.getNameInShader()).toString();
                    writeField(buffer, false, topIndex, index, raw, name, refLayout.getType(), fieldValue);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    System.out.println("failed to write field: " + ex.getMessage());
                }
            }
        }
    }
}
