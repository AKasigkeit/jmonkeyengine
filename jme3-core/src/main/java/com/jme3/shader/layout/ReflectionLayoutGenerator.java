/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import com.jme3.util.SafeArrayList;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Extracts layout data relevant for getting opengl buffer data written
 * correctly using reflection
 *
 * @author Alexander Kasigkeit
 */
public class ReflectionLayoutGenerator {

    private Map<String, Map<String, FieldInfo>> layouts = new HashMap<>();
    private Map<String, SafeArrayList<FieldInfo>> layoutsList = new HashMap<>();

    public ReflectionLayoutGenerator() {

    }

    public Map<String, SafeArrayList<FieldInfo>> getLayoutLists() {
        return layoutsList;
    }

    public <T extends Struct> void registerStruct(Class<T> struct) {
        String name = struct.getSimpleName();
        Map<String, FieldInfo> infoMap = layouts.get(name);
        if (infoMap != null) {
            throw new IllegalArgumentException("struct of same type has already been registered: " + name);
        }

        Field[] fields = struct.getDeclaredFields();
        if (fields.length == 0) {
            throw new IllegalArgumentException("provided struct (" + name + ") does not declare any fields");
        }

        infoMap = new HashMap<>(fields.length);
        SafeArrayList<FieldInfo> infoList = new SafeArrayList<>(FieldInfo.class, fields.length);
        for (Field field : fields) {
            field.setAccessible(true);
            Member m = field.getDeclaredAnnotation(Member.class);
            if (m != null) {
                String fieldName = field.getName();
                BlockVarType type = m.type();
                if (type == null || type == BlockVarType.AutoDetect) {
                    type = BlockVarType.fromClass(field.getType());
                    if (type == null) { //not a well known one, check if it is a struct or struct array
                        if (field.getType().isArray() && Struct.class.isAssignableFrom(field.getType().getComponentType())) {
                            type = BlockVarType.StructArray;
                        } else if (Struct.class.isAssignableFrom(field.getType())) {
                            type = BlockVarType.Struct;
                        }
                    }
                }

                if (type == null) {
                    throw new IllegalArgumentException("cannot detect type of field: " + field.getName());
                }
                String maps = m.maps();
                if (maps == null || Member.AUTOMAPPING.equals(maps)) {
                    maps = fieldName;
                }
                FieldInfo info = new FieldInfo(fieldName, field, type, maps);
                infoMap.put(fieldName, info);
                infoList.add(info);
            }
        }
        int memFieldCount = infoMap.size();
        if (memFieldCount < 1) {
            throw new IllegalArgumentException("provided class (" + name + ") does not declare any member fields. Please use annotation @Member");
        }

        layoutsList.put(name, infoList);
        layouts.put(name, infoMap);
    }

    public Map<String, FieldInfo> getLayoutInformation(String structName) {
        return layouts.get(structName);
    }

    public SafeArrayList<FieldInfo> getLayoutInformationList(String structName) {
        return layoutsList.get(structName);
    }

    public static class FieldInfo {

        private final String name;
        private final Field field;
        private final String maps;
        private final BlockVarType type;

        private FieldInfo(String name, Field field, BlockVarType type, String maps) {
            this.name = name;
            this.field = field;
            this.type = type;
            this.maps = maps;
        }

        /**
         * Returns the glsl name of this field
         *
         * @return the glsl name
         */
        public String getNameInShader() {
            return maps;
        }

        /**
         * Returns the cpu-side name of this field
         *
         * @return the cpu-side name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the Field used for reflection
         *
         * @return field for reflection
         */
        public Field getField() {
            return field;
        }

        /**
         * Returns the BlockVarType of this Field
         *
         * @return the blockVarType
         */
        public BlockVarType getType() {
            return type;
        }
    }

}
