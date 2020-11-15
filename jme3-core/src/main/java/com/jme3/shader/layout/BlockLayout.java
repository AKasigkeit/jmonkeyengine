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

    private final String NAME;
    private final int index, size;
    private final BlockFieldLayout[] layouts;
    private final StructNode tree;
    private final Map<String, BlockFieldLayout> layoutsMap;

    public BlockLayout(BlockFieldLayout[] layouts, String n, int index, int size) {
        this.NAME = n;
        this.index = index;
        this.size = size;
        this.layouts = layouts;
        layoutsMap = new HashMap<>(layouts.length);
        for (BlockFieldLayout layout : layouts) {
            layoutsMap.put(layout.getName(), layout);
        }
        //create tree
        StructNode rootStruct = new StructNode(n, false, 0);
        for (BlockFieldLayout fieldLayout : layouts) {
            String[] parts = fieldLayout.getName().split("\\.");
            StructNode parent = rootStruct;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                String name = part;
                boolean isArray = false;
                int arraySlot = 0;
                if (part.endsWith("]")) {
                    name = name.substring(0, part.indexOf("["));
                    isArray = true;
                    arraySlot = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                }

                StructNode node = parent.getChild(name);
                if (node == null) {
                    if (i == parts.length - 1) {
                        node = new StructField(name, isArray, fieldLayout.getTopLevelArrayStride(), fieldLayout);
                    } else {
                        node = new StructNode(name, isArray, fieldLayout.getTopLevelArrayStride());
                    }
                    parent.children.put(name, node);
                }
                node.arrayLength = Math.max(node.arrayLength, arraySlot + 1);
                if (i == 0) {
                    node.arrayLength = fieldLayout.getTopLevelArraySize();
                } else if (i == parts.length - 1) {
                    node.arrayLength = Math.max(node.arrayLength, fieldLayout.getArraySize());
                }
                parent = node;
            }
        }
        tree = rootStruct;
    }

    /**
     * Returns the name of this block
     *
     * @return the name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the index of this block
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the size of this block (for block with open arrays at the end,
     * assumes an array length of 1)
     *
     * @return the size of this block
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns an array of BlockFieldLayouts describing all of this blocks
     * members fields
     *
     * @return an array of BlockFieldLayouts
     */
    public BlockFieldLayout[] getFieldLayouts() {
        return layouts;
    }

    /**
     * Returns the BlockFieldLayout related to the field with the provided name
     *
     * @param name name of the field to get layout of
     * @return BlockFieldLayout of the requested field or null if it was not
     * declared in the shader.
     */
    public BlockFieldLayout getFieldLayout(String name) {
        return layoutsMap.get(name);
    }

    /**
     * Returns a tree-like view of the layout of this block
     *
     * @return tree-view of the layout
     */
    public StructNode getTreeView() {
        return tree;
    }

    public static class StructNode {

        private final String name;
        private final boolean isArray;
        private final int stride;
        private int arrayLength;
        private final Map<String, StructNode> children;

        private StructNode(String name, boolean isArray, int stride) {
            this(name, isArray, stride, false);
        }

        private StructNode(String name, boolean isArray, int stride, boolean isLeaf) {
            this.name = name;
            this.isArray = isArray;
            this.stride = stride;
            children = isLeaf ? null : new HashMap<>(4);
        }

        /**
         * Returns the stride of this struct-node
         *
         * @return the stride
         */
        public int getStride() {
            return stride;
        }

        /**
         * Returns the name of this node
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns true if this node is an array
         *
         * @return true if array, false otherwise
         */
        public boolean isArray() {
            return isArray;
        }

        /**
         * Returns the length of this node in case it is an array. Returns 0 for
         * open-arrays and 1 in case it is not an array at all.
         *
         * @return the length
         */
        public int getArrayLength() {
            return arrayLength;
        }

        /**
         * Returns the child of this node with the provided name or null if it
         * doesnt exits.
         *
         * @param name name of the child to look for
         */
        public StructNode getChild(String name) {
            return children.get(name);
        }

        /**
         * Returns the child field of this node with the provided name or null
         * if it doesnt exist.
         *
         * @param name name of the field to look for
         * @return the field with the requested name
         */
        public StructField getField(String name) {
            StructNode node = children.get(name);
            if (node == null || !(node instanceof StructField)) {
                return null;
            }
            return (StructField) node;
        }

        /**
         * Prints the structure of this node to the console. Helpful for
         * debugging glsl code
         */
        public void printDebug() {
            System.out.println("node " + name + " debug view: ");
            printDebug(" ");
        }

        private void printDebug(String indent) {
            if (this instanceof StructField) {
                System.out.println(indent + "- field: " + name + " (" + (((StructField) this).getLayout().getType()) + "), array? " + isArray + ", stride: " + stride + ", length: " + arrayLength);
            } else {
                System.out.println(indent + "- struct: " + name + ", array? " + isArray + ", stride: " + stride + ", length: " + arrayLength);
                for (StructNode node : children.values()) {
                    node.printDebug(indent + "  ");
                }
            }
        }
    }

    public static class StructField extends StructNode {

        private final BlockFieldLayout layout;

        private StructField(String name, boolean isArray, int stride, BlockFieldLayout l) {
            super(name, isArray, stride, true);
            layout = l;
        }

        /**
         * Returns the BlockFieldLayout describing this field.
         *
         * @return BlockFieldLayout of this field
         */
        public BlockFieldLayout getLayout() {
            return layout;
        }
    }
}
