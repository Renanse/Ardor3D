/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.clip.AbstractAnimationChannel;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.TransformChannel;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Multimap;

/**
 * Data storage object meant to hold objects parsed from a Collada file that the user might want to
 * directly access.
 */
public class ColladaStorage implements Savable {

  private Node _scene;
  private final List<SkinData> _skins = new ArrayList<>();
  private AssetData _assetData;

  private final List<AbstractAnimationChannel> _transformChannels = new ArrayList<>();
  private AnimationItem _animationItemRoot;

  // List of parsed color buffers, useful if collada includes multiple color channels per meshdata
  // object
  // Transient because the key is identity - meshdata.
  private transient Multimap<MeshData, FloatBuffer> _parsedVertexColors;

  // Map of parsed material information, useful for doing post-manipulation of model information.
  // Transient because the key is identity - mesh.
  private transient Map<Mesh, String> _meshMaterialInfo;
  private transient Map<String, MaterialInfo> _materialMap;

  public void setScene(final Node scene) { _scene = scene; }

  /**
   * @return a Node representing the parsed "visual scene".
   */
  public Node getScene() { return _scene; }

  /**
   * @return a list of data objects representing each <skin> tag parsed during reading of the visual
   *         scene.
   */
  public List<SkinData> getSkins() { return _skins; }

  public AssetData getAssetData() { return _assetData; }

  public void setAssetData(final AssetData assetData) { _assetData = assetData; }

  public List<AbstractAnimationChannel> getAnimationChannels() { return _transformChannels; }

  /**
   * @return the root of our animation library. We can use this to walk through all animations in the
   *         Collada file.
   */
  public AnimationItem getAnimationItemRoot() { return _animationItemRoot; }

  public void setAnimationItemRoot(final AnimationItem animationItemRoot) { _animationItemRoot = animationItemRoot; }

  /**
   * Extract all animation channels in the Collada file as a single, unified AnimationClip.
   *
   * @param name
   *          the name to give our new clip.
   * @return the new AnimationClip.
   */
  public AnimationClip extractChannelsAsClip(final String name) {
    final AnimationClip clip = new AnimationClip(name);
    for (final AbstractAnimationChannel channel : getAnimationChannels()) {
      clip.addChannel(channel);
    }
    return clip;
  }

  /**
   * @return a transient Multimap of MeshData -> List of parsed vertex colors. Only MeshData objects
   *         that had multiple vertex colors parsed will show up in this map.
   */
  public Multimap<MeshData, FloatBuffer> getParsedVertexColors() { return _parsedVertexColors; }

  public void setParsedVertexColors(final Multimap<MeshData, FloatBuffer> parsedVertexColors) {
    _parsedVertexColors = parsedVertexColors;
  }

  public Map<String, MaterialInfo> getMaterialMap() { return _materialMap; }

  public void setMaterialMap(final Map<String, MaterialInfo> materialMap) { _materialMap = materialMap; }

  public Map<Mesh, String> getMeshMaterialInfo() { return _meshMaterialInfo; }

  public void setMeshMaterialInfo(final Map<Mesh, String> meshMaterialInfo) { _meshMaterialInfo = meshMaterialInfo; }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<?> getClassTag() { return this.getClass(); }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _assetData = capsule.readSavable("assetData", null);
    _scene = capsule.readSavable("scene", null);
    _skins.addAll(capsule.readSavableList("skins", new LinkedList<SkinData>()));
    _transformChannels.clear();
    _transformChannels.addAll(capsule.readSavableList("jointChannels", new LinkedList<TransformChannel>()));
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_assetData, "assetData", null);
    capsule.write(_scene, "scene", null);
    capsule.writeSavableList(_skins, "skins", new LinkedList<SkinData>());
    capsule.writeSavableList(_transformChannels, "jointChannels", new LinkedList<TransformChannel>());
  }
}
