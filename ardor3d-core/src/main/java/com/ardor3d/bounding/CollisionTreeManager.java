/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.bounding;

import java.util.*;

import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * CollisionTreeManager is an automated system for handling the creation and deletion of
 * CollisionTrees. The manager maintains a cache map of currently generated collision trees. The
 * collision system itself requests a collision tree from the manager via the
 * <code>getCollisionTree</code> method. The cache is checked for the tree, and if it is available,
 * sent to the caller. If the tree is not in the cache, and generateTrees is true, a new
 * CollisionTree is generated on the fly and sent to the caller. When a new tree is created, the
 * cache size is compared to the maxElements value. If the cache is larger than maxElements, the
 * cache is sent to the CollisionTreeController for cleaning.
 * <p>
 * There are a number of settings that can be used to control how trees are generated. First,
 * generateTrees denotes whether the manager should be creating trees at all. This is set to true by
 * default. doSort defines if the CollisionTree primitive array should be sorted as it is built.
 * This is false by default. Sorting is beneficial for model data that is not well ordered
 * spatially. This occurrence is rare, and sorting slows creation time. It is, therefore, only to be
 * used when model data requires it. maxPrimitivesPerLeaf defines the number of primitives a leaf
 * node in the collision tree should maintain. The larger number of primitives maintained in a leaf
 * node, the smaller the tree, but the larger the number of checks during a collision. By default,
 * this value is set to 16. maxElements defines the maximum number of trees that will be maintained
 * before clean-up is required. A collision tree is defined for each mesh that is being collided
 * with. The user should determine the optimal number of trees to maintain (a memory/performance
 * tradeoff), based on the number of meshes, their population density and their primitive size. By
 * default, this value is set to 25. The type of trees that will be generated is defined by the
 * treeType value, where valid options are define in CollisionTree as AABB_TREE, OBB_TREE and
 * SPHERE_TREE. You can set the functionality of how trees are removed from the cache by providing
 * the manager with a CollisionTreeController implementation. By default, the manager will use the
 * UsageTreeController for removing trees, but any other CollisionTreeController is acceptable. You
 * can create protected tree manually. These are collision trees that you request the manager to
 * create and not allow them to be removed by the CollisionTreeController.
 *
 * @see com.ardor3d.bounding.CollisionTree
 * @see com.ardor3d.bounding.CollisionTreeController
 */
public enum CollisionTreeManager {
  INSTANCE;

  /**
   * defines the default maximum number of trees to maintain.
   */
  public static final int DEFAULT_MAX_ELEMENTS = 25;
  /**
   * defines the default maximum number of primitives in a tree leaf.
   */
  public static final int DEFAULT_MAX_PRIMITIVES_PER_LEAF = 16;

  // the cache and protected list for storing trees.
  private final Map<Mesh, CollisionTree> _cache;
  private final List<Mesh> _protectedList;

  private boolean _generateTrees = true;
  private boolean _doSort;

  private CollisionTree.Type _treeType = CollisionTree.Type.AABB;

  private int _maxPrimitivesPerLeaf = DEFAULT_MAX_PRIMITIVES_PER_LEAF;
  private int _maxElements = DEFAULT_MAX_ELEMENTS;

  private CollisionTreeController _treeRemover;

  /**
   * private constructor for the Singleton. Initializes the cache.
   */
  CollisionTreeManager() {
    _cache = new WeakHashMap<>();
    _protectedList = Collections.synchronizedList(new ArrayList<>(1));
    setCollisionTreeController(new UsageTreeController());
  }

  /**
   * retrieves the singleton instance of the CollisionTreeManager.
   * 
   * @return the singleton instance of the manager.
   */
  public static CollisionTreeManager getInstance() { return INSTANCE; }

  private CollisionTree cacheGet(final Mesh mesh) {
    return _cache.get(mesh);
  }

  private void cacheRemove(final Mesh mesh) {
    _cache.remove(mesh);
  }

  private void cachePut(final Mesh mesh, final CollisionTree tree) {
    _cache.put(mesh, tree);
  }

  /**
   * sets the CollisionTreeController used for cleaning the cache when the maximum number of elements
   * is reached.
   * 
   * @param treeRemover
   *          the controller used to clean the cache.
   */
  public void setCollisionTreeController(final CollisionTreeController treeRemover) { _treeRemover = treeRemover; }

