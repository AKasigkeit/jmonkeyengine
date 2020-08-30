/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;


import com.jme3.renderer.opengl.GL2; 
import com.jme3.shader.VarType;

/**
 *
 * @author Alexander Kasigkeit
 */
public enum Type {
    Struct(-1, null, null),
    Bool(GL2.GL_BOOL, VarType.Boolean, null),
    Int(GL2.GL_INT, VarType.Int, VarType.IntArray),
    Float(GL2.GL_FLOAT, VarType.Float, VarType.FloatArray),
    Vec2(GL2.GL_FLOAT_VEC2, VarType.Vector2, VarType.Vector2Array),
    Vec3(GL2.GL_FLOAT_VEC3, VarType.Vector3, VarType.Vector3Array),
    Vec4(GL2.GL_FLOAT_VEC4, VarType.Vector4, VarType.Vector4Array),
    Mat3(GL2.GL_FLOAT_MAT3, VarType.Matrix3, VarType.Matrix3Array),
    Mat4(GL2.GL_FLOAT_MAT4, VarType.Matrix4, VarType.Matrix4Array);

    private final int GL_TYPE;
    private final VarType TYPE;
    private final VarType ARRAY_TYPE;

    private Type(int glType, VarType type, VarType arrType) {
        GL_TYPE = glType;
        TYPE = type;
        ARRAY_TYPE = arrType;
    }

    public int getGL() {
        return GL_TYPE;
    }

    public VarType getVarType() {
        return TYPE;
    }

    public VarType getArrayVarType() {
        return ARRAY_TYPE;
    }

    public static Type fromGLConstant(int gl) {
        switch (gl) {
            case GL2.GL_BOOL:
                return Type.Bool;
            case GL2.GL_FLOAT:
                return Type.Float;
            case GL2.GL_INT:
                return Type.Int;
            case GL2.GL_FLOAT_VEC2:
                return Type.Vec2;
            case GL2.GL_FLOAT_VEC3:
                return Type.Vec3;
            case GL2.GL_FLOAT_VEC4:
                return Type.Vec4;
            case GL2.GL_FLOAT_MAT3:
                return Type.Mat3;
            case GL2.GL_FLOAT_MAT4:
                return Type.Mat4;
        }
        return null;
    }

    public static Type fromVarType(VarType t) {
        switch (t) {
            case Int:
            case IntArray:
                return Type.Int;
            case Float:
            case FloatArray:
                return Type.Float;
            case Boolean:
                return Type.Bool;
            case Vector2:
            case Vector2Array:
                return Type.Vec2;
            case Vector3:
            case Vector3Array:
                return Type.Vec3;
            case Vector4:
            case Vector4Array:
                return Type.Vec4;
            case Matrix3:
            case Matrix3Array:
                return Type.Mat3;
            case Matrix4:
            case Matrix4Array:
                return Type.Mat4;
        }
        return null;
    }
}
