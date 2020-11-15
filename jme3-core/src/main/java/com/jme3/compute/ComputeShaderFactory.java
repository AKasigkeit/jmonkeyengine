/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.compute;

import com.jme3.asset.AssetManager;
import com.jme3.buffer.DispatchIndirectBuffer;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import java.util.Arrays;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ComputeShaderFactory {

    /**
     * Creates a new ComputeShaderFactory. Will return null is compute shaders
     * are not supported by the hardware.
     *
     * @param renderer the renderer needed to access the underlying system
     * @return the new ComputeShaderFactory
     */
    public static ComputeShaderFactory create(Renderer renderer) {
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            return null;
        }
        return new ComputeShaderFactory(renderer);
    }

    private final Renderer RENDERER;

    private final int[][][] MAX_LOCAL_SIZES = new int[2][3][3];

    private final int[] MAX_LOCAL_SIZES_1 = new int[1];
    private final int[] MAX_LOCAL_SIZES_2 = new int[2];
    private final int[] MAX_LOCAL_SIZES_3 = new int[3];

    public ComputeShaderFactory(Renderer renderer) {
        RENDERER = renderer;
        initMaxLocalSizes();
    }

    /**
     * Creates a compute shader instance from the file located at the specified
     * location with the specified GLSL version
     *
     * @param assetManager assetManager to help load the file
     * @param location location of the shader file, something like
     * "Shaders/Compute/MyShader.comp"
     * @param language version string of the shader, eg "GLSL430"
     * @return the created ComputeShader instance
     */
    public ComputeShader createComputeShader(AssetManager assetManager, String location, String language) {
        String code = (String) assetManager.loadAsset(location);
        return new ComputeShader(this, code, language, location);
    }

    /**
     * Creates a ComputeShader from the given String, useful to generate
     * ComputeShader code procedurally.
     *
     * @param sourceCode the code to generate a ComputeShader from
     * @param language the GLSL version string, eg "GLSL430"
     * @return the created ComputeShader instance
     */
    public ComputeShader createComputeShader(String sourceCode, String language) {
        return new ComputeShader(this, sourceCode, language, null);
    }
    
    protected void queryLayouts(ComputeShader shader) {
        RENDERER.queryBlockLayouts(shader);
    }

    protected void run(ComputeShader computeShader, DispatchIndirectBuffer buffer, int offset, MemoryBarrierBits bits) {
        RENDERER.runComputeShader(computeShader, buffer, offset);
        RENDERER.memoryBarrier(bits);
    }

    protected void run(ComputeShader computeShader, int x, int y, int z, MemoryBarrierBits bits) {
        RENDERER.runComputeShader(computeShader, x, y, z);
        RENDERER.memoryBarrier(bits);
    }

    protected void setShader(ComputeShader shader) {
        RENDERER.setShader(shader.getShader());
    }

    protected void getLocalWorkGroupSize(ComputeShader shader, int[] store) {
        RENDERER.getLocalWorkGroupSize(shader, store);
    }

    public int getMaxLocalSize(int totalDims, int dim, boolean forcePOT) {
        if (totalDims < 1 || totalDims > 3) {
            throw new IllegalArgumentException("totalDims must be between 1 and 3");
        }
        if (dim < 1 || dim > 3) {
            throw new IllegalArgumentException("dim must be between 1 and 3");
        }
        return MAX_LOCAL_SIZES[forcePOT ? 1 : 0][totalDims][dim];
    }

    private void initMaxLocalSizes() {
        int maxInvocations = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupInvocations);
        int maxSizeX = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeX);
        int maxSizeY = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeY);
        int maxSizeZ = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeZ);

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
        /*
        for (int i = 0; i < 3; i++) {
            for (int p = 0; p < 2; p++) {
                System.out.println("DIM " + (i + 1) + " with" + (p == 0 ? "out" : "") + " power of two:");
                for (int j = 0; j < (i + 1); j++) {
                    System.out.print(" " + MAX_LOCAL_SIZES[p][i][j]);
                }
                System.out.println();
            }
        }
         */
    }

    public int[] getMaxLocalSize1D() {
        return MAX_LOCAL_SIZES[0][0];
    }

    public int[] getMaxLocalSize2D() {
        return MAX_LOCAL_SIZES[0][1];
    }

    public int[] getMaxLocalSize3D() {
        return MAX_LOCAL_SIZES[0][2];
    }

    private int getPowerOfTwoSmallerEqual(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p == n ? n : p >> 1;
    }

    private int getLimitOrZero(Limits limit) {
        Integer value = RENDERER.getLimits().get(limit);
        if (value == null) {
            return 0;
        }
        return value;
    }
}
