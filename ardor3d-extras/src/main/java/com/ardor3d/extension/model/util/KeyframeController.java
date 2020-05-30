/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.util;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.ComplexSpatialController;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * TODO: Revisit for better Ardor3D integration.
 *
 * Started Date: Jun 12, 2004 <br>
 *
 *
 * Class can do linear interpolation of a Mesh between units of time. Similar to
 * VertexKeyframeController but interpolates float units of time instead of integer key frames.
 *
 * setSpeed(float) sets a speed relative to the defined speed. For example, the default is 1. A
 * speed of 2 would run twice as fast and a speed of .5 would run half as fast
 *
 * setMinTime(float) and setMaxTime(float) both define the bounds that KeyframeController should
 * follow. It is the programmer's responsibility to make sure that the MinTime and MaxTime are
 * within the span of the defined setKeyframe
 *
 * Controller functions RepeatType and isActive are both defined in their default way for
 * KeyframeController
 *
 * When this controller is saved/loaded to XML format, it assumes that the mesh it morphs is the
 * Mesh it belongs to, so it is recommended to only attach this controller to the Mesh it animates.
 *
 * (Based on work by Jack Lindamood, kevglass (parts), hevee (blend time), Julien Gouesse (port to
 * Ardor3D))
 */

public class KeyframeController<T extends Spatial> extends ComplexSpatialController<T> {

  private static final Logger logger = Logger.getLogger(KeyframeController.class.getName());

  private static final long serialVersionUID = 1L;

  /**
   * An array of PointInTime s that defines the animation
   */
  transient public ArrayList<PointInTime> _keyframes;

  /**
   * A special array used with SmoothTransform to store temporary smooth transforms
   */
  transient private ArrayList<PointInTime> _prevKeyframes;

  /**
   * The mesh that is actually morphed
   */
  private Mesh _morphMesh;

  /**
   * The current time in the animation
   */
  transient private double _curTime;

  /**
   * The current frame of the animation
   */
  transient private int _curFrame;

  /**
   * The frame of animation we're heading towards
   */
  transient private int _nextFrame;

  /**
   * The PointInTime before curTime
   */
  transient private PointInTime _before;

  /**
   * The PointInTime after curTime
   */
  transient private PointInTime _after;

  /**
   * If true, the animation is moving forward, if false the animation is moving backwards
   */
  transient private boolean _movingForward = true;

  /**
   * Used with SmoothTransform to signal it is doing a smooth transform
   */
  transient private boolean _isSmooth;

  /**
   * Used with SmoothTransform to signal it is doing a smooth transform
   */
  transient private boolean _interpTex = true;

  /**
   * Used with SmoothTransform to hold the new beginning and ending time once the transform is
   * complete
   */
  transient private double _tempNewBeginTime;

  transient private double _tempNewEndTime;

  /** If true, the model's bounding volume will update every frame. */
  private boolean _updateBounding = true;

  /**
   * Default constructor. Speed is 1, MinTime is 0 MaxTime is 0. Both MinTime and MaxTime are
   * automatically adjusted by setKeyframe if the setKeyframe time is less than MinTime or greater
   * than MaxTime. Default RepeatType is WRAP.
   */
  public KeyframeController() {
    setSpeed(1);
    _keyframes = new ArrayList<>();
    _curFrame = 0;
    setRepeatType(ComplexSpatialController.RepeatType.WRAP);
    setMinTime(0);
    setMaxTime(0);
  }

  public double getCurrentTime() { return _curTime; }

  public int getCurrentFrame() { return _curFrame; }

  /**
   * Gets the current time in the animation
   */
  public double getCurTime() { return _curTime; }

  /**
   * Sets the current time in the animation
   *
   * @param time
   *          The time this Controller should continue at
   */
  public void setCurTime(final double time) { _curTime = time; }

  /**
   * Sets the Mesh that will be physically changed by this KeyframeController
   *
   * @param morph
   *          The new mesh to morph
   */
  public void setMorphingMesh(final Mesh morph) {
    _morphMesh = morph;
    _keyframes.clear();
    _keyframes.add(new PointInTime(0, null));
  }

  public void shallowSetMorphMesh(final Mesh morph) {
    _morphMesh = morph;
  }

