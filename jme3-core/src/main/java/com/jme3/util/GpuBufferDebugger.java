/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util;

import com.jme3.buffer.UntypedBuffer;
import com.jme3.shader.VarType;
import java.nio.ByteBuffer;

/**
 *
 * @author Alexander Kasigkeit
 */
public class GpuBufferDebugger {

    private static ByteBuffer buf = BufferUtils.createByteBuffer(64);
    private static StringBuilder sb = new StringBuilder(256);

    /**
     * Only works when called on the main thread. Given a direct buffer,
     * downloads its whole content from the GPU and prints it to the console.
     * Useful to check if compute shaders actually wrote data to the buffer for
     * example
     *
     * @param buffer the buffer to check
     * @param type the type to interpret and print the data as
     */
    public static void print(UntypedBuffer buffer, VarType type, int minLen) {
        int bytesPerType = 0;
        switch (type) {
            case Int:
            case Float:
            case Boolean:
                bytesPerType = 4;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (buffer.getSizeOnGpu() > buf.capacity()) {
            buf = BufferUtils.createByteBuffer(buffer.getSizeOnGpu());
        } else {
            buf.position(0).limit(buffer.getSizeOnGpu());
        }
        sb.setLength(0);
        buffer.downloadData(buf, 0);
        for (int i = 0; i < buffer.getSizeOnGpu(); i += bytesPerType) {
            switch (type) {
                case Int:
                    String intS = String.valueOf(buf.getInt(i));
                    fill(minLen - intS.length());
                    sb.append(intS).append(" ");
                    break;
                case Float:
                    String floatS = String.valueOf(buf.getFloat(i));
                    fill(minLen - floatS.length());
                    sb.append(floatS).append(" ");
                    break;
            }
        }
        System.out.println(sb.toString());
    }

    private static void fill(int num) {
        for (int i = 0; i < num; i++) {
            sb.append(" ");
        }
    }

}
