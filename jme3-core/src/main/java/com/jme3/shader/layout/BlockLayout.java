/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import com.jme3.util.SafeArrayList;

/**
 *
 * @author Alexander Kasigkeit
 */
public class BlockLayout {

    private final String name;
    private final int index, size;
    private final BlockFieldLayout[] layouts;

    public BlockLayout(BlockFieldLayout[] layouts, String name, int index, int size) {
        this.name = name;
        this.index = index;
        this.size = size;
        this.layouts = layouts;
    }
    
    public String getName() {
        return name;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getSize() {
        return size;
    }
    
    public BlockFieldLayout[] getFieldLayouts() {
        return layouts;
    }
}