
package com.ardor3d.example.pipeline;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.clip.TriggerCallback;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;

public class FireballTrigger implements TriggerCallback {

  private final AnimationDemoExample example;

  public FireballTrigger() {
    // a bit of a hack. in a real game we might use manager singletons, etc.
    example = AnimationDemoExample.instance;
  }

  @Override
  public void doTrigger(final SkeletonPose applyToPose, final AnimationManager manager) {
    GameTaskQueueManager.getManager(example.getCanvas().getCanvasRenderer().getRenderContext()).update(() -> {
      addParticles(example.getMeshForPose(applyToPose));
      return null;
    });
  }

  private void addParticles(final SkinnedMesh mesh) {
    // find location of fist
    final SkeletonPose pose = mesh.getCurrentPose();
    final Transform loc = mesh.getWorldTransform().multiply(pose.getGlobalJointTransforms()[15], null);

    // Spawn a short lived explosion
    final ParticleSystem explosion = ParticleFactory.buildParticles("big", 80);
    explosion.setEmissionDirection(new Vector3(0.0f, 1.0f, 0.0f));
    explosion.setMaximumAngle(MathUtils.PI);
    explosion.setSpeed(0.9f);
    explosion.setMinimumLifeTime(300.0f);
    explosion.setMaximumLifeTime(500.0f);
    explosion.setStartSize(2.0f);
    explosion.setEndSize(5.0f);
    explosion.setStartColor(new ColorRGBA(1.0f, 0.312f, 0.121f, 1.0f));
    explosion.setEndColor(new ColorRGBA(1.0f, 0.24313726f, 0.03137255f, 0.0f));
    explosion.setControlFlow(false);
    explosion.setInitialVelocity(0.04f);
    explosion.setParticleSpinSpeed(0.0f);
    explosion.setRepeatType(RepeatType.CLAMP);

    // attach to root, at fist location
    explosion.setTransform(loc);
    explosion.warmUp(1);
    example.getRoot().attachChild(explosion);
    example.getRoot().updateWorldTransform(true);

    explosion.getParticleController().addListener(particles -> explosion.removeFromParent());
    explosion.forceRespawn();

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    blend.setDestinationFunction(BlendState.DestinationFunction.One);
    explosion.setRenderState(blend);

    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear,
        TextureStoreFormat.GuessCompressedFormat, true));
    ts.getTexture().setWrap(WrapMode.BorderClamp);
    ts.setEnabled(true);
    explosion.setRenderState(ts);

    final ZBufferState zstate = new ZBufferState();
    zstate.setWritable(false);
    explosion.setRenderState(zstate);
  }
}
