/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit
 */
public class BlockLayout {

    private final String name;
    private final int index, size;
    private final BlockFieldLayout[] layouts;
    private final Map<String, BlockFieldLayout> layoutsMap;

    public BlockLayout(BlockFieldLayout[] layouts, String name, int index, int size) {
        this.name = name;
        this.index = index;
        this.size = size;
        this.layouts = layouts;
        layoutsMap = new HashMap<>(layouts.length);
        for (BlockFieldLayout layout : layouts) {
            layoutsMap.put(layout.getName(), layout);
        }
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

    public BlockFieldLayout getFieldLayout(String name) {
        return layoutsMap.get(name);
    }
}
