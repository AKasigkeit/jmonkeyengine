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
import com.jme3.shader.VarType;
import com.jme3.util.SafeArrayList;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 *
 * @author Alexander Kasigkeit
 */
public class BufferWriterUtils {

    public static int writeField(final ByteBuffer data, int index, int length, int arrStride, int matStride, VarType type, Object value) {
        if (index < 0) {
            return 0;
        }

        switch (type) {
            case Int:
                return writeInt(data, ((Number) value).intValue());
            case Float:
                return writeFloat(data, ((Number) value).floatValue());
            case Boolean:
                return writeBoolean(data, (Boolean) value);
            case Vector2:
                return writeVector2(data, (Vector2f) value);
            case Vector3:
                return writeVector3(data, (Vector3f) value);
            case Vector4:
                return writeVector4(data, value);
            case Matrix3:
                return writeMatrix3(data, (Matrix3f) value, matStride - 12);
            case Matrix4:
                return writeMatrix4(data, (Matrix4f) value, matStride - 16);
            case FloatArray:
                return writeFloatArray(data, (float[]) value, arrStride - 4);
            case IntArray:
                return writeIntArray(data, (int[]) value, arrStride - 4);
            case Vector2Array:
                return writeVector2Array(data, value, arrStride - 8);
            case Vector3Array:
                return writeVector3Array(data, value, arrStride - 12);
            case Vector4Array:
                return writeVector4Array(data, value, arrStride - 16);
            case Matrix3Array:
                int mat3colPad = matStride - 12;
                int mat3arrPad = arrStride - 3 * matStride;
                return writeMatrix3Array(data, value, mat3colPad, mat3arrPad);
            case Matrix4Array:
                int mat4colPad = matStride - 16;
                int mat4arrPad = arrStride - 4 * matStride;
                return writeMatrix4Array(data, value, mat4colPad, mat4arrPad);
        }
        return 0;
    }

    public static int writeInt(final ByteBuffer data, int value) {
        data.putInt(value);
        return 4;
    }

    public static int writeFloat(final ByteBuffer data, float value) {
        data.putFloat(value);
        return 4;
    }

    public static int writeBoolean(final ByteBuffer data, boolean value) {
        data.putInt(value ? 1 : 0);
        return 4;
    }

    public static int writeFloats(final ByteBuffer data, float value1, float value2) {
        data.putFloat(value1).putFloat(value2);
        return 8;
    }

    public static int writeFloats(final ByteBuffer data, float value1, float value2, float value3) {
        data.putFloat(value1);
        data.putFloat(value2);
        data.putFloat(value3);
        return 12;

    }

    public static int writeFloats(final ByteBuffer data, float value1, float value2, float value3, float value4) {
        data.putFloat(value1).putFloat(value2).putFloat(value3).putFloat(value4);
        return 16;
    }

