/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shader.layout;

import com.jme3.util.SafeArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexander Kasigkeit
 */
public class BlockLayout {

    private final String name;
    private final int index, size;
    private final SafeArrayList<BlockFieldLayout> fields = new SafeArrayList<>(BlockFieldLayout.class);
    private final Map<String, BlockFieldLayout> fieldsMap = new HashMap<>();

    public BlockLayout(String name, int index, int size) {
        this.name = name;
        this.index = index;
        this.size = size;
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

    public void addFieldLayout(BlockFieldLayout field) {
        fields.add(field);
        fieldsMap.put(field.getName(), field);
    }

    public SafeArrayList<BlockFieldLayout> getFieldLayouts() {
        return fields;
    }
    
    public BlockFieldLayout getFieldLayout(String fieldName) {
        return fieldsMap.get(fieldName);
    }
}