  /**
   * Tells the controller to change its morphMesh to shape at time seconds. Time must be >=0 and shape
   * must be non-null and shape must have the same number of vertexes as the current shape. If not,
   * then nothing happens. It is also required that setMorphingMesh(Mesh) is called before
   * setKeyframe. It is assumed that shape.indices == morphMesh.indices, otherwise morphing may look
   * funny
   *
   * @param time
   *          The time for the change
   * @param shape
   *          The new shape at that time
   */
  public void setKeyframe(final double time, final Mesh shape) {
    if (_morphMesh == null || time < 0
        || shape.getMeshData().getVertexBuffer().capacity() != _morphMesh.getMeshData().getVertexBuffer().capacity()) {
      return;
    }
    for (int i = 0; i < _keyframes.size(); i++) {
      final PointInTime lookingTime = _keyframes.get(i);
      if (lookingTime._time == time) {
        lookingTime._newShape = shape;
        return;
      }
      if (lookingTime._time > time) {
        _keyframes.add(i, new PointInTime(time, shape));
        return;
      }
    }
    _keyframes.add(new PointInTime(time, shape));
    if (time > getMaxTime()) {
      setMaxTime(time);
    }
    if (time < getMinTime()) {
      setMinTime(time);
    }
  }

  /**
   * This function will do a smooth translation between a keframe's current look, to the look directly
   * at newTimeToReach. It takes translationLen time (in seconds) to do that translation, and once
   * translated will animate like normal between newBeginTime and newEndTime <br>
   * <br>
   * This would be useful for example when a figure stops running and tries to raise an arm. Instead
   * of "teleporting" to the raise-arm animation beginning, a smooth translation can occur.
   *
   * @param newTimeToReach
   *          The time to reach.
   * @param translationLen
   *          How long it takes
   * @param newBeginTime
   *          The new cycle beginning time
   * @param newEndTime
   *          The new cycle ending time.
   */
  public void setSmoothTranslation(final float newTimeToReach, final float translationLen, final float newBeginTime,
      final float newEndTime) {
    if (!isActive() || _isSmooth) {
      return;
    }
    if (newBeginTime < 0 || newBeginTime > _keyframes.get(_keyframes.size() - 1)._time) {
      KeyframeController.logger.warning("Attempt to set invalid begintime:" + newBeginTime);
      return;
    }
    if (newEndTime < 0 || newEndTime > _keyframes.get(_keyframes.size() - 1)._time) {
      KeyframeController.logger.warning("Attempt to set invalid endtime:" + newEndTime);
      return;
    }
    Mesh begin = null, end = null;
    if (_prevKeyframes == null) {
      _prevKeyframes = new ArrayList<>();
      begin = new Mesh();
      end = new Mesh();
    } else {
      begin = _prevKeyframes.get(0)._newShape;
      end = _prevKeyframes.get(1)._newShape;
      _prevKeyframes.clear();
    }

    getCurrent(begin);

    _curTime = newTimeToReach;
    _curFrame = 0;
    setMinTime(0);
    setMaxTime(_keyframes.get(_keyframes.size() - 1)._time);
    update(0.0d, null);
    getCurrent(end);

    swapKeyframeSets();
    _curTime = 0;
    _curFrame = 0;
    setMinTime(0);
    setMaxTime(translationLen);
    setKeyframe(0, begin);
    setKeyframe(translationLen, end);
    _isSmooth = true;
    _tempNewBeginTime = newBeginTime;
    _tempNewEndTime = newEndTime;
  }

  /**
   * Swaps prevKeyframes and keyframes
   */
  private void swapKeyframeSets() {
    final ArrayList<PointInTime> temp = _keyframes;
    _keyframes = _prevKeyframes;
    _prevKeyframes = temp;
  }

