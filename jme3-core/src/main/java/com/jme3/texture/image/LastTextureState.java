/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.texture.image;

import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Renderer;
import com.jme3.texture.Texture;

/**
 * Stores / caches texture state parameters so they don't have to be set 
 * each time by the {@link Renderer}.
 * 
 * @author Kirill Vainer
 */
public final class LastTextureState {
    
    public Texture.WrapMode sWrap, tWrap, rWrap;
    public Texture.MagFilter magFilter;
    public Texture.MinFilter minFilter;
    public ColorRGBA color = ColorRGBA.Black;
    public int anisoFilter;
    public Texture.ShadowCompareMode shadowCompareMode;
    
    public LastTextureState() {
        reset();
    }
    
    public void reset() {
        sWrap = null;
        tWrap = null;
        rWrap = null;
        magFilter = null;
        minFilter = null;
        anisoFilter = 1;
        
        // The default in OpenGL is OFF, so we avoid setting this per texture
        // if it's not used.
        shadowCompareMode = Texture.ShadowCompareMode.Off;
    }
}
