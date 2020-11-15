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
    private final BlockVarType type;
    private final int index, offset, blockIndex, arrayStride, arraySize, matrixStride, topLevelArraySize, topLevelArrayStride;
    private final boolean matrixRowMajor;

    public BlockFieldLayout(String name, int index, int offset, BlockVarType type, int blockIndex, int arrayStride,
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

    /**
     * Returns the name of this BlockField
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the index of this BlockField
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the offset of this BlockField
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the type of this BlockField
     *
     * @return the type
     */
    public BlockVarType getType() {
        return type;
    }

    /**
     * Returns the block index of this BlockField
     *
     * @return the blockIndex
     */
    public int getBlockIndex() {
        return blockIndex;
    }

    /**
     * Returns the array stride of this BlockField
     *
     * @return the array stride
     */
    public int getArrayStride() {
        return arrayStride;
    }

    /**
     * Returns the array side of this BlockField
     *
     * @return the array side
     */
    public int getArraySize() {
        return arraySize;
    }

    /**
     * Returns the matrix stride of this BlockField
     *
     * @return the matrix stride
     */
    public int getMatrixStride() {
        return matrixStride;
    }

    /**
     * Returns the top level array size of this BlockField
     *
     * @return the top level array size
     */
    public int getTopLevelArraySize() {
        return topLevelArraySize;
    }

    /**
     * Returns the top level array stride of this BlockField
     *
     * @return the top level array stride
     */
    public int getTopLevelArrayStride() {
        return topLevelArrayStride;
    }

    /**
     * Returns true if this BlockField is in row-major mode
     *
     * @return the row-major mode
     */
    public boolean isMatrixRowMajor() {
        return matrixRowMajor;
    }

    /**
     * prints all information to the console, helpful for debugging glsl code
     */
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
