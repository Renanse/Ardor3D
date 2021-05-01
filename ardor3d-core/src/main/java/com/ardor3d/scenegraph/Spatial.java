/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.ValidatingTransform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateStack;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.event.DirtyEventListener;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.Hintable;
import com.ardor3d.scenegraph.hint.PropertyMode;
import com.ardor3d.scenegraph.hint.SceneHints;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Base class for all scenegraph objects.
 */
public abstract class Spatial implements Savable, Hintable {

  private static final Logger logger = Logger.getLogger(Spatial.class.getName());

  /** This spatial's name. */
  protected String _name;

  /** Spatial's transform relative to its parent. */
  protected final Transform _localTransform;

  /** Spatial's absolute transform. */
  protected final Transform _worldTransform;

  /** Spatial's world bounding volume. */
  protected BoundingVolume _worldBound;

  /** Spatial's parent, or null if it has none. */
  protected Node _parent;

  /** ArrayList of controllers for this spatial. */
  protected List<SpatialController<?>> _controllers;

  /** The local render states of this spatial. */
  protected EnumMap<RenderState.StateType, RenderState> _renderStateList = new EnumMap<>(RenderState.StateType.class);

  /** Listeners for dirty events. */
  protected List<DirtyEventListener> _listeners;

  /** Field for accumulating dirty marks. */
  protected EnumSet<DirtyType> _dirtyMark = EnumSet.of(DirtyType.Bounding, DirtyType.RenderState, DirtyType.Transform);

  /** User supplied properties. */
  protected Map<String, Object> _properties = new HashMap<>();

  /** Keeps track of the current frustum intersection state of this Spatial. */
  protected Camera.FrustumIntersect _frustumIntersects = Camera.FrustumIntersect.Intersects;

  /** The hints for Ardor3D's use when evaluating and rendering this spatial. */
  protected SceneHints _sceneHints;

  /** The local RenderMaterial used by this spatial. Inherited by children if they have none set. */
  protected RenderMaterial _material;

  /**
   * The local RenderMaterial used by this spatial for light occluder rendering. Inherited by children
   * if they have none set.
   */
  protected RenderMaterial _occluderMaterial;

  /** This spatial's layer; defaults to {@link #LAYER_DEFAULT}. */
  protected int _layer = Spatial.LAYER_INHERIT;

  public transient double _queueDistance = Double.NEGATIVE_INFINITY;

  protected static final EnumSet<DirtyType> ON_DIRTY_TRANSFORM_ONLY = EnumSet.of(DirtyType.Transform);
  protected static final EnumSet<DirtyType> ON_DIRTY_TRANSFORM = EnumSet.of(DirtyType.Bounding, DirtyType.Transform);
  protected static final EnumSet<DirtyType> ON_DIRTY_RENDERSTATE = EnumSet.of(DirtyType.RenderState);
  protected static final EnumSet<DirtyType> ON_DIRTY_BOUNDING = EnumSet.of(DirtyType.Bounding);
  protected static final EnumSet<DirtyType> ON_DIRTY_ATTACHED =
      EnumSet.of(DirtyType.Transform, DirtyType.RenderState, DirtyType.Bounding);

  public static final String KEY_UserData = "_userData";
  public static final String KEY_DefaultColor = "_defaultColor";

  /** Use our parent layer. If no parent, we will fall back to {@link #LAYER_DEFAULT}. */
  public static final int LAYER_INHERIT = -1;

  /** Default layer used by spatials. Most Spatials will be in this layer. */
  public static final int LAYER_DEFAULT = 0;

  /** Layer for use by UI elements. */
  public static final int LAYER_UI = 1;

  /** User layers should start at this value. */
  public static final int LAYER_FIRST_USER_DEFINED = 10;

  /**
   * Constructs a new Spatial. Initializes the transform fields.
   */
  public Spatial() {
    _localTransform = Constants.useValidatingTransform ? new ValidatingTransform() : new Transform();
    _worldTransform = Constants.useValidatingTransform ? new ValidatingTransform() : new Transform();
    _sceneHints = new SceneHints(this);
  }

  /**
   * Constructs a new <code>Spatial</code> with a given name.
   *
   * @param name
   *          the name of the spatial. This is required for identification purposes.
   */
  public Spatial(final String name) {
    this();
    _name = name;
  }

  /**
   * Returns the name of this spatial.
   *
   * @return This spatial's name.
   */
  public String getName() { return _name; }

  /**
   * Sets the name of this Spatial.
   *
   * @param name
   *          new name
   */
  public void setName(final String name) { _name = name; }

  /**
   * @return this Spatial's layer, used for render filtering. Defaults to {@link #LAYER_INHERIT}
   */
  public int getLayer() {
    if (_layer == Spatial.LAYER_INHERIT) {
      if (_parent != null) {
        return _parent.getLayer();
      }
      return Spatial.LAYER_DEFAULT;
    }
    return _layer;
  }

  /**
   * Sets the layer of this Spatial.
   *
   * @param layer
   *          the new layer value
   */
  public void setLayer(final int layer) { _layer = layer; }

  /**
   * <code>getParent</code> retrieve's this node's parent. If the parent is null this is the root
   * node.
   *
   * @return the parent of this node.
   */
  public Node getParent() { return _parent; }

