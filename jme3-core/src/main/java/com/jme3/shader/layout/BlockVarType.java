/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit
 */
public enum BlockVarType {
    AutoDetect(false),
    Struct(false), StructArray(true),
    Int(false), IntArray(true),
    Float(false), FloatArray(true),
    Vec2(false), Vec2Array(true),
    Vec3(false), Vec3Array(true),
    Vec4(false), Vec4Array(true),
    Mat3(false), Mat3Array(true),
    Mat4(false), Mat4Array(true);

    private final boolean ARRAY;

    private BlockVarType(boolean array) {
        ARRAY = array;
    }

    /**
     * Returns true if this type is an array, false otherwise.
     *
     * @return true if this type is an array
     */
    public boolean isArray() {
        return ARRAY;
    }

    private static final Map<Class<?>, BlockVarType> CLASS_TO_TYPE = new HashMap<>();

    static {
        CLASS_TO_TYPE.put(float.class, BlockVarType.Float);
        CLASS_TO_TYPE.put(int.class, BlockVarType.Int);
        CLASS_TO_TYPE.put(float[].class, BlockVarType.FloatArray);
        CLASS_TO_TYPE.put(int[].class, BlockVarType.IntArray);
        CLASS_TO_TYPE.put(Float[].class, BlockVarType.FloatArray);
        CLASS_TO_TYPE.put(Integer[].class, BlockVarType.IntArray);

        CLASS_TO_TYPE.put(Float.class, BlockVarType.Float);
        CLASS_TO_TYPE.put(Integer.class, BlockVarType.Int);
        CLASS_TO_TYPE.put(Vector2f.class, BlockVarType.Vec2);
        CLASS_TO_TYPE.put(Vector3f.class, BlockVarType.Vec3);
        CLASS_TO_TYPE.put(ColorRGBA.class, BlockVarType.Vec4);
        CLASS_TO_TYPE.put(Quaternion.class, BlockVarType.Vec4);
        CLASS_TO_TYPE.put(Vector4f.class, BlockVarType.Vec4);

        CLASS_TO_TYPE.put(Vector2f[].class, BlockVarType.Vec2Array);
        CLASS_TO_TYPE.put(Vector3f[].class, BlockVarType.Vec3Array);
        CLASS_TO_TYPE.put(Vector4f[].class, BlockVarType.Vec4Array);
        CLASS_TO_TYPE.put(ColorRGBA[].class, BlockVarType.Vec4Array);
        CLASS_TO_TYPE.put(Quaternion[].class, BlockVarType.Vec4Array);

        CLASS_TO_TYPE.put(Matrix3f.class, BlockVarType.Mat3);
        CLASS_TO_TYPE.put(Matrix4f.class, BlockVarType.Mat4);
        CLASS_TO_TYPE.put(Matrix3f[].class, BlockVarType.Mat3Array);
        CLASS_TO_TYPE.put(Matrix4f[].class, BlockVarType.Mat4Array);
    }

    /**
     * Returns the BlockVarType of the provided class or null if it doesnt
     * exist.
     *
     * @param clazz class to get BlockVarType of
     * @return BlockVarType of the provided class
     */
    public static BlockVarType fromClass(Class<?> clazz) {
        return CLASS_TO_TYPE.get(clazz);
    }
}
