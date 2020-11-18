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

import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;

/**
 *
 * @author Alexander Kasigkeit <alexander.kasigkeit@web.de>
 */
public class WorkGroupSizes {

    private final int[][][] MAX_LOCAL_SIZES = new int[2][3][3];

    public WorkGroupSizes(Renderer renderer) {
        int maxInvocations = getLimitOrZero(renderer, Limits.ComputeShaderMaxWorkGroupInvocations);
        int maxSizeX = getLimitOrZero(renderer, Limits.ComputeShaderMaxWorkGroupSizeX);
        int maxSizeY = getLimitOrZero(renderer, Limits.ComputeShaderMaxWorkGroupSizeY);
        int maxSizeZ = getLimitOrZero(renderer, Limits.ComputeShaderMaxWorkGroupSizeZ);

        //max local size for 1 dimenstional
        MAX_LOCAL_SIZES[0][0][0] = Math.min(maxSizeX, maxInvocations);
        MAX_LOCAL_SIZES[0][0][1] = 1;
        MAX_LOCAL_SIZES[0][0][2] = 1;

        //max local size for two dimensional 
        int m2x = getPowerOfTwoSmallerEqual((int) Math.sqrt(maxInvocations));
        int m2y = maxInvocations / m2x;
        MAX_LOCAL_SIZES[0][1][0] = Math.min(maxSizeX, Math.max(m2x, m2y));
        MAX_LOCAL_SIZES[0][1][1] = Math.min(maxSizeY, Math.min(m2x, m2y));
        MAX_LOCAL_SIZES[0][1][2] = 1;

        int m3x = getPowerOfTwoSmallerEqual((int) Math.pow(maxInvocations, 1.0 / 3.0));
        int m3y = getPowerOfTwoSmallerEqual(maxInvocations / (m3x * m3x));
        int m3z = maxInvocations / (m3x * m3y);
        if (m3x < m3y) {
            int tmp = m3x;
            m3x = m3y;
            m3y = tmp;
        }
        if (m3y < m3z) {
            int tmp = m3y;
            m3y = m3z;
            m3z = tmp;
        }
        if (m3x < m3y) {
            int tmp = m3x;
            m3x = m3y;
            m3y = tmp;
        }
        MAX_LOCAL_SIZES[0][2][0] = Math.min(maxSizeX, m3x);
        MAX_LOCAL_SIZES[0][2][1] = Math.min(maxSizeY, m3y);
        MAX_LOCAL_SIZES[0][2][2] = Math.min(maxSizeZ, m3z);

        //now calculate power of 2 versions (that are recommended)
        //for 1D
        MAX_LOCAL_SIZES[1][0][0] = getPowerOfTwoSmallerEqual(MAX_LOCAL_SIZES[0][0][0]);
        MAX_LOCAL_SIZES[1][0][1] = 1;
        MAX_LOCAL_SIZES[1][0][2] = 1;

        //for 2D
        int p2x = getPowerOfTwoSmallerEqual(MAX_LOCAL_SIZES[0][1][0]);
        int p2y = getPowerOfTwoSmallerEqual(maxInvocations / p2x);
        MAX_LOCAL_SIZES[1][1][0] = Math.min(maxSizeX, Math.max(p2x, p2y));
        MAX_LOCAL_SIZES[1][1][1] = Math.min(maxSizeY, Math.min(p2x, p2y));
        MAX_LOCAL_SIZES[1][1][2] = 1;

        //and for 3D
        int p3x = getPowerOfTwoSmallerEqual(MAX_LOCAL_SIZES[0][2][0]);
        int p3y = getPowerOfTwoSmallerEqual(maxInvocations / (p3x * p3x));
        int p3z = getPowerOfTwoSmallerEqual(maxInvocations / (p3x * p3y));
        if (p3x < p3y) {
            int tmp = p3x;
            p3x = p3y;
            p3y = tmp;
        }
        if (p3y < p3z) {
            int tmp = p3y;
            p3y = p3z;
            p3z = tmp;
        }
        if (p3x < p3y) {
            int tmp = p3x;
            p3x = p3y;
            p3y = tmp;
        }
        MAX_LOCAL_SIZES[1][2][0] = Math.min(maxSizeX, p3x);
        MAX_LOCAL_SIZES[1][2][1] = Math.min(maxSizeY, p3y);
        MAX_LOCAL_SIZES[1][2][2] = Math.min(maxSizeZ, p3z);
    }

    public int getMaxLocalSize(int totalDimensions, int dimension, boolean forcePOT) {
        if (totalDimensions < 1 || totalDimensions > 3) {
            throw new IllegalArgumentException("totalDimensions needs to be between 1 and 3");
        }
        if (dimension < 1 || dimension > 3) {
            throw new IllegalArgumentException("dimension needs to be between 1 and 3");
        }
        return MAX_LOCAL_SIZES[forcePOT ? 1 : 0][totalDimensions - 1][dimension - 1];
    }

    private int getPowerOfTwoSmallerEqual(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p == n ? n : p >> 1;
    }

    private int getLimitOrZero(Renderer renderer, Limits limit) {
        Integer value = renderer.getLimits().get(limit);
        if (value == null) {
            return 0;
        }
        return value;
    }
}