  /**
   * Sets the new animation boundaries for this controller. This will start at newBeginTime and
   * proceed in the direction of newEndTime (either forwards or backwards). If both are the same, then
   * the animation is set to their time and turned off, otherwise the animation is turned on to start
   * the animation acording to the repeat type. If either BeginTime or EndTime are invalid times (less
   * than 0 or greater than the maximum set keyframe time) then a warning is set and nothing happens.
   * <br>
   * It is suggested that this function be called if new animation boundaries need to be set, instead
   * of setMinTime and setMaxTime directly.
   *
   * @param newBeginTime
   *          The starting time
   * @param newEndTime
   *          The ending time
   */
  public void setNewAnimationTimes(final double newBeginTime, final double newEndTime) {
    if (_isSmooth) {
      return;
    }
    if (newBeginTime < 0 || newBeginTime > _keyframes.get(_keyframes.size() - 1)._time) {
      KeyframeController.logger.warning("Attempt to set invalid begintime:" + newBeginTime);
      return;
    }
    if (newEndTime < 0 || newEndTime > _keyframes.get(_keyframes.size() - 1)._time) {
      KeyframeController.logger.warning("Attempt to set invalid endtime:" + newEndTime);
      return;
    }
    setMinTime(newBeginTime);
    setMaxTime(newEndTime);
    setActive(true);
    if (newBeginTime <= newEndTime) { // Moving forward
      _movingForward = true;
      _curTime = newBeginTime;
      if (newBeginTime == newEndTime) {
        update(0.0d, null);
        setActive(false);
      }
    } else { // Moving backwards
      _movingForward = false;
      _curTime = newEndTime;
    }
  }

  /**
   * Saves whatever the current morphMesh looks like into the dataCopy
   *
   * @param dataCopy
   *          The copy to save the current mesh into
   */
  private void getCurrent(final Mesh dataCopy) {
    final MeshData srcMeshData = _morphMesh.getMeshData();
    final MeshData dstMeshData = dataCopy.getMeshData();
    if (srcMeshData.getColorBuffer() != null) {
      FloatBuffer dstColors = dstMeshData.getColorBuffer();
      if (dstColors != null) {
        dstColors.clear();
      }
      final FloatBuffer srcColors = srcMeshData.getColorBuffer();
      srcColors.clear();
      if (dstColors == null || dstColors.capacity() != srcColors.capacity()) {
        dstColors = BufferUtils.createFloatBuffer(srcColors.capacity());
        dstColors.clear();
        dstMeshData.setColorBuffer(dstColors);
      }

      dstColors.put(srcColors);
      dstColors.flip();
      dstMeshData.markBufferDirty(MeshData.KEY_ColorCoords);
    }
    if (srcMeshData.getVertexBuffer() != null) {
      FloatBuffer dstVerts = dstMeshData.getVertexBuffer();
      if (dstVerts != null) {
        dstVerts.clear();
      }
      final FloatBuffer srcVerts = srcMeshData.getVertexBuffer();
      srcVerts.clear();
      if (dstVerts == null || dstVerts.capacity() != srcVerts.capacity()) {
        dstVerts = BufferUtils.createFloatBuffer(srcVerts.capacity());
        dstVerts.clear();
        dstMeshData.setVertexBuffer(dstVerts);
      }

      dstVerts.put(srcVerts);
      dstVerts.flip();
      dstMeshData.markBufferDirty(MeshData.KEY_VertexCoords);
    }
    if (srcMeshData.getNormalBuffer() != null) {
      FloatBuffer dstNorms = dstMeshData.getNormalBuffer();
      if (dstNorms != null) {
        dstNorms.clear();
      }
      final FloatBuffer srcNorms = srcMeshData.getNormalBuffer();
      srcNorms.clear();
      if (dstNorms == null || dstNorms.capacity() != srcNorms.capacity()) {
        dstNorms = BufferUtils.createFloatBuffer(srcNorms.capacity());
        dstNorms.clear();
        dstMeshData.setNormalBuffer(dstNorms);
      }

      dstNorms.put(srcNorms);
      dstNorms.flip();
      dstMeshData.markBufferDirty(MeshData.KEY_NormalCoords);
    }
    if (srcMeshData.getIndices() != null) {
      IndexBufferData<?> dstInds = dstMeshData.getIndices();
      if (dstInds != null) {
        dstInds.rewind();
      }
      final IndexBufferData<?> srcInds = srcMeshData.getIndices();
      srcInds.clear();
      if (dstInds == null || dstInds.capacity() != srcInds.capacity() || dstInds.getClass() != srcInds.getClass()) {
        dstInds = BufferUtils.createIndexBufferData(srcInds.capacity(), srcMeshData.getVertexBuffer().capacity() - 1);
        dstInds.clear();
        dstMeshData.setIndices(dstInds);
      }

      dstInds.put(srcInds);
      dstInds.flip();
      dstMeshData.markIndicesDirty();

    }
    if (srcMeshData.getTextureCoords(0) != null) {
      FloatBuffer dstTexs = dstMeshData.getTextureCoords(0).getBuffer();
      if (dstTexs != null) {
        dstTexs.clear();
      }
      final FloatBuffer srcTexs = srcMeshData.getTextureCoords(0).getBuffer();
      srcTexs.clear();
      if (dstTexs == null || dstTexs.capacity() != srcTexs.capacity()) {
        dstTexs = BufferUtils.createFloatBuffer(srcTexs.capacity());
        dstTexs.clear();
        dstMeshData.setTextureCoords(new FloatBufferData(dstTexs, 2), 0);
      }

      dstTexs.put(srcTexs);
      dstTexs.flip();
      dstMeshData.markBufferDirty(MeshData.KEY_TextureCoords0);
    }
  }