  /**
   * getCollisionTree obtains a collision tree that is assigned to a supplied Mesh. The cache is
   * checked for a pre-existing tree, if none is available and generateTrees is true, a new tree is
   * created and returned.
   * 
   * @param mesh
   *          the mesh to use as the key for the tree to obtain.
   * @return the tree associated with a given mesh
   */
  public synchronized CollisionTree getCollisionTree(final Mesh mesh) {
    var toReturn = cacheGet(mesh);

    // we didn't have it in the cache, create it if possible.
    if (toReturn == null) {
      if (_generateTrees) {
        return generateCollisionTree(_treeType, mesh, false);
      } else {
        return null;
      }
    } else {
      // we had it in the cache, to keep the keyset in order, reinsert this element
      cacheRemove(mesh);
      cachePut(mesh, toReturn);
      return toReturn;
    }
  }

  /**
   * creates a new collision tree for the provided spatial. If the spatial is a node, it recursively
   * calls generateCollisionTree for each child. If it is a Mesh, a call to generateCollisionTree is
   * made for each mesh. If this tree(s) is to be protected, i.e. not deleted by the
   * CollisionTreeController, set protect to true.
   * 
   * @param type
   *          the type of collision tree to generate.
   * @param object
   *          the Spatial to generate tree(s) for.
   * @param protect
   *          true to keep these trees from being removed, false otherwise.
   */
  public void generateCollisionTree(final CollisionTree.Type type, final Spatial object, final boolean protect) {
    if (object instanceof Mesh) {
      generateCollisionTree(type, (Mesh) object, protect);
    }
    if (object instanceof Node) {
      if (((Node) object).getNumberOfChildren() > 0) {
        for (final Spatial sp : ((Node) object).getChildren()) {
          generateCollisionTree(type, sp, protect);
        }
      }
    }
  }

  /**
   * generates a new tree for the associated mesh. The type is provided and a new tree is constructed
   * of this type. The tree is placed in the cache. If the cache's size then becomes too large, the
   * cache is sent to the CollisionTreeController for clean-up. If this tree is to be protected, i.e.
   * protected from the CollisionTreeController, set protect to true.
   * 
   * @param type
   *          the type of collision tree to generate.
   * @param mesh
   *          the mesh to generate the tree for.
   * @param protect
   *          true if this tree is to be protected, false otherwise.
   * @return the new collision tree.
   */
  public CollisionTree generateCollisionTree(final CollisionTree.Type type, final Mesh mesh, final boolean protect) {
    if (mesh == null) {
      return null;
    }

    final CollisionTree tree = new CollisionTree(type);

    generateCollisionTree(tree, mesh, protect);

    return tree;
  }

  /**
   * generates a new tree for the associated mesh. It is provided with a pre-existing, non-null tree.
   * The tree is placed in the cache. If the cache's size then becomes too large, the cache is sent to
   * the CollisionTreeController for clean-up. If this tree is to be protected, i.e. protected from
   * the CollisionTreeController, set protect to true.
   * 
   * @param tree
   *          the tree to use for generation
   * @param mesh
   *          the mesh to generate the tree for.
   * @param protect
   *          true if this tree is to be protected, false otherwise.
   */
  private void generateCollisionTree(final CollisionTree tree, final Mesh mesh, final boolean protect) {
    tree.construct(mesh, _doSort);
    cachePut(mesh, tree);
    // This mesh has been added by outside sources and labeled
    // as protected. Therefore, put it in the protected list
    // so it is not removed by a controller.
    if (protect) {
      setProtected(mesh);
    }

    // Are we over our max? Test
    if (_cache.size() > _maxElements && _treeRemover != null) {
      _treeRemover.clean(_cache, _protectedList, _maxElements);
    }
  }

  /**
   * removes a collision tree from the manager based on the mesh supplied.
   * 
   * @param mesh
   *          the mesh to remove the corresponding collision tree.
   */
  public void removeCollisionTree(final Mesh mesh) {
    cacheRemove(mesh);
    removeProtected(mesh);
  }

  /**
   * removes all collision trees associated with a Spatial object.
   * 
   * @param object
   *          the spatial to remove all collision trees from.
   */
  public void removeCollisionTree(final Spatial object) {
    if (object instanceof Node n) {
      for (int i = n.getNumberOfChildren() - 1; i >= 0; i--) {
        removeCollisionTree(n.getChild(i));
      }
    } else if (object instanceof Mesh) {
      removeCollisionTree((Mesh) object);
    }
  }