  /**
   * Called by {@link Node#attachChild(Spatial)} and {@link Node#detachChild(Spatial)} - don't call
   * directly. <code>setParent</code> sets the parent of this node.
   *
   * @param parent
   *          the parent of this node.
   */
  protected void setParent(final Node parent) { _parent = parent; }

  /**
   * <code>removeFromParent</code> removes this Spatial from it's parent.
   *
   * @return true if it has a parent and performed the remove.
   */
  public boolean removeFromParent() {
    if (_parent != null) {
      _parent.detachChild(this);
      return true;
    }
    return false;
  }

  /**
   * determines if the provided Node is the parent, or parent's parent, etc. of this Spatial.
   *
   * @param ancestor
   *          the ancestor object to look for.
   * @return true if the ancestor is found, false otherwise.
   */
  public boolean hasAncestor(final Node ancestor) {
    if (_parent == null) {
      return false;
    } else if (_parent.equals(ancestor)) {
      return true;
    } else {
      return _parent.hasAncestor(ancestor);
    }
  }

  /**
   * @see Hintable#getParentHintable()
   */
  @Override
  public Hintable getParentHintable() { return _parent; }

  /**
   * Gets the scene hints.
   *
   * @return the scene hints set on this Spatial
   */
  @Override
  public SceneHints getSceneHints() { return _sceneHints; }

  /**
   * Add the listener for dirty events on this node.
   *
   * @param listener
   *          listener to add.
   */
  public void addListener(final DirtyEventListener listener) {
    if (_listeners == null) {
      _listeners = new ArrayList<>();
    }
    _listeners.add(listener);
  }

  /**
   * Removes the listener for dirty events on this node.
   *
   * @param listener
   *          listener to use.
   * @return true if the given listener was found
   */
  public boolean removeListener(final DirtyEventListener listener) {
    if (_listeners == null) {
      return false;
    }
    return _listeners.remove(listener);
  }

  /**
   * Mark this node as dirty. Can be marked as Transform, Bounding, Attached, Detached, Destroyed or
   * RenderState
   *
   * @param dirtyType
   *          the dirty type
   */
  public void markDirty(final DirtyType dirtyType) {
    markDirty(this, dirtyType);
  }

  /**
   * Mark this node as dirty. Can be marked as Transform, Bounding, Attached, Detached, Destroyed or
   * RenderState
   *
   * @param caller
   *          the spatial where the marking was initiated
   * @param dirtyType
   *          the dirty type
   */
  protected void markDirty(final Spatial caller, final DirtyType dirtyType) {
    switch (dirtyType) {
      case Transform:
        propagateDirtyDown(Spatial.ON_DIRTY_TRANSFORM);
        if (_parent != null) {
          _parent.propagateDirtyUp(Spatial.ON_DIRTY_BOUNDING);
        }
        break;
      case RenderState:
        propagateDirtyDown(Spatial.ON_DIRTY_RENDERSTATE);
        break;
      case Bounding:
        // not just _parent here, on purpose
        propagateDirtyUp(Spatial.ON_DIRTY_BOUNDING);
        break;
      case Attached:
        propagateDirtyDown(Spatial.ON_DIRTY_ATTACHED);
        if (_parent != null) {
          _parent.propagateDirtyUp(Spatial.ON_DIRTY_BOUNDING);
        }
        break;
      case Detached:
      case Destroyed:
        if (_parent != null) {
          _parent.propagateDirtyUp(Spatial.ON_DIRTY_BOUNDING);
        }
        break;
      default:
        break;
    }

    propageEventUp(caller, dirtyType, true);
  }

  /**
   * Test if this spatial is marked as dirty in respect to the supplied DirtyType.
   *
   * @param dirtyType
   *          dirty type to test against
   * @return true if spatial marked dirty against the supplied dirty type
   */
  public boolean isDirty(final DirtyType dirtyType) {
    return _dirtyMark.contains(dirtyType);
  }

  /**
   * Clears the dirty flag set at this spatial for the supplied dirty type.
   *
   * @param dirtyType
   *          dirty type to clear flag for
   */
  public void clearDirty(final DirtyType dirtyType) {
    clearDirty(this, dirtyType);
  }

  /**
   * Clears the dirty flag set at this spatial for the supplied dirty type.
   *
   * @param caller
   *          the spatial where the clearing was initiated
   * @param dirtyType
   *          dirty type to clear flag for
   */
  public void clearDirty(final Spatial caller, final DirtyType dirtyType) {
    _dirtyMark.remove(dirtyType);

    propageEventUp(caller, dirtyType, false);
  }

  /**
   * Propagate the dirty mark up the tree hierarchy.
   *
   * @param dirtyTypes
   *          the dirty types
   */
  protected void propagateDirtyUp(final EnumSet<DirtyType> dirtyTypes) {
    _dirtyMark.addAll(dirtyTypes);

    if (_parent != null) {
      _parent.propagateDirtyUp(dirtyTypes);
    }
  }

  /**
   * Propagate the dirty mark down the tree hierarchy.
   *
   * @param dirtyTypes
   *          the dirty types
   */
  protected void propagateDirtyDown(final EnumSet<DirtyType> dirtyTypes) {
    _dirtyMark.addAll(dirtyTypes);
  }