  /**
   * As defined in Controller
   *
   * @param time
   *          as defined in Controller
   */
  @Override
  public void update(final double time, final T caller) {
    if (easyQuit()) {
      return;
    }
    if (_movingForward) {
      _curTime += time * getSpeed();
    } else {
      _curTime -= time * getSpeed();
    }

    findFrame();
    _before = _keyframes.get(_curFrame);
    // Change this bit so the next frame we're heading towards isn't always going
    // to be one frame ahead since now we could be animating from the last to first
    // frames.
    // after = keyframes.get(curFrame + 1));
    _after = _keyframes.get(_nextFrame);

    final double localMinTime = Math.min(_before._time, _after._time);
    final double localMaxTime = Math.max(_before._time, _after._time);
    final double clampedCurTime = Math.max(localMinTime, Math.min(_curTime, localMaxTime));
    final double delta;

    // If we doing that wrapping bit then delta should be calculated based
    // on the time before the start of the animation we are.
    if (_nextFrame < _curFrame) {
      delta = blendTime - (getMinTime() - clampedCurTime);
    } else {
      // general case
      delta = (clampedCurTime - _before._time) / (_after._time - _before._time);
    }

    final Mesh oldShape = _before._newShape;
    final Mesh newShape = _after._newShape;

    final FloatBuffer verts = _morphMesh.getMeshData().getVertexBuffer();
    final FloatBuffer norms = _morphMesh.getMeshData().getNormalBuffer();
    final FloatBuffer texts = _interpTex ? _morphMesh.getMeshData().getTextureCoords(0) != null
        ? _morphMesh.getMeshData().getTextureCoords(0).getBuffer()
        : null : null;
    final FloatBuffer colors = _morphMesh.getMeshData().getColorBuffer();

    final FloatBuffer oldverts = oldShape.getMeshData().getVertexBuffer();
    final FloatBuffer oldnorms = oldShape.getMeshData().getNormalBuffer();
    final FloatBuffer oldtexts = _interpTex
        ? oldShape.getMeshData().getTextureCoords(0) != null ? oldShape.getMeshData().getTextureCoords(0).getBuffer()
            : null
        : null;
    final FloatBuffer oldcolors = oldShape.getMeshData().getColorBuffer();

    final FloatBuffer newverts = newShape.getMeshData().getVertexBuffer();
    final FloatBuffer newnorms = newShape.getMeshData().getNormalBuffer();
    final FloatBuffer newtexts = _interpTex
        ? newShape.getMeshData().getTextureCoords(0) != null ? newShape.getMeshData().getTextureCoords(0).getBuffer()
            : null
        : null;
    final FloatBuffer newcolors = newShape.getMeshData().getColorBuffer();
    if (verts == null || oldverts == null || newverts == null) {
      return;
    }
    final int vertQuantity = verts.capacity() / 3;
    verts.rewind();
    oldverts.rewind();
    newverts.rewind();

    if (norms != null) {
      norms.rewind(); // reset to start
    }
    if (oldnorms != null) {
      oldnorms.rewind(); // reset to start
    }
    if (newnorms != null) {
      newnorms.rewind(); // reset to start
    }

    if (texts != null) {
      texts.rewind(); // reset to start
    }
    if (oldtexts != null) {
      oldtexts.rewind(); // reset to start
    }
    if (newtexts != null) {
      newtexts.rewind(); // reset to start
    }

    if (colors != null) {
      colors.rewind(); // reset to start
    }
    if (oldcolors != null) {
      oldcolors.rewind(); // reset to start
    }
    if (newcolors != null) {
      newcolors.rewind(); // reset to start
    }

    final ReadOnlyTransform oldtransform = oldShape.getTransform();
    final ReadOnlyTransform newtransform = newShape.getTransform();
    if (!oldtransform.isIdentity() || !newtransform.isIdentity()) {
      final Vector3 trOldverts = new Vector3();
      final Vector3 trNewverts = new Vector3();
      for (int i = 0; i < vertQuantity; i++) {
        for (int x = 0; x < 3; x++) {
          trOldverts.setValue(x, oldverts.get(i * 3 + x));
          trNewverts.setValue(x, newverts.get(i * 3 + x));
        }
        oldtransform.applyForward(trOldverts);
        newtransform.applyForward(trNewverts);
        for (int x = 0; x < 3; x++) {
          verts.put(i * 3 + x, (float) ((1f - delta) * trOldverts.getValue(x) + delta * trNewverts.getValue(x)));
        }

      }
      _morphMesh.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    } else {
      for (int i = 0; i < vertQuantity; i++) {
        for (int x = 0; x < 3; x++) {
          verts.put(i * 3 + x, (float) ((1f - delta) * oldverts.get(i * 3 + x) + delta * newverts.get(i * 3 + x)));
        }
      }
      _morphMesh.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    }

    for (int i = 0; i < vertQuantity; i++) {
      if (norms != null && oldnorms != null && newnorms != null) {
        for (int x = 0; x < 3; x++) {
          norms.put(i * 3 + x, (float) ((1f - delta) * oldnorms.get(i * 3 + x) + delta * newnorms.get(i * 3 + x)));
        }
        _morphMesh.getMeshData().markBufferDirty(MeshData.KEY_NormalCoords);
      }

      if (_interpTex && texts != null && oldtexts != null && newtexts != null) {
        for (int x = 0; x < 2; x++) {
          texts.put(i * 2 + x, (float) ((1f - delta) * oldtexts.get(i * 2 + x) + delta * newtexts.get(i * 2 + x)));
        }
        _morphMesh.getMeshData().markBufferDirty(MeshData.KEY_TextureCoords0);
      }

      if (colors != null && oldcolors != null && newcolors != null) {
        for (int x = 0; x < 4; x++) {
          colors.put(i * 4 + x, (float) ((1f - delta) * oldcolors.get(i * 4 + x) + delta * newcolors.get(i * 4 + x)));
        }
        _morphMesh.getMeshData().markBufferDirty(MeshData.KEY_ColorCoords);
      }
    }

    if (_updateBounding) {
      updateBounding();
    }
  }