  /**
   * updates the existing tree for a supplied mesh. If this tree does not exist, the tree is not
   * updated. If the tree is not in the cache, no further operations are handled.
   * 
   * @param mesh
   *          the mesh key for the tree to update.
   */
  public void updateCollisionTree(final Mesh mesh) {
    final CollisionTree ct = cacheGet(mesh);
    if (ct != null) {
      generateCollisionTree(ct, mesh, _protectedList != null && _protectedList.contains(mesh));
    }
  }

  /**
   * updates the existing tree(s) for a supplied spatial. If this tree does not exist, the tree is not
   * updated. If the tree is not in the cache, no further operations are handled.
   * 
   * @param object
   *          the object on which to update the tree.
   */
  public void updateCollisionTree(final Spatial object) {
    if (object instanceof Node n) {
      for (int i = n.getNumberOfChildren() - 1; i >= 0; i--) {
        updateCollisionTree(n.getChild(i));
      }
    } else if (object instanceof Mesh) {
      updateCollisionTree((Mesh) object);
    }
  }

  /**
   * returns true if the manager is set to sort new generated trees. False otherwise.
   * 
   * @return true to sort tree, false otherwise.
   */
  public boolean isDoSort() { return _doSort; }

  /**
   * set if this manager should have newly generated trees sort primitives.
   * 
   * @param doSort
   *          true to sort trees, false otherwise.
   */
  public void setDoSort(final boolean doSort) { _doSort = doSort; }

  /**
   * returns true if the manager will automatically generate new trees as needed, false otherwise.
   * 
   * @return true if this manager is generating trees, false otherwise.
   */
  public boolean isGenerateTrees() { return _generateTrees; }

  /**
   * set if this manager should generate new trees as needed.
   * 
   * @param generateTrees
   *          true to generate trees, false otherwise.
   */
  public void setGenerateTrees(final boolean generateTrees) { _generateTrees = generateTrees; }

  /**
   * @return the type of tree the manager will create.
   * @see CollisionTree.Type
   */
  public CollisionTree.Type getTreeType() { return _treeType; }

  /**
   * @param treeType
   *          the type of tree to create.
   * @see CollisionTree.Type
   */
  public void setTreeType(final CollisionTree.Type treeType) { _treeType = treeType; }

  /**
   * returns the maximum number of primitives a leaf of the collision tree may contain.
   * 
   * @return the maximum number of primitives a leaf may contain.
   */
  public int getMaxPrimitivesPerLeaf() { return _maxPrimitivesPerLeaf; }

  /**
   * set the maximum number of primitives a leaf of the collision tree may contain.
   * 
   * @param maxPrimitivesPerLeaf
   *          the maximum number of primitives a leaf may contain.
   */
  public void setMaxPrimitivesPerLeaf(final int maxPrimitivesPerLeaf) {
    _maxPrimitivesPerLeaf = maxPrimitivesPerLeaf;
  }

  /**
   * returns the maximum number of CollisionTree elements this manager will hold on to before starting
   * to clear some.
   * 
   * @return the maximum number of CollisionTree elements.
   */
  public int getMaxElements() { return _maxElements; }

  /**
   * set the maximum number of CollisionTree elements this manager will hold on to before starting to
   * clear some.
   * 
   * @param maxElements
   *          the maximum number of CollisionTree elements.
   */
  public void setMaxElements(final int maxElements) { _maxElements = maxElements; }

  /**
   * Add the given mesh to our "protected" list. This will signal to our cleanup operation that when
   * deciding which trees to trim in an effort to keep our cache size to a certain desired size, do
   * not trim the tree associated with this mesh.
   * 
   * @param meshToProtect
   *          the mesh whose CollisionTree we want to protect.
   */
  public void setProtected(final Mesh meshToProtect) {
    if (!_protectedList.contains(meshToProtect)) {
      _protectedList.add(meshToProtect);
    }
  }

  /**
   * Removes the supplied mesh from the "protected" list.
   * 
   * @param mesh
   */
  public void removeProtected(final Mesh mesh) {
    _protectedList.remove(mesh);
  }

  /**
   * 
   * @return an immutable copy of the list of protected meshes.
   */
  public List<Mesh> getProtectedMeshes() { return List.copyOf(_protectedList); }
}
