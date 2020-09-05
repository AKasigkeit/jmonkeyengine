/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.buffer;

import com.jme3.shader.VarType;
import com.jme3.shader.layout.BlockFieldLayout;
import com.jme3.shader.layout.BufferWriterUtils;
import com.jme3.shader.layout.ReflectionLayoutGenerator;
import com.jme3.shader.layout.ReflectionLayoutGenerator.FieldInfo;
import com.jme3.shader.layout.Struct;
import com.jme3.util.BufferUtils;
import com.jme3.util.SafeArrayList;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ShaderStorageBuffer extends FieldBuffer {

    /**
     * Creates a new UniformBuffer. It will automatically layout the data
     * accoring to the layout specified in the shader as long as the names and
     * varTypes match
     *
     * @return a new UniformBuffer in autolayout mode
     */
    public static ShaderStorageBuffer createNewAutolayout() {
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticRead);
        buffer.initialize(BufferUtils.createByteBuffer(0)); //empty for now, will adjust automatically
        return buffer.asShaderStorageBuffer(null);
    }

    /**
     * Creates a new UniformBuffer. It will use the provided FieldBufferWriter
     * to write the fields into the buffer, GL is not queried for the layout and
     * it is up to the user to make sure it matches the layout in the shader
     *
     * @param writer the writer to use
     * @return a new UniformBuffer
     */
    public static ShaderStorageBuffer createNewManualLayout(FieldBufferWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("for manual layouts, writer cannot be null");
        }
        UntypedBuffer buffer = UntypedBuffer.createNewBufferDataLazy(UntypedBuffer.MemoryMode.CpuGpu, UntypedBuffer.BufferDataUsage.StaticRead);
        buffer.initialize(BufferUtils.createByteBuffer(0)); //empty for now, will adjust automatically
        return buffer.asShaderStorageBuffer(writer);
    }

    private ReflectionLayoutGenerator layoutGenerator = null;

    protected ShaderStorageBuffer(UntypedBuffer buffer, FieldBufferWriter writer) {
        super(buffer, Type.ShaderStorageBuffer, writer);
    }

    public <T extends Struct> void registerStruct(Class<T> clazz) {
        if (BUFFER.getId() != -1) {
            throw new UnsupportedOperationException("cannot register new struct when this buffer has already been created"); //TODO support that
        }
        if (layoutGenerator == null) {
            layoutGenerator = new ReflectionLayoutGenerator();
        }
        layoutGenerator.registerStruct(clazz);
    }

    @Override
    public void flushFieldUpdatesSpecific() {
        boolean isCpuGpu = BUFFER.getMemoryMode() == UntypedBuffer.MemoryMode.CpuGpu;
        boolean isTempBuffer = false;
        ByteBuffer data;
        if (isCpuGpu) {
            if (layout.getSize() > BUFFER.getSizeOnCpu()) {
                data = ByteBuffer.allocate(layout.getSize()); //will be destroyed further down again
                isTempBuffer = true;
            } else {
                data = BUFFER.getCpuData();
                data.clear();
            }
        } else {
            data = getByteBuffer(layout.getSize());
        }

        int bytesWritten = 0, bytesWrittenTotal = 0;
        int firstByte = Integer.MAX_VALUE, lastByte = Integer.MIN_VALUE;
        long start = System.nanoTime(), dur;

        String fieldName;
        try {
            for (BlockField field : fields.getArray()) {
                fieldName = isVarTypeArray(field.getVarType()) ? field.getName() + "[0]" : field.getName();
                BlockFieldLayout fieldLayout = layout.getFieldLayout(fieldName);
                if (field.getVarType() != null && fieldLayout == null) {
                    throw new IllegalArgumentException("field: " + field.getName() + " is not declared in the GLSL layout of the buffer");
                }

                if (fieldLayout != null) {
                    //it is a raw field in the buffer, not part of a struct 
                    bytesWritten += BufferWriterUtils.writeField(data,
                            fieldLayout.getIndex(), fieldLayout.getArraySize(), fieldLayout.getArrayStride(),
                            fieldLayout.getMatrixStride(), field.getVarType(), field.getValue());
                } else {
                    //it is a struct, go fancy
                    System.out.println("check for array: " + field.getValue() + ": " + isObjectArray(field.getValue()));
                    if (isObjectArray(field.getValue())) {
                        Class<?> structClass;
                        if (field.getValue().getClass().isArray()) {
                            structClass = Array.get(field.getValue(), 0).getClass();
                        } else {
                            structClass = ((List<?>) field.getValue()).get(0).getClass();
                        }
                        bytesWritten += flushStructArray(data, fieldName, structClass, field.getValue(), -1);
                    } else {
                        bytesWritten += flushStruct(data, fieldName, field.getValue().getClass(), field.getValue(), -1);

                    }
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            ex.printStackTrace();
            throw new UnsupportedOperationException("failed to read field value: " + ex.getMessage());
        }
        data.flip();

        firstByte = 0;
        lastByte = layout.getSize();
        dur = System.nanoTime() - start;
        //System.out.println("writing " + fields.size() + " fields took " + (dur / 1000000.0) + " ms, " + bytesWrittenTotal + " bytes were written. The blocks total size is: " + layout.getSize());

        if (isCpuGpu) {
            if (isTempBuffer) {
                data.position(firstByte).limit(lastByte);
                BUFFER.updateData(data, firstByte);
            } else {
                BUFFER.markUpdate(firstByte, lastByte - firstByte);
            }
        } else {
            data.position(firstByte).limit(lastByte);
            BUFFER.updateData(data, 0);
        }
    }

    private int flushStruct(ByteBuffer buffer, String fieldName, Class<?> structClass, Object value, int topLevelIndex) throws IllegalArgumentException, IllegalAccessException {
        String structName = structClass.getSimpleName();
        SafeArrayList<FieldInfo> fieldInfos = layoutGenerator.getLayoutInformationList(structName);
        if (fieldInfos == null) {
            throw new IllegalArgumentException("Struct " + structName + " is unknown. Please register structs using resgierStruct(Struct.class) before using the buffer");
        }
        System.out.println("flushStruct: " + structName + ", value: " + value);

        int bytesWritten = 0;
        for (FieldInfo fieldInfo : fieldInfos.getArray()) {
            String innerFieldName = fieldName + "." + fieldInfo.getName();
            if (fieldInfo.getDefinition().type() == com.jme3.shader.layout.Type.Struct) {
                if (fieldInfo.getDefinition().length() != 1) {
                    bytesWritten += flushStruct(buffer, innerFieldName, fieldInfo.getField().getType(), fieldInfo.getField().get(value), topLevelIndex);
                } else {
                    bytesWritten += flushStructArray(buffer, innerFieldName, fieldInfo.getField().getType().getComponentType(), fieldInfo.getField().get(value), topLevelIndex);
                }
            } else {
                innerFieldName = fieldInfo.getDefinition().length() != 1 ? innerFieldName + "[0]" : innerFieldName;
                BlockFieldLayout fieldLayout = layout.getFieldLayout(innerFieldName);
                bytesWritten += flushField(buffer, fieldLayout, topLevelIndex, fieldInfo.getField().get(value));
            }
        }
        return bytesWritten;
    }

    private int flushStructArray(ByteBuffer buffer, String fieldName, Class<?> structClass, Object value, int topLevelIndex) throws IllegalArgumentException, IllegalAccessException {
        String structName = structClass.getSimpleName();
        SafeArrayList<FieldInfo> fieldInfos = layoutGenerator.getLayoutInformationList(structName);
        if (fieldInfos == null) {
            throw new IllegalArgumentException("Struct " + structName + " is unknown. Please register structs using resgierStruct(Struct.class) before using the buffer");
        }
        System.out.println("flushStructArray: " + structName + ", value: " + value);

        //check if this struct is nested already, or if it contains a struct itsself
        boolean isNestedAlready = fieldName.contains(".");
        boolean tryNested = isNestedAlready;
        if (!isNestedAlready) {
            for (FieldInfo fieldInfo : fieldInfos.getArray()) {
                if (fieldInfo.getDefinition().type() == com.jme3.shader.layout.Type.Struct) {
                    tryNested = true;
                    break;
                }
            }
        }
        boolean isNested = isObjectArray(value);
        int length = (tryNested || isNested) ? getLength(value) : 1;
        boolean infereIndex = isNestedAlready; //if we are nested already we need to infere an actual index (not actually correct but works well enough)

        //check if we need to pass down topLevelIndex
        boolean insertTopLevel = !isNestedAlready && topLevelIndex == -1 && length != 1; //if we are a top level struct array with fixed length, insert top level index

        System.out.println("struct name: " + structName);
        System.out.println("field name: " + fieldName);
        System.out.println("isNestedAlready: " + isNestedAlready);
        System.out.println("tryNested: " + tryNested);
        System.out.println("infereIndex: " + infereIndex);
        System.out.println("isNested: " + isNested);
        System.out.println("insertTopLevel: " + insertTopLevel);
        System.out.println("length: " + length);
        int bytesWritten = 0;
        for (int i = 0; i < length; i++) {
            for (FieldInfo fieldInfo : fieldInfos.getArray()) {
                String innerFieldName = fieldName + (isNested ? infereIndex ? "[" + i + "]." : "[0]." : ".") + fieldInfo.getName();
                System.out.println("checking inner field name: " + innerFieldName);
                Object innerFieldValue = value;
                if (value instanceof List<?>) {
                    innerFieldValue = ((List<?>) value).get(i);
                } else if (value.getClass().isArray()) {
                    innerFieldValue = Array.get(value, i);
                }
                if (insertTopLevel) {
                    topLevelIndex = i;
                }
                if (fieldInfo.getDefinition().type() == com.jme3.shader.layout.Type.Struct) {
                    //struct contains a struct itsself
                    if (fieldInfo.getDefinition().length() == 1) {
                        bytesWritten += flushStruct(buffer, innerFieldName, fieldInfo.getField().getType(), fieldInfo.getField().get(innerFieldValue), topLevelIndex);
                    } else {
                        bytesWritten += flushStructArray(buffer, innerFieldName, fieldInfo.getField().getType().getComponentType(), fieldInfo.getField().get(innerFieldValue), topLevelIndex);
                    }
                } else {
                    innerFieldName = fieldInfo.getDefinition().length() != 1 ? innerFieldName + "[0]" : innerFieldName;
                    BlockFieldLayout fieldLayout = layout.getFieldLayout(innerFieldName);
                    bytesWritten += flushField(buffer, fieldLayout, topLevelIndex, fieldInfo.getField().get(innerFieldValue));
                }
            }
        }
        return bytesWritten;
    }

    private int flushField(ByteBuffer buffer, BlockFieldLayout layout, int topLevelIndex, Object fieldValue) {
        if (layout.getIndex() == -1) {
            throw new IllegalArgumentException("field " + layout.getName() + " is not declared in the GLSL layout of the buffer");
        }

        int length = getLength(fieldValue);
        int calculatedOffset = layout.getOffset();
        if (topLevelIndex != -1) {
            calculatedOffset += topLevelIndex * layout.getTopLevelArrayStride();
        }
        com.jme3.shader.layout.Type typeDef = com.jme3.shader.layout.Type.fromGLConstant(layout.getType());
        VarType varType = isObjectArray(fieldValue) ? typeDef.getArrayVarType() : typeDef.getVarType();

        buffer.position(calculatedOffset);
        System.out.println("flushing field: " + layout.getName() + " with value: " + fieldValue + " at offset: " + calculatedOffset);
        return BufferWriterUtils.writeField(buffer, 0, length, layout.getArrayStride(), layout.getMatrixStride(), varType, fieldValue);
    }

    private boolean isObjectArray(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass().isArray()) {
            return true;
        }
        return (obj instanceof Collection<?>);
    }

    private int getLength(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        } else if (obj instanceof Collection<?>) {
            return ((Collection<?>) obj).size();
        }
        return 1;
    }

    private boolean isVarTypeArray(VarType type) {
        if (type == null) {
            return false;
        }
        switch (type) {
            case IntArray:
            case FloatArray:
            case Vector2Array:
            case Vector3Array:
            case Vector4Array:
            case Matrix3Array:
            case Matrix4Array:
                return true;
        }
        return false;
    }

    @Override
    protected VarType getVarTypeByValue(Object value) {
        VarType varType = UniformBuffer.CLASS_TO_VAR_TYPE.get(value.getClass());
        if (varType != null) {
            return varType;
        } else if (value instanceof Collection<?> && ((Collection) value).isEmpty()) {
            throw new IllegalArgumentException("Can't calculate a var type for the empty collection value[" + value + "].");
        } else if (value instanceof List<?>) {
            varType = getVarTypeByValue(((List) value).get(0));
        } else if (value instanceof Collection<?>) {
            varType = getVarTypeByValue(((Collection) value).iterator().next());
        }
        if (varType != null) {
            return varType;
        }
        Object obj;
        if (value instanceof Collection<?>) {
            obj = ((Collection<?>) value).iterator().next();
        } else if (value.getClass().isArray()) {
            obj = Array.get(value, 0);
        } else {
            obj = value;
        }
        if (layoutGenerator.getLayoutInformation(obj.getClass().getSimpleName()) != null) {
            return null; //null marks struct
        }
        throw new IllegalArgumentException("Can't calculate a var type for the value " + value + " of class " + value.getClass().getSimpleName());
    }

}
