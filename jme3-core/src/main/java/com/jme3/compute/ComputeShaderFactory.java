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

    public int[] getMaxLocalSize1D() {
        return MAX_LOCAL_SIZES_1;
    }

    public int[] getMaxLocalSize2D() {
        return MAX_LOCAL_SIZES_2;
    }

    public int[] getMaxLocalSize3D() {
        return MAX_LOCAL_SIZES_3;
    }

    private void initMaxLocalSizes() {
        int maxInvocations = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupInvocations);
        int maxSizeX = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeX);
        int maxSizeY = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeY);
        int maxSizeZ = getLimitOrZero(Limits.ComputeShaderMaxWorkGroupSizeZ);

        //max local size for 1 dimenstional
        MAX_LOCAL_SIZES_1[0] = Math.min(maxSizeX, maxInvocations);

        //max local size for two dimensional
        int sqrt = (int) Math.sqrt(maxInvocations);
        int twoDimX;
        if (isPowerOfTwo(sqrt)) {
            twoDimX = sqrt;
        } else {
            twoDimX = 1;
            while (twoDimX < sqrt) {
                twoDimX <<= 1;
            }
            twoDimX >>= 1;
        }
        int twoDimY = maxInvocations / twoDimX;
        MAX_LOCAL_SIZES_2[0] = Math.min(maxSizeX, twoDimX);
        MAX_LOCAL_SIZES_2[1] = Math.min(maxSizeY, twoDimY);

        //max local size for three dimensional
        int thirdrt = thirdRoot(maxInvocations);
        int threeDimX, threeDimY, threeDimZ;
        if (isPowerOfTwo(thirdrt)) {
            threeDimX = thirdrt;
            threeDimY = thirdrt;
            threeDimZ = thirdrt;
        } else {
            threeDimZ = 1;
            while (threeDimZ < thirdrt) {
                threeDimZ <<= 1;
            }
            threeDimZ >>= 1;
            threeDimZ = Math.min(maxSizeZ, threeDimZ);
            int remainingInvocations = maxInvocations / threeDimZ;
            int rt = (int) Math.sqrt(remainingInvocations);
            if (isPowerOfTwo(rt)) {
                threeDimX = rt;
                threeDimY = rt;
            } else {
                threeDimX = 1;
                while (threeDimX < rt) {
                    threeDimX <<= 1;
                }
                threeDimX >>= 1;
                threeDimX = Math.min(maxSizeX, threeDimX);
                threeDimY = maxInvocations / (threeDimZ * threeDimX);
                if (!isPowerOfTwo(threeDimY)) {
                    int v = 1;
                    while (v < threeDimY) {
                        v <<= 1;
                    }
                    threeDimY = v;
                }
            }
        }
        MAX_LOCAL_SIZES_3[0] = Math.min(maxSizeY, threeDimY);
        MAX_LOCAL_SIZES_3[1] = Math.min(maxSizeX, threeDimX);
        MAX_LOCAL_SIZES_3[2] = Math.min(maxSizeZ, threeDimZ);

        //print for debug
        System.out.println("max invocations: " + maxInvocations + ", maxSizes: (" + maxSizeX + ", " + maxSizeY + ", " + maxSizeZ + ")");
        System.out.println("MAX LOCAL SIZE 1 : " + Arrays.toString(MAX_LOCAL_SIZES_1));
        System.out.println("MAX LOCAL SIZE 2 : " + Arrays.toString(MAX_LOCAL_SIZES_2));
        System.out.println("MAX LOCAL SIZE 3 : " + Arrays.toString(MAX_LOCAL_SIZES_3));
    }

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & n - 1) == 0;
    }

    private int thirdRoot(int value) {
        return (int) Math.pow(value, 1.0 / 3.0);
    }

    private int getLimitOrZero(Limits limit) {
        Integer value = RENDERER.getLimits().get(limit);
        if (value == null) {
            return 0;
        }
        return value;
    }
}