  /**
   * Propagate the dirty event up the hierarchy to any registered listeners
   *
   * @param spatial
   *          the spatial
   * @param dirtyType
   *          the dirty type
   * @param dirty
   *          true if this is a dirty event, false if clean
   */
  protected void propageEventUp(final Spatial spatial, final DirtyType dirtyType, final boolean dirty) {
    boolean consumed;
    if (_listeners != null) {
      for (final var listener : _listeners) {
        consumed = dirty ? listener.spatialDirty(spatial, dirtyType) : listener.spatialClean(spatial, dirtyType);
        if (consumed) {
          return;
        }
      }
    }

    if (_parent != null) {
      _parent.propageEventUp(spatial, dirtyType, dirty);
    }
  }

  /**
   * Gets the local rotation matrix.
   *
   * @return the rotation
   */
  public ReadOnlyMatrix3 getRotation() { return _localTransform.getMatrix(); }

  /**
   * Gets the local scale vector.
   *
   * @return the scale
   */
  public ReadOnlyVector3 getScale() { return _localTransform.getScale(); }

  /**
   * Gets the local translation vector.
   *
   * @return the translation
   */
  public ReadOnlyVector3 getTranslation() { return _localTransform.getTranslation(); }

  /**
   * Gets the local transform.
   *
   * @return the transform
   */
  public ReadOnlyTransform getTransform() { return _localTransform; }

  /**
   * Sets the local transform.
   *
   * @param transform
   *          the new transform
   */
  public void setTransform(final ReadOnlyTransform transform) {
    _localTransform.set(transform);
    markDirty(DirtyType.Transform);
  }

  /**
   * Sets the world rotation matrix.
   *
   * @param rotation
   *          the new world rotation
   */
  public void setWorldRotation(final ReadOnlyMatrix3 rotation) {
    _worldTransform.setRotation(rotation);
  }

  /**
   * Sets the world rotation quaternion.
   *
   * @param rotation
   *          the new world rotation
   */
  public void setWorldRotation(final ReadOnlyQuaternion rotation) {
    _worldTransform.setRotation(rotation);
  }

  /**
   * Sets the world scale.
   *
   * @param scale
   *          the new world scale vector
   */
  public void setWorldScale(final ReadOnlyVector3 scale) {
    _worldTransform.setScale(scale);
  }

  /**
   * Sets the world scale.
   *
   * @param x
   *          the x coordinate
   * @param y
   *          the y coordinate
   * @param z
   *          the z coordinate
   */
  public void setWorldScale(final double x, final double y, final double z) {
    _worldTransform.setScale(x, y, z);
  }

  /**
   * Sets the world scale.
   *
   * @param scale
   *          the new world scale
   */
  public void setWorldScale(final double scale) {
    _worldTransform.setScale(scale);
  }

  /**
   * Sets the world translation vector.
   *
   * @param translation
   *          the new world translation
   */
  public void setWorldTranslation(final ReadOnlyVector3 translation) {
    _worldTransform.setTranslation(translation);
  }

  /**
   * Sets the world translation.
   *
   * @param x
   *          the x coordinate
   * @param y
   *          the y coordinate
   * @param z
   *          the z coordinate
   */
  public void setWorldTranslation(final double x, final double y, final double z) {
    _worldTransform.setTranslation(x, y, z);
  }

  /**
   * Sets the world transform.
   *
   * @param transform
   *          the new world transform
   */
  public void setWorldTransform(final ReadOnlyTransform transform) {
    _worldTransform.set(transform);
  }

  /**
   * Sets the rotation of this spatial. This marks the spatial as DirtyType.Transform.
   *
   * @param rotation
   *          the new rotation of this spatial
   * @see Transform#setRotation(Matrix3)
   */
  public void setRotation(final ReadOnlyMatrix3 rotation) {
    _localTransform.setRotation(rotation);
    markDirty(DirtyType.Transform);
  }

  /**
   * Sets the rotation of this spatial. This marks the spatial as DirtyType.Transform.
   *
   * @param rotation
   *          the new rotation of this spatial
   * @see Transform#setRotation(Quaternion)
   */
  public void setRotation(final ReadOnlyQuaternion rotation) {
    _localTransform.setRotation(rotation);
    markDirty(DirtyType.Transform);
  }

  /**
   * <code>setScale</code> sets the scale of this spatial. This marks the spatial as
   * DirtyType.Transform.
   *
   * @param scale
   *          the new scale of this spatial
   */
  public void setScale(final ReadOnlyVector3 scale) {
    _localTransform.setScale(scale);
    markDirty(DirtyType.Transform);
  }

  /**
   * <code>setScale</code> sets the scale of this spatial. This marks the spatial as
   * DirtyType.Transform.
   *
   * @param scale
   *          the new scale of this spatial
   */
  public void setScale(final double scale) {
    _localTransform.setScale(scale);
    markDirty(DirtyType.Transform);
  }

  /**
   * sets the scale of this spatial. This marks the spatial as DirtyType.Transform.
   *
   * @param x
   *          the x scale factor
   * @param y
   *          the y scale factor
   * @param z
   *          the z scale factor
   */
  public void setScale(final double x, final double y, final double z) {
    _localTransform.setScale(x, y, z);
    markDirty(DirtyType.Transform);
  }