  /**
   * Updates the bounding volume of the morph mesh
   */
  protected void updateBounding() {
    _morphMesh.updateModelBound();
  }

  /**
   * If both min and max time are equal and the model is already updated, then it's an easy quit, or
   * if it's on CLAMP and I've exceeded my time it's also an easy quit.
   *
   * @return true if update doesn't need to be called, false otherwise
   */
  private boolean easyQuit() {
    if (getMaxTime() == getMinTime() && _curTime != getMinTime()) {
      return true;
    } else if (getRepeatType() == ComplexSpatialController.RepeatType.CLAMP
        && (_curTime > getMaxTime() || _curTime < getMinTime())) {
      return true;
    } else if (_keyframes.size() < 2) {
      return true;
    }
    return false;
  }

  /**
   * If true, the model's bounding volume will be updated every frame. If false, it will not.
   *
   * @param update
   *          The new update model volume per frame value.
   */
  public void setUpdateBounding(final boolean update) { _updateBounding = update; }

  /**
   * Returns true if the model's bounding volume is being updated every frame.
   *
   * @return True if bounding volume is updating.
   */
  public boolean isUpdateBounding() { return _updateBounding; }

  public void setInterpTex(final boolean interpTex) { _interpTex = interpTex; }

  public boolean isInterpTex() { return _interpTex; }

  private float blendTime = 0;

  /**
   * If repeat type RT_WRAP is set, after reaching the last frame of the currently set animation
   * maxTime (see Controller.setMaxTime), there will be an additional blendTime seconds long phase
   * inserted, morphing from the last frame to the first.
   *
   * @param blendTime
   *          The blend time to set
   */
  public void setBlendTime(final float blendTime) { this.blendTime = blendTime; }

  /**
   * Gets the currently set blending time for smooth animation transitions
   *
   * @return The current blend time
   * @see #setBlendTime(float blendTime)
   */
  public float getBlendTime() { return blendTime; }

