/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.compute;

import com.jme3.material.MatParam;
import com.jme3.shader.ShaderVariable;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;

/**
 *
 * @author Alexander Kasigkeit
 */
public class TexImgParam extends MatParam {

    protected int imageBindingPoint = ShaderVariable.LOC_UNKNOWN;
    protected Texture.Access access = null;
    protected int level = 0;
    protected int layer = -1;
    protected boolean sampler = false;

    protected boolean configChange = true;

    public TexImgParam(VarType type, String name, Object value, Texture.Access access, int level, int layer, boolean sampler) {
        super(type, name, value);
        this.access = access;
        this.level = level;
        this.layer = layer;
        this.sampler = sampler;
    }

    public boolean update(Texture.Access access, int level, int layer, boolean isTexture, Object value) {
        if (!checkEquals(access, level, layer, isTexture, value)) {
            applyUpdate(access, level, layer, isTexture, value);
            return true;
        }
        return false;
    }

    private boolean checkEquals(Texture.Access access, int level, int layer, boolean isTexture, Object value) {
        return this.access == access && this.level == level && this.layer == layer && this.sampler == isTexture && this.value == value; //obj with == is ok here
    }

    private void applyUpdate(Texture.Access access, int level, int layer, boolean isTexture, Object value) {
        this.access = access;
        this.level = level;
        this.layer = layer;
        this.sampler = isTexture;
        this.value = value;
        this.configChange = true;
    }

    public int getLayer() {
        return layer;
    }

    public int getLevel() {
        return level;
    }

    public boolean isSampler() {
        return sampler;
    }

    public int getImageBindingPoint() {
        return imageBindingPoint;
    }

    public void setImageBindingPoint(int point) {
        imageBindingPoint = point;
    }

    public Texture.Access getAccess() {
        return access;
    }
}