  /**
   * <code>setTranslation</code> sets the translation of this spatial. This marks the spatial as
   * DirtyType.Transform.
   *
   * @param translation
   *          the new translation of this spatial
   */
  public void setTranslation(final ReadOnlyVector3 translation) {
    _localTransform.setTranslation(translation);
    markDirty(DirtyType.Transform);
  }

  /**
   * sets the translation of this spatial. This marks the spatial as DirtyType.Transform.
   *
   * @param x
   *          the x coordinate
   * @param y
   *          the y coordinate
   * @param z
   *          the z coordinate
   */
  public void setTranslation(final double x, final double y, final double z) {
    _localTransform.setTranslation(x, y, z);
    markDirty(DirtyType.Transform);
  }

  /**
   * <code>addTranslation</code> adds the given translation to the translation of this spatial. This
   * marks the spatial as DirtyType.Transform.
   *
   * @param translation
   *          the translation vector
   */
  public void addTranslation(final ReadOnlyVector3 translation) {
    addTranslation(translation.getX(), translation.getY(), translation.getZ());
  }

  /**
   * adds to the current translation of this spatial. This marks the spatial as DirtyType.Transform.
   *
   * @param x
   *          the x amount
   * @param y
   *          the y amount
   * @param z
   *          the z amount
   */
  public void addTranslation(final double x, final double y, final double z) {
    _localTransform.translate(x, y, z);
    markDirty(DirtyType.Transform);
  }

  public void lookAt(final double x, final double y, final double z) {
    lookAt(x, y, z, Vector3.UNIT_Y);
  }

  public void lookAt(final double x, final double y, final double z, final ReadOnlyVector3 worldUp) {
    final var lookDir = Vector3.fetchTempInstance();
    final var rot = Matrix3.fetchTempInstance();
    try {
      // create vector to look at point
      lookDir.set(x, y, z).subtractLocal(getWorldTranslation());

      // create and apply rotational matrix
      rot.lookAt(lookDir, worldUp);
      setRotation(rot);
    } finally {
      Vector3.releaseTempInstance(lookDir);
      Matrix3.releaseTempInstance(rot);
    }
    markDirty(DirtyType.Transform);
  }

  /**
   * Gets the world rotation matrix.
   *
   * @return the world rotation
   */
  public ReadOnlyMatrix3 getWorldRotation() { return _worldTransform.getMatrix(); }

  /**
   * Gets the world scale vector.
   *
   * @return the world scale
   */
  public ReadOnlyVector3 getWorldScale() { return _worldTransform.getScale(); }

  /**
   * Gets the world translation vector.
   *
   * @return the world translation
   */
  public ReadOnlyVector3 getWorldTranslation() { return _worldTransform.getTranslation(); }

  /**
   * Gets the world transform.
   *
   * @return the world transform
   */
  public ReadOnlyTransform getWorldTransform() { return _worldTransform; }

  /**
   * <code>getWorldBound</code> retrieves the world bound at this level.
   *
   * @return the world bound at this level.
   */
  public BoundingVolume getWorldBound() { return _worldBound; }

  /**
   * <code>onDraw</code> checks the spatial with the camera to see if it should be culled, if not, the
   * node's draw method is called.
   * <p>
   * This method is called by the renderer. Usually it should not be called directly.
   *
   * @param r
   *          the renderer used for display.
   */
  public void onDraw(final Renderer r) {
    final CullHint cm = _sceneHints.getCullHint();
    if (cm == CullHint.Always) {
      setLastFrustumIntersection(Camera.FrustumIntersect.Outside);
      return;
    } else if (cm == CullHint.Never) {
      setLastFrustumIntersection(Camera.FrustumIntersect.Intersects);
      draw(r);
      return;
    }

    final Camera camera = Camera.getCurrentCamera();
    final int state = camera.getPlaneState();

    // check to see if we can cull this node
    _frustumIntersects = ((_parent != null && _parent.getWorldBound() != null) ? _parent._frustumIntersects
        : Camera.FrustumIntersect.Intersects);

    if (cm == CullHint.Dynamic && _frustumIntersects == Camera.FrustumIntersect.Intersects) {
      _frustumIntersects = camera.contains(_worldBound);
    }

    if (_frustumIntersects != Camera.FrustumIntersect.Outside) {
      draw(r);
    }
    camera.setPlaneState(state);
  }

  /**
   * <code>draw</code> abstract method that handles drawing data to the renderer if it is geometry and
   * passing the call to its children if it is a node.
   *
   * @param renderer
   *          the renderer used for display.
   */
  public abstract void draw(final Renderer renderer);

  /**
   * Update geometric state.
   *
   * @param time
   *          The time in seconds between the last two consecutive frames (time per frame). See
   *          {@link ReadOnlyTimer#getTimePerFrame()}
   * @see #updateGeometricState(double, boolean)
   */
  public void updateGeometricState(final double time) {
    updateGeometricState(time, true);
  }