  /**
   * This is used by update(float). It calculates PointInTime before and after as well as makes
   * adjustments on what to do when curTime is beyond the MinTime and MaxTime bounds
   */
  private void findFrame() {
    // If we're in our special wrapping case then just ignore changing
    // frames. Once we get back into the actual series we'll revert back
    // to the normal process
    if (_curTime < getMinTime() && _nextFrame < _curFrame) {
      return;
    }

    // Update the rest to maintain our new nextFrame marker as one infront
    // of the curFrame in all cases. The wrap case is where the real work
    // is done.
    if (_curTime > getMaxTime()) {
      if (_isSmooth) {
        swapKeyframeSets();
        _isSmooth = false;
        _curTime = _tempNewBeginTime;
        _curFrame = 0;
        _nextFrame = 1;
        setNewAnimationTimes(_tempNewBeginTime, _tempNewEndTime);
        return;
      }
      if (getRepeatType() == ComplexSpatialController.RepeatType.WRAP) {
        final float delta = blendTime;
        _curTime = getMinTime() - delta;
        _curFrame = Math.min(_curFrame + 1, _keyframes.size() - 1);

        for (_nextFrame = 0; _nextFrame < _keyframes.size() - 1; _nextFrame++) {
          if (getMinTime() <= _keyframes.get(_nextFrame)._time) {
            break;
          }
        }
        return;
      } else if (getRepeatType() == ComplexSpatialController.RepeatType.CLAMP) {
        return;
      } else { // Then assume it's RT_CYCLE
        _movingForward = false;
        _curTime = getMaxTime();
      }
    } else if (_curTime < getMinTime()) {
      if (getRepeatType() == ComplexSpatialController.RepeatType.WRAP) {
        _curTime = getMaxTime();
        _curFrame = 0;
      } else if (getRepeatType() == ComplexSpatialController.RepeatType.CLAMP) {
        return;
      } else { // Then assume it's RT_CYCLE
        _movingForward = true;
        _curTime = getMinTime();
      }
    }

    _nextFrame = _curFrame + 1;

    if (_curTime > _keyframes.get(_curFrame)._time) {
      if (_curTime < _keyframes.get(_curFrame + 1)._time) {
        _nextFrame = _curFrame + 1;
        return;
      }

      for (; _curFrame < _keyframes.size() - 1; _curFrame++) {
        if (_curTime <= _keyframes.get(_curFrame + 1)._time) {
          _nextFrame = _curFrame + 1;
          return;
        }
      }

      // This -should- be unreachable because of the above
      _curTime = getMinTime();
      _curFrame = 0;
      _nextFrame = _curFrame + 1;
      return;
    }

    for (; _curFrame >= 0; _curFrame--) {
      if (_curTime >= _keyframes.get(_curFrame)._time) {
        _nextFrame = _curFrame + 1;
        return;
      }
    }

    // This should be unreachable because curTime>=0 and
    // keyframes[0].time=0;
    _curFrame = 0;
    _nextFrame = _curFrame + 1;
  }

  /**
   * This class defines a point in time that states _morphShape should look like _newShape at _time
   * seconds
   */
  public static class PointInTime implements Savable {

    public Mesh _newShape;

    public double _time;

    public PointInTime() {}

    public PointInTime(final double time, final Mesh shape) {
      _time = time;
      _newShape = shape;
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
      _time = capsule.readDouble("time", 0);
      _newShape = capsule.readSavable("newShape", null);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
      capsule.write(_time, "time", 0);
      capsule.write(_newShape, "newShape", null);
    }

    @Override
    public Class<?> getClassTag() { return this.getClass(); }
  }

  @SuppressWarnings("unchecked")
  private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    _keyframes = (ArrayList<PointInTime>) in.readObject();
    _movingForward = true;
  }

  public Mesh getMorphMesh() { return _morphMesh; }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<? extends KeyframeController> getClassTag() { return this.getClass(); }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _updateBounding = capsule.readBoolean("updateBounding", false);
    _morphMesh = capsule.readSavable("morphMesh", null);
    _keyframes = (ArrayList<PointInTime>) capsule.readSavableList("keyframes", new ArrayList<PointInTime>());
    _movingForward = true;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_updateBounding, "updateBounding", true);
    capsule.write(_morphMesh, "morphMesh", null);
    capsule.writeSavableList(_keyframes, "keyframes", new ArrayList<PointInTime>());
  }

}
