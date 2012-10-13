
package com.ardor3d.example.pipeline;

import java.io.IOException;
import java.util.List;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.util.SkeletalDebugger;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.DataMode;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

/**
 * Illustrates gpu skinning with normal map, specular map and diffuse coloring.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.AnimationBlinnPhongExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_AnimationBlinnPhongExample.jpg", //
maxHeapMemory = 64)
public class AnimationBlinnPhongExample extends ExampleBase {
    private ColladaImporter colladaImporter;
    private ColladaStorage colladaStorage;
    private Node colladaNode;
    private GLSLShaderObjectsState gpuShader;
    private float quantizationFactor = 8f; // used for posterization
    private boolean useNormalMap = false;
    private boolean useDiffuseMap = false;
    private boolean useSpecularMap = false;
    private boolean showSkeleton = false;

    private static final boolean UPDATE_BOUNDS = false;
    private static final double UPDATE_RATE = 0.03333333333333333;

    private boolean updateLight = true;
    final double LOOP_TIME = 15d;

    // temporals
    private final Transform calcTrans1 = new Transform();
    private final Transform calcTrans2 = new Transform();
    private final Transform calcTrans3 = new Transform();
    private final Quaternion calcQuat1 = new Quaternion();
    private PointLight p, pp;

    private BasicText frameRateLabel;
    private int frames = 0;
    private long startTime = System.currentTimeMillis();
    private boolean showMesh = true;

    public static void main(final String[] args) {
        ExampleBase.start(AnimationBlinnPhongExample.class);
    }

    @Override
    protected void initExample() {
        importDogDae();

        p = new PointLight();
        p.setDiffuse(new ColorRGBA(0.75f, 0.65f, 0.65f, 0.65f));
        p.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        p.setEnabled(true);

        p.setLocation(Vector3.ZERO);
        pp = (PointLight) ((LightState) _root.getLocalRenderState(RenderState.StateType.Light)).get(0);
        ((LightState) _root.getLocalRenderState(RenderState.StateType.Light)).attach(p);
        pp.setAmbient(new ColorRGBA(.0f, .0f, .0f, 1f));
        pp.setDiffuse(new ColorRGBA(.39f, .39f, .39f, 1f));
        pp.setSpecular(new ColorRGBA(.2f, .2f, .2f, 1f));
        pp.setConstant(.1f);
        pp.setLinear(0.0000008f);
        pp.setQuadratic(0.0000008f);

        final Box sb = new Box("SkyBox", Vector3.ZERO, 50, 50, 50);
        sb.setModelBound(new BoundingBox());
        _root.attachChild(sb);

        final BasicText t1 = BasicText.createDefaultTextLabel("Text1", "[K] Show Skeleton.");
        t1.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t1.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t1.setTranslation(new Vector3(5, 0 * (t1.getHeight() + 5) + 10, 0));
        _root.attachChild(t1);
        _root.getSceneHints().setCullHint(CullHint.Never);

        final BasicText t2 = BasicText.createDefaultTextLabel("Text2", "[M] Hide Mesh.");
        t2.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t2.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t2.setTranslation(new Vector3(5, 1 * (t2.getHeight() + 5) + 10, 0));
        _root.attachChild(t2);
        _root.getSceneHints().setCullHint(CullHint.Never);

        final BasicText t3 = createTextLabel("Text3", "[U] Use Normal Map.", 2);
        final BasicText t4 = createTextLabel("Text4", "[F] Use Diffuse Map.", 3);
        final BasicText t5 = createTextLabel("Text5", "[P] Use Specular Map.", 4);
        final BasicText t6 = createTextLabel("Text5", "[Y] Enable Light Motion.", 5);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                useDiffuseMap = !useDiffuseMap;
                gpuShader.setUniform("flags", useNormalMap, useDiffuseMap, useSpecularMap, false);
                if (useDiffuseMap) {
                    t4.setText("[F] Skip Diffuse Map.");
                } else {
                    t4.setText("[F] Use Diffuse Map.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NUMPADADD), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (quantizationFactor > 1) {
                    quantizationFactor /= 2f;
                }
                gpuShader.setUniform("quantizationFactor", 1f / quantizationFactor);
                System.out.println("quantizationFactor = " + quantizationFactor);
            }
        }));
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.NUMPADSUBTRACT),
                new TriggerAction() {
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        if (quantizationFactor < 512) {
                            quantizationFactor *= 2f;
                        }
                        gpuShader.setUniform("quantizationFactor", 1f / quantizationFactor);
                        System.out.println("quantizationFactor = " + quantizationFactor);
                    }
                }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.Y), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                updateLight = !updateLight;
                if (updateLight) {
                    t6.setText("[Y] Disable Light Motion.");
                } else {
                    t6.setText("[Y] Enable Light Motion.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.P), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                useSpecularMap = !useSpecularMap;
                gpuShader.setUniform("flags", useNormalMap, useDiffuseMap, useSpecularMap, false);
                if (useSpecularMap) {
                    t5.setText("[P] Skip Specular Map.");
                } else {
                    t5.setText("[P] Use Specular Map.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.U), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                useNormalMap = !useNormalMap;
                gpuShader.setUniform("flags", useNormalMap, useDiffuseMap, useSpecularMap, false);
                if (useNormalMap) {
                    t3.setText("[U] Skip Normal Map.");
                } else {
                    t3.setText("[U] Use Normal Map.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showSkeleton = !showSkeleton;
                if (showSkeleton) {
                    t1.setText("[K] Hide Skeleton.");
                } else {
                    t1.setText("[K] Show Skeleon.");
                }
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.M), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                showMesh = !showMesh;
                colladaNode.getSceneHints().setCullHint(showMesh ? CullHint.Dynamic : CullHint.Always);
                if (showMesh) {
                    t2.setText("[M] Hide Mesh.");
                } else {
                    t2.setText("[M] Show Mesh.");
                }
            }
        }));

        frameRateLabel = BasicText.createDefaultTextLabel("fpsLabel", "");
        frameRateLabel.setTranslation(5, _canvas.getCanvasRenderer().getCamera().getHeight() - 5
                - frameRateLabel.getHeight(), 0);
        frameRateLabel.setTextColor(ColorRGBA.WHITE);
        frameRateLabel.getSceneHints().setOrthoOrder(-1);
        _root.attachChild(frameRateLabel);

        _root.updateGeometricState(0);
        _root.updateWorldBound(true);
        _root.updateWorldTransform(true);

    }

    private BasicText createTextLabel(final String name, final String text, final int pos) {
        final BasicText t3 = BasicText.createDefaultTextLabel(name, text);
        t3.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        t3.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        t3.setTranslation(new Vector3(5, pos * (t3.getHeight() + 5) + 10, 0));
        _root.attachChild(t3);
        _root.getSceneHints().setCullHint(CullHint.Never);
        return t3;
    }

    private void importDogDae() {

        final ResourceSource rts = new URLResourceSource(ResourceLocatorTool.getClassPathResource(
                AnimationBlinnPhongExample.class,
                "com/ardor3d/example/media/models/collada/juanita/dog_Apr18_normals.dds"));
        final Texture t = TextureManager.load(rts, Texture.MinificationFilter.NearestNeighborNearestMipMap, true);
        final ResourceSource specularRS = new URLResourceSource(ResourceLocatorTool.getClassPathResource(
                AnimationBlinnPhongExample.class,
                "com/ardor3d/example/media/models/collada/juanita/dog_Apr18_specular.dds"));
        final Texture specular = TextureManager.load(specularRS,
                Texture.MinificationFilter.NearestNeighborNearestMipMap, false);

        colladaImporter = new ColladaImporter();

        final ResourceSource rs = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,
                "com/ardor3d/example/media/models/collada/juanita/dog_Apr18_smooth.dae");
        try {
            colladaStorage = colladaImporter.load(rs);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }

        colladaNode = colladaStorage.getScene();

        gpuShader = new GLSLShaderObjectsState();
        gpuShader.setEnabled(true);
        try {
            gpuShader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(
                    AnimationBlinnPhongExample.class,
                    "com/ardor3d/example/media/models/collada/juanita/skinning_gpu2.vert"));
            gpuShader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(
                    AnimationBlinnPhongExample.class,
                    "com/ardor3d/example/media/models/collada/juanita/skinning_gpu2.frag"));

            gpuShader.setUniform("quantizationFactor", 1f / quantizationFactor);
            gpuShader.setUniform("flags", useNormalMap, useDiffuseMap, useSpecularMap, false);

            gpuShader.setUniform("colorMap", 0);
            gpuShader.setUniform("normalMap", 1);
            gpuShader.setUniform("specularMap", 2);

            colladaNode.acceptVisitor(new Visitor() {
                public void visit(final Spatial spatial) {
                    if (spatial instanceof SkinnedMesh) {
                        final SkinnedMesh skinnedMesh = (SkinnedMesh) spatial;
                        skinnedMesh.setGPUShader(gpuShader);

                        final TextureState ts = (TextureState) skinnedMesh
                                .getLocalRenderState(RenderState.StateType.Texture);
                        if (ts != null) {
                            ts.setTexture(t, 1);
                            ts.setTexture(specular, 2);
                            skinnedMesh.setRenderState(ts);
                        }

                        final MaterialState ms = new MaterialState();
                        ms.setAmbient(MaterialState.MaterialFace.FrontAndBack, ColorRGBA.WHITE);
                        ms.setDiffuse(MaterialState.MaterialFace.FrontAndBack, ColorRGBA.WHITE);
                        ms.setSpecular(MaterialState.MaterialFace.FrontAndBack, ColorRGBA.WHITE);
                        ms.setShininess(32);
                        skinnedMesh.setRenderState(ms);

                        skinnedMesh.setGpuAttributeSize(3);
                        skinnedMesh.setGpuUseMatrixAttribute(true);
                        skinnedMesh.setUseGPU(true);
                    }
                }
            }, false);
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }

        final Node rot = new Node("Model Rotation");

        rot.getSceneHints().setDataMode(DataMode.VBO);
        rot.attachChild(colladaNode);
        _root.attachChild(rot);

    }

    @Override
    protected void renderDebug(final Renderer renderer) {
        super.renderDebug(renderer);
        if (showSkeleton) {
            SkeletalDebugger.drawSkeletons(_root, renderer, true, true);
        }
    }

    double time = 0.0, lightTime = 0.0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        time += timer.getTimePerFrame();
        if (time > UPDATE_RATE) {
            time -= UPDATE_RATE;
            final List<SkinData> skinDataList = colladaStorage.getSkins();
            final SkinData skinData = skinDataList.get(0);
            final SkeletonPose pose = skinData.getPose();

            final double time = timer.getTimeInSeconds();

            // Neck
            final Quaternion q = Quaternion.fetchTempInstance();
            final double vv = (time % (LOOP_TIME * 2d)) / LOOP_TIME - 1d;
            final double v = (vv > 0) ? vv : -vv;
            q.fromAngleAxis(v * 60 * MathUtils.DEG_TO_RAD - 30 * MathUtils.DEG_TO_RAD, Vector3.UNIT_Z);
            targetJoint(pose, 13, q);

            q.fromAngleAxis(v * 5 * MathUtils.DEG_TO_RAD - 35 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X);
            q.fromAngleAxis(v * 75 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X);

            Quaternion.releaseTempInstance(q);

            p.setLocation(Math.sin(time) * 5, Math.cos(time) * 5 + 10, 5);
            if (updateLight) {
                lightTime = time;
            }
            pp.setLocation(Math.sin(lightTime / 5) * 5, Math.cos(lightTime / 5) * 5 + 10, 5);

            pose.updateTransforms();

            if (UPDATE_BOUNDS) {
                final List<SkinnedMesh> skins = skinData.getSkins();
                for (final SkinnedMesh skinnedMesh : skins) {
                    skinnedMesh.updateModelBound();
                }
            }
        }

        final long now = System.currentTimeMillis();
        final long dt = now - startTime;
        if (dt > 200) {
            final long fps = Math.round(1e3 * frames / dt);
            frameRateLabel.setText(fps + " fps");

            startTime = now;
            frames = 0;
        }
        frames++;
    }

    private void targetJoint(final SkeletonPose pose, final int jointIndex, final Quaternion rotation) {
        final Joint[] joints = pose.getSkeleton().getJoints();
        final Transform[] transforms = pose.getLocalJointTransforms();

        final short parentIndex = joints[jointIndex].getParentIndex();

        // neckBindGlobalTransform is the neck bone -> model space transform. essentially, it is the world transform of
        // the neck bone in bind pose.
        final ReadOnlyTransform inverseNeckBindGlobalTransform = joints[jointIndex].getInverseBindPose();
        final ReadOnlyTransform neckBindGlobalTransform = inverseNeckBindGlobalTransform.invert(calcTrans1);

        calcQuat1.fromRotationMatrix(neckBindGlobalTransform.getMatrix());
        calcQuat1.slerpLocal(calcQuat1, rotation, 1);

        final Transform subTransform = calcTrans2.setIdentity();
        subTransform.setRotation(calcQuat1);

        // Calculate a global version of that transform, as if it were attached to the neck
        final Transform subGlobal = neckBindGlobalTransform.multiply(subTransform, calcTrans3);

        // now remove the global/world transform of the neck's parent bone, leaving us with just the local transform of
        // neck + rotation.
        final Transform local = joints[parentIndex].getInverseBindPose().multiply(subGlobal, calcTrans2);

        // set that as the neck's transform
        transforms[jointIndex].set(local);
    }

}