  /**
   * <code>updateGeometricState</code> updates all the geometry information for the node.
   *
   * @param time
   *          The time in seconds between the last two consecutive frames (time per frame). See
   *          {@link ReadOnlyTimer#getTimePerFrame()}
   * @param initiator
   *          true if this node started the update process.
   */
  public void updateGeometricState(final double time, final boolean initiator) {
    updateControllers(time);

    if (_dirtyMark.isEmpty()) {
      updateChildren(time);
    } else {
      if (isDirty(DirtyType.Transform)) {
        updateWorldTransform(false);
      }

      if (isDirty(DirtyType.RenderState)) {
        updateWorldRenderStates(false);
        clearDirty(DirtyType.RenderState);
      }

      updateChildren(time);

      if (isDirty(DirtyType.Bounding)) {
        updateWorldBound(false);
        if (initiator) {
          propagateBoundToRoot();
        }
      }
    }
  }

  /**
   * Override to allow objects like Node to update their children.
   *
   * @param time
   *          The time in seconds between the last two consecutive frames (time per frame). See
   *          {@link ReadOnlyTimer#getTimePerFrame()}
   */
  protected void updateChildren(final double time) {}

  /**
   * Update all controllers set on this spatial.
   *
   * @param time
   *          The time in seconds between the last two consecutive frames (time per frame). See
   *          {@link ReadOnlyTimer#getTimePerFrame()}
   */
  @SuppressWarnings("unchecked")
  public void updateControllers(final double time) {
    if (_controllers != null) {
      for (int i = 0, gSize = _controllers.size(); i < gSize; i++) {
        try {
          final SpatialController<Spatial> controller = (SpatialController<Spatial>) _controllers.get(i);
          if (controller != null) {
            controller.update(time, this);
          }
        } catch (final IndexOutOfBoundsException e) {
          // a controller was removed in SpatialController.update (note: this
          // may skip one controller)
          break;
        }
      }
    }
  }

  /**
   * Updates the worldTransform.
   *
   * @param recurse
   *          usually false when updating the tree. Set to true when you just want to update the world
   *          transforms for a branch without updating the full geometric state.
   */
  public void updateWorldTransform(final boolean recurse) {
    if (_parent != null) {
      _parent._worldTransform.multiply(_localTransform, _worldTransform);
    } else {
      _worldTransform.set(_localTransform);
    }
    clearDirty(DirtyType.Transform);
  }

  /**
   * Updates the world material.
   *
   * @param recurse
   *          usually false when updating the tree. Set to true when you just want to update the world
   *          materials for a branch without updating the full geometric state.
   */
  public void updateWorldRenderMaterial(final boolean recurse) {
    if (_parent != null) {
      _parent._worldTransform.multiply(_localTransform, _worldTransform);
    } else {
      _worldTransform.set(_localTransform);
    }
    clearDirty(DirtyType.Transform);
  }

  /**
   * Convert a vector (in) from this spatial's local coordinate space to world coordinate space.
   *
   * @param in
   *          vector to read from
   * @param store
   *          where to write the result (null to create a new vector, may be same as in)
   * @return the result (store)
   */
  public Vector3 localToWorld(final ReadOnlyVector3 in, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }

