/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.compute;

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
