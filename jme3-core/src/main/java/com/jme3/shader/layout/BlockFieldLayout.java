/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

/**
 *
 * @author Alexander Kasigkeit
 */
public class BlockFieldLayout {

    private final String name;
    private final int index, offset, type, blockIndex, arrayStride, arraySize, matrixStride, topLevelArraySize, topLevelArrayStride;
    private final boolean matrixRowMajor;

    public BlockFieldLayout(String name, int index, int offset, int type, int blockIndex, int arrayStride,
            int arraySize, int matrixStride, int topLevelArraySize, int topLevelArrayStride, boolean matrixRowMajor) {
        this.name = name;
        this.index = index;
        this.offset = offset;
        this.type = type;
        this.blockIndex = blockIndex;
        this.arrayStride = arrayStride;
        this.arraySize = arraySize;
        this.matrixStride = matrixStride;
        this.topLevelArraySize = topLevelArraySize;
        this.topLevelArrayStride = topLevelArrayStride;
        this.matrixRowMajor = matrixRowMajor;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public int getOffset() {
        return offset;
    }

    public int getType() {
        return type;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public int getArrayStride() {
        return arrayStride;
    }

    public int getArraySize() {
        return arraySize;
    }

    public int getMatrixStride() {
        return matrixStride;
    }

    public int getTopLevelArraySize() {
        return topLevelArraySize;
    }

    public int getTopLevelArrayStride() {
        return topLevelArrayStride;
    }

    public boolean isMatrixRowMajor() {
        return matrixRowMajor;
    }

    public void printInfo() {
        System.out.println("index : " + index);
        System.out.println(" - name  : " + name);
        System.out.println(" - offset: " + offset);
        System.out.println(" - type  : " + type);
        System.out.println(" - blockIndex : " + blockIndex);
        System.out.println(" - arrStride  : " + arrayStride);
        System.out.println(" - arrSize    : " + arraySize);
        System.out.println(" - matStride  : " + matrixStride);
        System.out.println(" - matRowMajor: " + matrixRowMajor);
        System.out.println(" - topLevelArraySize  : " + topLevelArraySize);
        System.out.println(" - topLevelArrayStride: " + topLevelArrayStride);
    }

}