    return _worldTransform.applyForward(in, store);
  }

  /**
   * Convert a vector (in) from world coordinate space to this spatial's local coordinate space.
   *
   * @param in
   *          vector to read from
   * @param store
   *          where to write the result (null to create a new vector, may be same as in)
   * @return the result (store)
   */
  public Vector3 worldToLocal(final ReadOnlyVector3 in, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }

    return _worldTransform.applyInverse(in, store);
  }

  /**
   * Sets the local render material.
   *
   * @param material
   *          the new material
   */
  public void setRenderMaterial(final RenderMaterial material) { _material = material; }

  /**
   * Sets the local render material.
   *
   * @param materialUrl
   *          the url of a Material to look up
   */
  public void setRenderMaterial(final String materialUrl) {
    _material = MaterialManager.INSTANCE.findMaterial(materialUrl);
  }

  /**
   * Gets the locally set render material.
   *
   * @return the material, or null if none set.
   */
  public RenderMaterial getRenderMaterial() { return _material; }

  /**
   * @return our locally set RenderMaterial, or the closest locally set material on a scene graph
   *         ancestor.
   */
  public RenderMaterial getWorldRenderMaterial() {
    return _material != null ? _material : _parent != null ? _parent.getWorldRenderMaterial() : null;
  }

  /**
   * Sets the local render material used when rendering this spatial as a light occluder.
   *
   * @param material
   *          the new material
   */
  public void setOccluderMaterial(final RenderMaterial material) { _occluderMaterial = material; }

  /**
   * Gets the locally set render material used when rendering this spatial as a light occluder.
   *
   * @return the material, or null if none set.
   */
  public RenderMaterial getOccluderMaterial() { return _occluderMaterial; }

  /**
   * @return our locally set RenderMaterial used when rendering this spatial as a light occluder, or
   *         the closest locally set occluder material on a scene graph ancestor.
   */
  public RenderMaterial getWorldOccluderMaterial() {
    return _occluderMaterial != null ? _occluderMaterial : _parent != null ? _parent.getWorldOccluderMaterial() : null;
  }

  /**
   * Updates the render state values of this Spatial and and children it has. Should be called
   * whenever render states change.
   *
   * @param recurse
   *          true to recurse down the scenegraph tree
   */
  public void updateWorldRenderStates(final boolean recurse) {
    updateWorldRenderStates(recurse, null);
  }

  /**
   * Called internally. Updates the render states of this Spatial. The stack contains parent render
   * states.
   *
   * @param recurse
   *          true to recurse down the scenegraph tree
   * @param stateStack
   *          The parent render states, or null if we are starting at this point in the scenegraph.
   */
  protected void updateWorldRenderStates(final boolean recurse, final RenderState.StateStack stateStack) {
    if (stateStack == null) {
      // grab all states from root to here.
      final RenderState.StateStack stack = RenderState.StateStack.fetchTempInstance();
      propagateStatesFromRoot(stack);

      applyWorldRenderStates(recurse, stack);

      RenderState.StateStack.releaseTempInstance(stack);
    } else {
      for (final RenderState state : _renderStateList.values()) {
        stateStack.push(state);
      }

      applyWorldRenderStates(recurse, stateStack);

      for (final RenderState state : _renderStateList.values()) {
        stateStack.pop(state);
      }
    }
  }

  /**
   * The method actually implements how the render states are applied to this spatial and (if recurse
   * is true) any children it may have. By default, this function does nothing.
   *
   * @param recurse
   *          true to recurse down the scenegraph tree
   * @param stack
   *          The stack for each state
   */
  protected void applyWorldRenderStates(final boolean recurse, final RenderState.StateStack stack) {}

  /**
   * Retrieves the complete renderstate list.
   *
   * @return the list of renderstates
   */
  public EnumMap<StateType, RenderState> getLocalRenderStates() { return _renderStateList; }

  /**
   * <code>setRenderState</code> sets a render state for this node. Note, there can only be one render
   * state per type per node. That is, there can only be a single BlendState a single TextureState,
   * etc. If there is already a render state for a type set the old render state will be returned.
   * Otherwise, null is returned.
   *
   * @param rs
   *          the render state to add.
   * @return the old render state.
   */
  public RenderState setRenderState(final RenderState rs) {
    if (rs == null) {
      return null;
    }

    final RenderState.StateType type = rs.getType();
    final RenderState oldState = _renderStateList.get(type);
    _renderStateList.put(type, rs);

    markDirty(DirtyType.RenderState);

    return oldState;
  }

  /**
   * Returns the requested RenderState that this Spatial currently has set or null if none is set.
   *
   * @param type
   *          the state type to retrieve
   * @return a render state at the given position or null
   */
  public RenderState getLocalRenderState(final RenderState.StateType type) {
    return _renderStateList.get(type);
  }

  /**
   * Clears a given render state index by setting it to null.
   *
   * @param type
   *          The type of RenderState to clear
   */
  public void clearRenderState(final RenderState.StateType type) {
    _renderStateList.remove(type);
    markDirty(DirtyType.RenderState);
  }

  /**
   * Called during updateRenderState(Stack[]), this function goes up the scene graph tree until the
   * parent is null and pushes RenderStates onto the states Stack array.
   *
   * @param stack
   *          The stack to push any parent states onto.
   */
  public void propagateStatesFromRoot(final StateStack stack) {
    // traverse to root to allow downward state propagation
    if (_parent != null) {
      _parent.propagateStatesFromRoot(stack);
    }

    // push states onto current render state stack
    for (final RenderState state : _renderStateList.values()) {
      stack.push(state);
    }
  }

  /**
   * updates the bounding volume of the world. Abstract, geometry transforms the bound while node
   * merges the children's bound. In most cases, users will want to call updateModelBound() and let
   * this function be called automatically during updateGeometricState().
   *
   * @param recurse
   *          true to recurse down the scenegraph tree
   */
  public abstract void updateWorldBound(boolean recurse);

  /**
   * passes the new world bound up the tree to the root.
   */
  public void propagateBoundToRoot() {
    if (_parent != null) {
      _parent.updateWorldBound(false);
      _parent.propagateBoundToRoot();
    }
  }

  /**
   * Get a property from this spatial, optionally inheriting values from higher in the scenegraph
   * based upon the {@link PropertyMode} set in our scene hints.
   *
   * @param key
   *          property key
   * @return the found property, cast to T
   * @throws ClassCastException
   *           if property is not correct type
   */
  public <T> T getProperty(final String key, final T defaultValue) {
    final PropertyMode mode = getSceneHints().getPropertyMode();

    if (mode == PropertyMode.UseOwn) {
      return getLocalProperty(key, defaultValue);
    }

    final Node parent = getParent();
    if (mode == PropertyMode.UseParentIfUnset) {
      if (hasLocalProperty(key)) {
        return getLocalProperty(key, defaultValue);
      }

      return parent != null ? parent.getProperty(key, defaultValue) : defaultValue;
    }

    if (mode == PropertyMode.UseOursLast) {
      if (parent.hasProperty(key)) {
        return parent.getProperty(key, defaultValue);
      }

      return getLocalProperty(key, defaultValue);
    }
    return defaultValue;
  }

  /**
   * Get a property locally set on this spatial by key. Does not check the scene or respect
   * {@link PropertyMode}.
   *
   * @param key
   *          property key
   * @param defaultValue
   *          a value to return if the given key is not found locally.
   * @return the found property, cast to T
   * @throws ClassCastException
   *           if property is not correct type
   */
  @SuppressWarnings("unchecked")
  public <T> T getLocalProperty(final String key, final T defaultValue) {
    return (T) _properties.getOrDefault(key, defaultValue);
  }

  /**
   * Set a property on this Spatial
   *
   * @param key
   *          property key
   * @param value
   *          property value
   */
  public void setProperty(final String key, final Object value) {
    _properties.put(key, value);
  }

  /**
   * Remove a property on this Spatial by a given key.
   *
   * @param key
   *          property key
   * @return the value previously set for this key, or null if the key is not found. Note that a
   *         returned value of null might also mean that null was previously set for the given key.
   */
  public Object removeProperty(final String key) {
    return _properties.remove(key);
  }

  /**
   * @param key
   *          property key
   * @return true if we locally have the given key in our property map, even if the associated value
   *         is null.
   */
  public boolean hasLocalProperty(final String key) {
    return _properties.containsKey(key);
  }

  /**
   * @param key
   *          property key
   * @return true if we have the given key somewhere reachable in the scenegraph, based on our
   *         SceneHints.
   */
  public boolean hasProperty(final String key) {
    final PropertyMode mode = getSceneHints().getPropertyMode();
    final boolean hasLocal = hasLocalProperty(key);

    if (mode == PropertyMode.UseOwn) {
      return hasLocal;
    }

    final Node parent = getParent();
    if (mode == PropertyMode.UseParentIfUnset) {
      return hasLocal || parent != null && parent.hasProperty(key);
    }

    if (mode == PropertyMode.UseOursLast) {
      final boolean parentHasProperty = parent != null && parent.hasProperty(key);
      return parentHasProperty || hasLocal;
    }

    return false;
  }

  public void setDefaultColor(final ReadOnlyColorRGBA color) {
    setDefaultColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
  }

  public void setDefaultColor(final float r, final float g, final float b, final float a) {
    final ColorRGBA store = getLocalProperty(Spatial.KEY_DefaultColor, null);
    if (store != null) {
      store.set(r, g, b, a);
    } else {
      setProperty(Spatial.KEY_DefaultColor, new ColorRGBA(r, g, b, a));
    }
  }

  public ReadOnlyColorRGBA getDefaultColor() { return getProperty(Spatial.KEY_DefaultColor, ColorRGBA.WHITE); }

  /**
   * Adds a SpatialController to this Spatial's list of controllers.
   *
   * @param controller
   *          The SpatialController to add
   * @see com.ardor3d.scenegraph.controller.SpatialController
   */
  public void addController(final SpatialController<?> controller) {
    if (_controllers == null) {
      _controllers = new ArrayList<>(1);
    }
    _controllers.add(controller);
  }

  /**
   * Removes a SpatialController from this Spatial's list of controllers, if it exist.
   *
   * @param controller
   *          The SpatialController to remove
   * @return True if the SpatialController was in the list to remove.
   * @see com.ardor3d.scenegraph.controller.SpatialController
   */
  public boolean removeController(final SpatialController<?> controller) {
    if (_controllers == null) {
      return false;
    }
    return _controllers.remove(controller);
  }

  /**
   * Removes a SpatialController from this Spatial's list of controllers by index.
   *
   * @param index
   *          The index of the controller to remove
   * @return The SpatialController removed or null if nothing was removed.
   * @see com.ardor3d.scenegraph.controller.SpatialController
   */
  public SpatialController<?> removeController(final int index) {
    if (_controllers == null) {
      return null;
    }
    return _controllers.remove(index);
  }

  /**
   * Removes all Controllers from this Spatial's list of controllers.
   *
   * @see com.ardor3d.scenegraph.controller.SpatialController
   */
  public void clearControllers() {
    if (_controllers != null) {
      _controllers.clear();
    }
  }

  /**
   * Returns the controller in this list of controllers at index i.
   *
   * @param i
   *          The index to get a controller from.
   * @return The controller at index i.
   * @see com.ardor3d.scenegraph.controller.SpatialController
   */
  public SpatialController<?> getController(final int i) {
    if (_controllers == null) {
      _controllers = new ArrayList<>(1);
    }
    return _controllers.get(i);
  }

  /**
   * Returns the ArrayList that contains this spatial's SpatialControllers.
   *
   * @return This spatial's _controllers.
   */
  public List<SpatialController<?>> getControllers() {
    if (_controllers == null) {
      _controllers = new ArrayList<>(1);
    }
    return _controllers;
  }

  /**
   * Gets the controller count.
   *
   * @return the number of controllers set on this Spatial.
   */
  public int getControllerCount() {
    if (_controllers == null) {
      return 0;
    }
    return _controllers.size();
  }

  /**
   * Returns this spatial's last frustum intersection result. This int is set when a check is made to
   * determine if the bounds of the object fall inside a camera's frustum. If a parent is found to
   * fall outside the frustum, the value for this spatial will not be updated.
   *
   * @return The spatial's last frustum intersection result.
   */
  public Camera.FrustumIntersect getLocalLastFrustumIntersection() { return _frustumIntersects; }

  /**
   * Tries to find the most accurate last frustum intersection for this spatial by checking the parent
   * for possible Outside value.
   *
   * @return Outside, if this, or any ancestor was Outside, otherwise the local intersect value.
   */
  public Camera.FrustumIntersect getLastFrustumIntersection() {
    if (_parent != null && _frustumIntersects != Camera.FrustumIntersect.Outside) {
      final Camera.FrustumIntersect parentIntersect = _parent.getLastFrustumIntersection();
      if (parentIntersect == Camera.FrustumIntersect.Outside) {
        return Camera.FrustumIntersect.Outside;
      }
    }
    return _frustumIntersects;
  }

  /**
   * Overrides the last intersection result. This is useful for operations that want to start
   * rendering at the middle of a scene tree and don't want the parent of that node to influence
   * culling. (See texture renderer code for example.)
   *
   * @param frustumIntersects
   *          the new frustum intersection value
   */
  public void setLastFrustumIntersection(final Camera.FrustumIntersect frustumIntersects) {
    _frustumIntersects = frustumIntersects;
  }

  /**
   * Execute the given Visitor on this Spatial, and any Spatials managed by this Spatial as
   * appropriate.
   *
   * @param visitor
   *          the Visitor object to use.
   * @param preexecute
   *          if true, we will visit <i>this</i> Spatial before any Spatials we manage (such as
   *          children of a Node.) If false, we will visit them first, then ourselves.
   */
  public void acceptVisitor(final Visitor visitor, final boolean preexecute) {
    visitor.visit(this);
  }

  /**
   * Returns the Spatial's name followed by the class of the spatial <br>
   * Example: "MyNode (com.ardor3d.scene.Spatial)
   *
   * @return Spatial's name followed by the class of the Spatial
   */
  @Override
  public String toString() {
    return _name + " (" + this.getClass().getName() + ')';
  }

  /**
   * Create a copy of this spatial.
   *
   * @param shareGeometricData
   *          if true, reuse any data fields describing the geometric shape of the spatial, as
   *          applicable.
   * @return the copy as described.
   */
  public Spatial makeCopy(final boolean shareGeometricData) {
    final Spatial spat = newInstance();

    // copy basic spatial info
    spat.setName(getName());
    spat.getSceneHints().set(_sceneHints);
    spat.setTransform(_localTransform);

    // copy local render states
    for (final StateType type : _renderStateList.keySet()) {
      final RenderState state = _renderStateList.get(type);
      if (state != null) {
        spat.setRenderState(state);
      }
    }

    // copy controllers
    if (_controllers != null) {
      for (final SpatialController<?> sc : _controllers) {
        spat.addController(sc);
      }
    }

    // copy material
    spat.setRenderMaterial(_material);

    // copy properties
    spat._properties.putAll(_properties);

    return spat;
  }

  private Spatial newInstance() {
    Spatial spat = null;
    final Class<? extends Spatial> clazz = getClass();
    try {
      final SavableFactory ann = clazz.getAnnotation(SavableFactory.class);
      if (ann == null) {
        spat = clazz.getDeclaredConstructor().newInstance();
      } else {
        spat = (Spatial) clazz.getMethod(ann.factoryMethod(), (Class<?>[]) null).invoke(null, (Object[]) null);
      }
    } catch (final InstantiationException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    } catch (final IllegalAccessException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    } catch (final NoSuchMethodException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    } catch (final IllegalArgumentException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    } catch (final SecurityException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    } catch (final InvocationTargetException e) {
      Spatial.logger.log(Level.SEVERE, "Could not access final constructor of class " + clazz.getCanonicalName(), e);
      throw new RuntimeException(e);
    }
    return spat;
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  /**
   * @see Savable#getClassTag()
   */
  @Override
  public Class<? extends Spatial> getClassTag() { return this.getClass(); }

  /**
   * @param capsule
   *          the input capsule
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @see Savable#read(InputCapsule)
   */
  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _name = capsule.readString("name", null);

    final RenderState[] states =
        CapsuleUtils.asArray(capsule.readSavableArray("renderStateList", null), RenderState.class);
    _renderStateList.clear();
    if (states != null) {
      for (final RenderState state : states) {
        _renderStateList.put(state.getType(), state);
      }
    }

    _localTransform.set(capsule.readSavable("localTransform", (Transform) Transform.IDENTITY));
    _worldTransform.set(capsule.readSavable("worldTransform", (Transform) Transform.IDENTITY));

    _properties.clear();
    _properties.putAll(capsule.readStringObjectMap("properties", new HashMap<>()));

    final List<Savable> list = capsule.readSavableList("controllers", null);
    if (list != null) {
      for (final Savable s : list) {
        if (s instanceof SpatialController<?>) {
          addController((SpatialController<?>) s);
        }
      }
    }

    _layer = capsule.readInt("layer", Spatial.LAYER_INHERIT);
  }

  /**
   * @param capsule
   *          the capsule
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   * @see Savable#write(OutputCapsule)
   */
  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_name, "name", null);

    capsule.write(_renderStateList.values().toArray(new RenderState[0]), "renderStateList", null);

    capsule.write(_localTransform, "localTransform", new Transform(Transform.IDENTITY));
    capsule.write(_worldTransform, "worldTransform", new Transform(Transform.IDENTITY));

    capsule.writeStringObjectMap(_properties, "properties", new HashMap<>());

    if (_controllers != null) {
      final List<Savable> list = new ArrayList<>();
      for (final SpatialController<?> sc : _controllers) {
        if (sc instanceof Savable) {
          list.add((Savable) sc);
        }
      }
      capsule.writeSavableList(list, "controllers", null);
    }

    capsule.write(_layer, "layer", Spatial.LAYER_INHERIT);
  }
}
