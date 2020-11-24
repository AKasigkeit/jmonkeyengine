/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.buffers;

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import java.util.Random;

/**
 *
 * @author Alexander Kasigkeit
 */
public class TestCameraUniformBuffer extends TestUtil.AutoScreenshotApp {

    public static final int NUM_GEOS = 1024 * 2;
    public static final boolean SEPERATE_MESHES = false;

    public static void main(String[] args) {
        TestCameraUniformBuffer t = new TestCameraUniformBuffer();
        t.start();
    }

    private Geometry[] geos = new Geometry[NUM_GEOS];

    @Override
    public void simpleInitApp() {
        if (!renderer.getCaps().contains(Caps.UniformBufferObject)) {
            throw new UnsupportedOperationException("Hardware doesnt support UniformBufferObjects");
        }
        super.simpleInitApp();

        Texture tex = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        Material mat = new Material(assetManager, "jme3test/ubo/CameraUniformBuffer.j3md");
        //Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");    
        mat.setTexture("ColorMap", tex);
        Mesh box = SEPERATE_MESHES ? null : new Box(0.5f, 0.5f, 0.5f);
        Random r = new Random(56103465223L);
        for (int i = 0; i < NUM_GEOS; i++) {
            geos[i] = new Geometry("box_" + i, SEPERATE_MESHES ? new Box(0.5f, 0.5f, 0.5f) : box);
            geos[i].setMaterial(mat);
            geos[i].setLocalTranslation(
                    (r.nextFloat() * 100) - 50, //for quick and dirty, Math.random() is fine
                    (r.nextFloat() * 100) - 50,
                    (r.nextFloat() * 100) - 50);
            rootNode.attachChild(geos[i]);
        }

        stateManager.attach(new DetailedProfilerState());
        flyCam.setMoveSpeed(20f);
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        for (Geometry geo : geos) {
            geo.rotate(tpf, tpf * 2f, 0f);
        }
    }

}
