/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.compute;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 *
 * @author Alexander Kasigkeit
 */
public class ComputeShaderFactory {

    public static class ComputeShaderNotAvailableException extends Exception {

        private ComputeShaderNotAvailableException() {
            super("ComputeShaders are not supported by the hardware");
        }

    }

    public static ComputeShaderFactory create(Renderer renderer) throws ComputeShaderNotAvailableException {
        if (!renderer.getCaps().contains(Caps.ComputeShader)) {
            throw new ComputeShaderNotAvailableException();
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

    protected void run(ComputeShader computeShader, int x, int y, int z, MemoryBarrierBits bits) {
        //prepareShader(computeShader);

        RENDERER.runComputeShader(computeShader, x, y, z);
        RENDERER.memoryBarrier(bits);
    }

    protected void setShader(ComputeShader shader) {
        RENDERER.setShader(shader.getShader());
    }

    protected void getLocalWorkGroupSize(ComputeShader shader, int[] store) {
        RENDERER.getLocalWorkGroupSize(shader, store);
    }

    protected int getFormat(Image.Format f) {
        switch (f) {
            case RGBA8:
                return GL3.GL_RGBA8I;
            // R ints (signed and unsigned)
            case R8I:
                return GL3.GL_R8I;
            case R8UI:
                return GL3.GL_R8UI;
            case R16I:
                return GL3.GL_R16I;
            case R16UI:
                return GL3.GL_R16UI;
            case R32I:
                return GL3.GL_R32I;
            case R32UI:
                return GL3.GL_R32UI;
            // R and G ints (signed and unsigned)
            case RG8I:
                return GL3.GL_RG8I;
            case RG8UI:
                return GL3.GL_RG8UI;
            case RG16I:
                return GL3.GL_RG16I;
            case RG16UI:
                return GL3.GL_RG16UI;
            case RG32I:
                return GL3.GL_RG32I;
            case RG32UI:
                return GL3.GL_RG32UI;
            // R, G, B and A ints (signed and unsigned)
            case RGBA8I:
                return GL3.GL_RGBA8I;
            case RGBA8UI:
                return GL3.GL_RGBA8UI;
            case RGBA16I:
                return GL3.GL_RGBA16I;
            case RGBA16UI:
                return GL3.GL_RGBA16UI;
            case RGBA32I:
                return GL3.GL_RGBA32I;
            case RGBA32UI:
                return GL3.GL_RGBA32UI;
            // floats 
            case R16F:
                return GL3.GL_R16F;
            case R32F:
                return GL3.GL_R32F;
            case RG16F:
                return GL3.GL_RG16F;
            case RG32F:
                return GL3.GL_RG32F;
            case RGBA16F:
                return GL3.GL_RGBA16F;
            case RGBA32F:
                return GL3.GL_RGBA32F;
            // rest
            case RGB10A2:
                return GL2.GL_RGB10_A2;

            default:
                throw new IllegalArgumentException("unsupported image format: " + f);
        }
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