    public static int writeVector2(final ByteBuffer data, Vector2f value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f);
        } else {
            return writeFloats(data, value.x, value.y);
        }
    }

    public static int writeVector3(final ByteBuffer data, Vector3f value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f, 0f);
        } else {
            return writeFloats(data, value.x, value.y, value.z);
        }
    }

    public static int writeVector4(final ByteBuffer data, Object value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f, 0f, 0f);
        } else if (value instanceof Vector4f) {
            return writeVector4(data, (Vector4f) value);
        } else if (value instanceof Quaternion) {
            return writeVector4(data, (Quaternion) value);
        } else {
            return writeVector4(data, (ColorRGBA) value);
        }
    }

    public static int writeVector4(final ByteBuffer data, Vector4f value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f, 0f, 0f);
        } else {
            return writeFloats(data, value.x, value.y, value.z, value.w);
        }
    }

    public static int writeVector4(final ByteBuffer data, Quaternion value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f, 0f, 0f);
        } else {
            return writeFloats(data, value.getX(), value.getY(), value.getZ(), value.getW());
        }
    }

    public static int writeVector4(final ByteBuffer data, ColorRGBA value) {
        if (value == null) {
            return writeFloats(data, 0f, 0f, 0f, 0f);
        } else {
            return writeFloats(data, value.r, value.g, value.b, value.a);
        }
    }

    public static int writeMatrix3(final ByteBuffer data, Matrix3f value, int columnPadding) {
        int written = 0;
        if (value == null) {
            written += writeFloats(data, 0f, 0f, 0f);
            written += pad(data, columnPadding);
            written += writeFloats(data, 0f, 0f, 0f);
            written += pad(data, columnPadding);
            written += writeFloats(data, 0f, 0f, 0f);
            written += pad(data, columnPadding);
        } else {
            written += writeFloats(data, value.get(0, 0), value.get(1, 0), value.get(2, 0));
            written += pad(data, columnPadding);
            written += writeFloats(data, value.get(0, 1), value.get(1, 1), value.get(2, 1));
            written += pad(data, columnPadding);
            written += writeFloats(data, value.get(0, 2), value.get(1, 2), value.get(2, 2));
            written += pad(data, columnPadding);
        }
        return written;
    }

    public static int writeMatrix4(final ByteBuffer data, Matrix4f value, int columnPadding) {
        int written = 0;
        if (value == null) {
            written += writeFloats(data, 0f, 0f, 0f, 0f);
            written += pad(data, columnPadding);
            written += writeFloats(data, 0f, 0f, 0f, 0f);
            written += pad(data, columnPadding);
            written += writeFloats(data, 0f, 0f, 0f, 0f);
            written += pad(data, columnPadding);
            written += writeFloats(data, 0f, 0f, 0f, 0f);
            written += pad(data, columnPadding);
        } else {
            written += writeFloats(data, value.get(0, 0), value.get(1, 0), value.get(2, 0), value.get(3, 0));
            written += pad(data, columnPadding);
            written += writeFloats(data, value.get(0, 1), value.get(1, 1), value.get(2, 1), value.get(3, 1));
            written += pad(data, columnPadding);
            written += writeFloats(data, value.get(0, 2), value.get(1, 2), value.get(2, 2), value.get(3, 2));
            written += pad(data, columnPadding);
            written += writeFloats(data, value.get(0, 3), value.get(1, 3), value.get(2, 3), value.get(3, 3));
            written += pad(data, columnPadding);
        }
        return written;
    }

    public static int writeFloatArray(final ByteBuffer data, float[] values, int padding) {
        int written = 0;
        if (values != null) {
            for (float value : values) {
                written += writeFloat(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeIntArray(final ByteBuffer data, int[] values, int padding) {
        int written = 0;
        if (values != null) {
            for (int value : values) {
                written += writeInt(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector2Array(final ByteBuffer data, Object value, int padding) {
        if (value == null) {
            return 0;
        } else if (value instanceof Vector2f[]) {
            return writeVector2Array(data, (Vector2f[]) value, padding);
        } else if (value instanceof SafeArrayList<?>) {
            return writeVector2Array(data, (SafeArrayList<Vector2f>) value, padding);
        } else {
            return writeVector2Array(data, (Collection<Vector2f>) value, padding);
        }
    }

    public static int writeVector2Array(final ByteBuffer data, Vector2f[] values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector2f value : values) {
                written += writeFloats(data, value.x, value.y);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector2Array(final ByteBuffer data, SafeArrayList<Vector2f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector2f value : values.getArray()) {
                written += writeFloats(data, value.x, value.y);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector2Array(final ByteBuffer data, Collection<Vector2f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector2f value : values) {
                written += writeFloats(data, value.x, value.y);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector3Array(final ByteBuffer data, Object value, int padding) {
        if (value == null) {
            return 0;
        } else if (value instanceof Vector3f[]) {
            return writeVector3Array(data, (Vector3f[]) value, padding);
        } else if (value instanceof SafeArrayList<?>) {
            return writeVector3Array(data, (SafeArrayList<Vector3f>) value, padding);
        } else {
            return writeVector3Array(data, (Collection<Vector3f>) value, padding);
        }
    }

    public static int writeVector3Array(final ByteBuffer data, Vector3f[] values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector3f value : values) {
                written += writeVector3(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector3Array(final ByteBuffer data, SafeArrayList<Vector3f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector3f value : values.getArray()) {
                written += writeVector3(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector3Array(final ByteBuffer data, Collection<Vector3f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector3f value : values) {
                written += writeVector3(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector4Array(final ByteBuffer data, Object value, int padding) {
        if (value == null) {
            return 0;
        } else if (value instanceof Vector4f[]) {
            return writeVector4Array(data, (Vector4f[]) value, padding);
        } else if (value instanceof SafeArrayList<?>) {
            return writeVector4Array(data, (SafeArrayList<Vector4f>) value, padding);
        } else {
            return writeVector4Array(data, (Collection<Vector4f>) value, padding);
        }
    }

    public static int writeVector4Array(final ByteBuffer data, Vector4f[] values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector4f value : values) {
                written += writeVector4(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector4Array(final ByteBuffer data, SafeArrayList<Vector4f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector4f value : values.getArray()) {
                written += writeVector4(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeVector4Array(final ByteBuffer data, Collection<Vector4f> values, int padding) {
        int written = 0;
        if (values != null) {
            for (Vector4f value : values) {
                written += writeVector4(data, value);
                written += pad(data, padding);
            }
        }
        return written;
    }

    public static int writeMatrix3Array(final ByteBuffer data, Object value, int columnPadding, int arrayPadding) {
        if (value == null) {
            return 0;
        } else if (value instanceof Matrix3f[]) {
            return writeMatrix3Array(data, (Matrix3f[]) value, columnPadding, arrayPadding);
        } else if (value instanceof SafeArrayList<?>) {
            return writeMatrix3Array(data, (SafeArrayList<Matrix3f>) value, columnPadding, arrayPadding);
        } else {
            return writeMatrix3Array(data, (Collection<Matrix3f>) value, columnPadding, arrayPadding);
        }
    }

    public static int writeMatrix3Array(final ByteBuffer data, Matrix3f[] values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix3f value : values) {
                written += writeMatrix3(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int writeMatrix3Array(final ByteBuffer data, SafeArrayList<Matrix3f> values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix3f value : values.getArray()) {
                written += writeMatrix3(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int writeMatrix3Array(final ByteBuffer data, Collection<Matrix3f> values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix3f value : values) {
                written += writeMatrix3(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int writeMatrix4Array(final ByteBuffer data, Object value, int columnPadding, int arrayPadding) {
        if (value == null) {
            return 0;
        } else if (value instanceof Matrix4f[]) {
            return writeMatrix4Array(data, (Matrix4f[]) value, columnPadding, arrayPadding);
        } else if (value instanceof SafeArrayList<?>) {
            return writeMatrix4Array(data, (SafeArrayList<Matrix4f>) value, columnPadding, arrayPadding);
        } else {
            return writeMatrix4Array(data, (Collection<Matrix4f>) value, columnPadding, arrayPadding);
        }
    }

    public static int writeMatrix4Array(final ByteBuffer data, Matrix4f[] values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix4f value : values) {
                written += writeMatrix4(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int writeMatrix4Array(final ByteBuffer data, SafeArrayList<Matrix4f> values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix4f value : values.getArray()) {
                written += writeMatrix4(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int writeMatrix4Array(final ByteBuffer data, Collection<Matrix4f> values, int columnPadding, int arrayPadding) {
        int written = 0;
        if (values != null) {
            for (Matrix4f value : values) {
                written += writeMatrix4(data, value, columnPadding);
                written += pad(data, arrayPadding);
            }
        }
        return written;
    }

    public static int pad(final ByteBuffer data, int bytes) {
        if (bytes <= 0) {
            return 0;
        }

        for (int i = 0; i < bytes; i++) {
            data.put((byte) 0);
        }
        return bytes;
    }
}
