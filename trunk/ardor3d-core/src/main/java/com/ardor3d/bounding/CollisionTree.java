/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.intersection.Intersection;
import com.ardor3d.intersection.PrimitiveKey;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * CollisionTree defines a well balanced red black tree used for triangle accurate collision detection. The
 * CollisionTree supports three types: Oriented Bounding Box, Axis-Aligned Bounding Box and Sphere. The tree is composed
 * of a hierarchy of nodes, all but leaf nodes have two children, a left and a right, where the children contain half of
 * the triangles of the parent. This "half split" is executed down the tree until the node is maintaining a set maximum
 * of triangles. This node is called the leaf node. Intersection checks are handled as follows:<br>
 * 1. The bounds of the node is checked for intersection. If no intersection occurs here, no further processing is
 * needed, the children (nodes or triangles) do not intersect.<br>
 * 2a. If an intersection occurs and we have children left/right nodes, pass the intersection information to the
 * children.<br>
 * 2b. If an intersection occurs and we are a leaf node, pass each triangle individually for intersection checking.<br>
 * <br>
 * Optionally, during creation of the collision tree, sorting can be applied. Sorting will attempt to optimize the order
 * of the triangles in such a way as to best split for left and right sub-trees. This function can lead to faster
 * intersection tests, but increases the creation time for the tree. The number of triangles a leaf node is responsible
 * for is defined in CollisionTreeManager. It is actually recommended to allow CollisionTreeManager to maintain the
 * collision trees for a scene.
 * 
 * @see com.ardor3d.bounding.CollisionTreeManager
 */
public class CollisionTree implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        /** CollisionTree using Oriented Bounding Boxes. */
        OBB,
        /** CollisionTree using Axis-Aligned Bounding Boxes. */
        AABB,
        /** CollisionTree using Bounding Spheres. */
        Sphere;
    }

    // Default tree is axis-aligned
    protected Type _type = Type.AABB;

    // children trees
    protected CollisionTree _left;
    protected CollisionTree _right;

    // bounding volumes that contain the triangles that the node is handling
    protected BoundingVolume _bounds;
    protected BoundingVolume _worldBounds;

    /**
     * the list of primitives indices that compose the tree. This list contains all the triangles of the mesh and is
     * shared between all nodes of this tree. Stored here to allow for sorting.
     */
    protected int[] _primitiveIndices;

    // Defines the pointers into the triIndex array that this node is directly responsible for.
    protected int _start, _end;

    // Required Spatial information
    protected transient WeakReference<Mesh> _mesh;
    protected int _section;

    // Comparator used to sort triangle indices
    protected transient final TreeComparator _comparator = new TreeComparator();

    /**
     * Constructor creates a new instance of CollisionTree.
     * 
     * @param type
     *            the type of collision tree to make
     * @see Type
     */
    public CollisionTree(final Type type) {
        _type = type;
    }

    /**
     * Recreate this Collision Tree for the given Node and child index.
     * 
     * @param childIndex
     *            the index of the child to generate the tree for.
     * @param parent
     *            The Node that this tree should represent.
     * @param doSort
     *            true to sort primitives during creation, false otherwise
     */
    public void construct(final int childIndex, final int section, final Node parent, final boolean doSort) {
        final Spatial spat = parent.getChild(childIndex);
        if (spat instanceof Mesh) {
            _mesh = makeRef((Mesh) spat);
            _primitiveIndices = new int[((Mesh) spat).getMeshData().getPrimitiveCount(section)];
            for (int i = 0; i < _primitiveIndices.length; i++) {
                _primitiveIndices[i] = i;
            }
            createTree(section, 0, _primitiveIndices.length, doSort);
        }
    }

    /**
     * Recreate this Collision Tree for the given mesh.
     * 
     * @param mesh
     *            The mesh that this tree should represent.
     * @param doSort
     *            true to sort primitives during creation, false otherwise
     */
    public void construct(final Mesh mesh, final boolean doSort) {
        _mesh = makeRef(mesh);
        if (mesh.getMeshData().getSectionCount() == 1) {
            _primitiveIndices = new int[mesh.getMeshData().getPrimitiveCount(0)];
            for (int i = 0; i < _primitiveIndices.length; i++) {
                _primitiveIndices[i] = i;
            }
            createTree(0, 0, _primitiveIndices.length, doSort);
        } else {
            // divide up the sections into the tree by adding intermediate nodes as needed.
            splitMesh(mesh, 0, mesh.getMeshData().getSectionCount(), doSort);
        }
    }

    protected void splitMesh(final Mesh mesh, final int sectionStart, final int sectionEnd, final boolean doSort) {
        _mesh = makeRef(mesh);

        // Split range in half
        final int rangeSize = sectionEnd - sectionStart;
        final int halfRange = rangeSize / 2; // odd number will give +1 to right.

        // left half:
        // if half size == 1, create as regular CollisionTree
        if (halfRange == 1) {
            // compute section
            final int section = sectionStart;

            // create the left child
            _left = new CollisionTree(_type);

            _left._primitiveIndices = new int[mesh.getMeshData().getPrimitiveCount(section)];
            for (int i = 0; i < _left._primitiveIndices.length; i++) {
                _left._primitiveIndices[i] = i;
            }
            _left._mesh = _mesh;
            _left.createTree(section, 0, _left._primitiveIndices.length, doSort);
        } else {
            // otherwise, make an empty collision tree and call split with new range
            _left = new CollisionTree(_type);
            _left.splitMesh(mesh, sectionStart, sectionStart + halfRange, doSort);
        }

        // right half:
        // if rangeSize - half size == 1, create as regular CollisionTree
        if (rangeSize - halfRange == 1) {
            // compute section
            final int section = sectionStart + 1;

            // create the left child
            _right = new CollisionTree(_type);

            _right._primitiveIndices = new int[mesh.getMeshData().getPrimitiveCount(section)];
            for (int i = 0; i < _right._primitiveIndices.length; i++) {
                _right._primitiveIndices[i] = i;
            }
            _right._mesh = _mesh;
            _right.createTree(section, 0, _right._primitiveIndices.length, doSort);
        } else {
            // otherwise, make an empty collision tree and call split with new range
            _right = new CollisionTree(_type);
            _right.splitMesh(mesh, sectionStart + halfRange, sectionEnd, doSort);
        }

        // Ok, now since we technically have no primitives, we need our bounds to be the merging of our children bounds
        // instead:
        _bounds = _left._bounds.clone(_bounds);
        _bounds.mergeLocal(_right._bounds);
        _worldBounds = _bounds.clone(_worldBounds);
    }

    /**
     * Creates a Collision Tree by recursively creating children nodes, splitting the primitives this node is
     * responsible for in half until the desired primitive count is reached.
     * 
     * @param start
     *            The start index of the primitivesArray, inclusive.
     * @param end
     *            The end index of the primitivesArray, exclusive.
     * @param doSort
     *            True if the primitives should be sorted at each level, false otherwise.
     */
    public void createTree(final int section, final int start, final int end, final boolean doSort) {
        _section = section;
        _start = start;
        _end = end;

        if (_primitiveIndices == null) {
            return;
        }

        createBounds();

        // the bounds at this level should contain all the primitives this level is responsible for.
        _bounds.computeFromPrimitives(getMesh().getMeshData(), _section, _primitiveIndices, _start, _end);

        // check to see if we are a leaf, if the number of primitives we reference is less than or equal to the maximum
        // defined by the CollisionTreeManager we are done.
        if (_end - _start + 1 <= CollisionTreeManager.getInstance().getMaxPrimitivesPerLeaf()) {
            return;
        }

        // if doSort is set we need to attempt to optimize the referenced primitives. optimizing the sorting of the
        // primitives will help group them spatially in the left/right children better.
        if (doSort) {
            sortPrimitives();
        }

        // create the left child
        if (_left == null) {
            _left = new CollisionTree(_type);
        }

        _left._primitiveIndices = _primitiveIndices;
        _left._mesh = _mesh;
        _left.createTree(_section, _start, (_start + _end) / 2, doSort);

        // create the right child
        if (_right == null) {
            _right = new CollisionTree(_type);
        }
        _right._primitiveIndices = _primitiveIndices;
        _right._mesh = _mesh;
        _right.createTree(_section, (_start + _end) / 2, _end, doSort);
    }

    /**
     * Tests if the world bounds of the node at this level intersects a provided bounding volume. If an intersection
     * occurs, true is returned, otherwise false is returned. If the provided volume is invalid, false is returned.
     * 
     * @param volume
     *            the volume to intersect with.
     * @return true if there is an intersect, false otherwise.
     */
    public boolean intersectsBounding(final BoundingVolume volume) {
        switch (volume.getType()) {
            case AABB:
                return _worldBounds.intersectsBoundingBox((BoundingBox) volume);
            case OBB:
                return _worldBounds.intersectsOrientedBoundingBox((OrientedBoundingBox) volume);
            case Sphere:
                return _worldBounds.intersectsSphere((BoundingSphere) volume);
            default:
                return false;
        }

    }

    /**
     * Determines if this Collision Tree intersects the given CollisionTree. If a collision occurs, true is returned,
     * otherwise false is returned. If the provided collisionTree is invalid, false is returned.
     * 
     * @param collisionTree
     *            The Tree to test.
     * @return True if they intersect, false otherwise.
     */
    public boolean intersect(final CollisionTree collisionTree) {
        if (collisionTree == null) {
            return false;
        }

        collisionTree._worldBounds = collisionTree._bounds.transform(collisionTree.getMesh().getWorldTransform(),
                collisionTree._worldBounds);

        // our two collision bounds do not intersect, therefore, our primitives
        // must not intersect. Return false.
        if (!intersectsBounding(collisionTree._worldBounds)) {
            return false;
        }

        // check children
        if (_left != null) { // This is not a leaf
            if (collisionTree.intersect(_left)) {
                return true;
            }
            if (collisionTree.intersect(_right)) {
                return true;
            }
            return false;
        }

        // This is a leaf
        if (collisionTree._left != null) {
            // but collision isn't
            if (intersect(collisionTree._left)) {
                return true;
            }
            if (intersect(collisionTree._right)) {
                return true;
            }
            return false;
        }

        // both are leaves
        final ReadOnlyTransform transformA = getMesh().getWorldTransform();
        final ReadOnlyTransform transformB = collisionTree.getMesh().getWorldTransform();

        final MeshData dataA = getMesh().getMeshData();
        final MeshData dataB = collisionTree.getMesh().getMeshData();

        Vector3[] storeA = null;
        Vector3[] storeB = null;

        // for every primitive to compare, put them into world space and check for intersections
        for (int i = _start; i < _end; i++) {
            storeA = dataA.getPrimitiveVertices(_primitiveIndices[i], _section, storeA);
            // to world space
            for (int t = 0; t < storeA.length; t++) {
                transformA.applyForward(storeA[t]);
            }
            for (int j = collisionTree._start; j < collisionTree._end; j++) {
                storeB = dataB.getPrimitiveVertices(collisionTree._primitiveIndices[j], collisionTree._section, storeB);
                // to world space
                for (int t = 0; t < storeB.length; t++) {
                    transformB.applyForward(storeB[t]);
                }
                if (Intersection.intersection(storeA, storeB)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines if this Collision Tree intersects the given CollisionTree. If a collision occurs, true is returned,
     * otherwise false is returned. If the provided collisionTree is invalid, false is returned. All collisions that
     * occur are stored in lists as an integer index into the mesh's triangle buffer. where aList is the primitives for
     * this mesh and bList is the primitives for the test tree.
     * 
     * @param collisionTree
     *            The Tree to test.
     * @param aList
     *            a list to contain the colliding primitives of this mesh.
     * @param bList
     *            a list to contain the colliding primitives of the testing mesh.
     * @return True if they intersect, false otherwise.
     */
    public boolean intersect(final CollisionTree collisionTree, final List<PrimitiveKey> aList,
            final List<PrimitiveKey> bList) {

        if (collisionTree == null) {
            return false;
        }

        collisionTree._worldBounds = collisionTree._bounds.transform(collisionTree.getMesh().getWorldTransform(),
                collisionTree._worldBounds);

        // our two collision bounds do not intersect, therefore, our primitives
        // must not intersect. Return false.
        if (!intersectsBounding(collisionTree._worldBounds)) {
            return false;
        }

        // if our node is not a leaf send the children (both left and right) to
        // the test tree.
        if (_left != null) { // This is not a leaf
            boolean test = collisionTree.intersect(_left, bList, aList);
            test = collisionTree.intersect(_right, bList, aList) || test;
            return test;
        }

        // This node is a leaf, but the testing tree node is not. Therefore,
        // continue processing the testing tree until we find its leaves.
        if (collisionTree._left != null) {
            boolean test = intersect(collisionTree._left, aList, bList);
            test = intersect(collisionTree._right, aList, bList) || test;
            return test;
        }

        // both this node and the testing node are leaves. Therefore, we can
        // switch to checking the contained primitives with each other. Any
        // that are found to intersect are placed in the appropriate list.
        final ReadOnlyTransform transformA = getMesh().getWorldTransform();
        final ReadOnlyTransform transformB = collisionTree.getMesh().getWorldTransform();

        final MeshData dataA = getMesh().getMeshData();
        final MeshData dataB = collisionTree.getMesh().getMeshData();

        Vector3[] storeA = null;
        Vector3[] storeB = null;

        boolean test = false;

        for (int i = _start; i < _end; i++) {
            storeA = dataA.getPrimitiveVertices(_primitiveIndices[i], _section, storeA);
            // to world space
            for (int t = 0; t < storeA.length; t++) {
                transformA.applyForward(storeA[t]);
            }
            for (int j = collisionTree._start; j < collisionTree._end; j++) {
                storeB = dataB.getPrimitiveVertices(collisionTree._primitiveIndices[j], collisionTree._section, storeB);
                // to world space
                for (int t = 0; t < storeB.length; t++) {
                    transformB.applyForward(storeB[t]);
                }
                if (Intersection.intersection(storeA, storeB)) {
                    test = true;
                    aList.add(new PrimitiveKey(_primitiveIndices[i], _section));
                    bList.add(new PrimitiveKey(collisionTree._primitiveIndices[j], collisionTree._section));
                }
            }
        }

        return test;
    }

    /**
     * intersect checks for collisions between this collision tree and a provided Ray. Any collisions are stored in a
     * provided list as primitive index values. The ray is assumed to have a normalized direction for accurate
     * calculations.
     * 
     * @param ray
     *            the ray to test for intersections.
     * @param store
     *            a list to fill with the index values of the primitive hit. if null, a new List is created.
     * @return the list.
     */
    public List<PrimitiveKey> intersect(final Ray3 ray, final List<PrimitiveKey> store) {
        List<PrimitiveKey> result = store;
        if (result == null) {
            result = new ArrayList<PrimitiveKey>();
        }

        // if our ray doesn't hit the bounds, then it must not hit a primitive.
        if (!_worldBounds.intersects(ray)) {
            return result;
        }

        // This is not a leaf node, therefore, check each child (left/right) for intersection with the ray.
        if (_left != null) {
            _left._worldBounds = _left._bounds.transform(getMesh().getWorldTransform(), _left._worldBounds);
            _left.intersect(ray, result);
        }

        if (_right != null) {
            _right._worldBounds = _right._bounds.transform(getMesh().getWorldTransform(), _right._worldBounds);
            _right.intersect(ray, result);
        } else if (_left == null) {
            // This is a leaf node. We can therefore check each primitive this node contains. If an intersection occurs,
            // place it in the list.

            final MeshData data = getMesh().getMeshData();
            final ReadOnlyTransform transform = getMesh().getWorldTransform();

            Vector3[] points = null;
            for (int i = _start; i < _end; i++) {
                points = data.getPrimitiveVertices(_primitiveIndices[i], _section, points);
                for (int t = 0; t < points.length; t++) {
                    transform.applyForward(points[t]);
                }
                if (ray.intersects(points, null)) {
                    result.add(new PrimitiveKey(_primitiveIndices[i], _section));
                }
            }
        }
        return result;
    }

    /**
     * Returns the bounding volume for this tree node in local space.
     * 
     * @return the bounding volume for this tree node in local space.
     */
    public BoundingVolume getBounds() {
        return _bounds;
    }

    /**
     * Returns the bounding volume for this tree node in world space.
     * 
     * @return the bounding volume for this tree node in world space.
     */
    public BoundingVolume getWorldBounds() {
        return _worldBounds;
    }

    /**
     * creates the appropriate bounding volume based on the type set during construction.
     */
    private void createBounds() {
        switch (_type) {
            case AABB:
                _bounds = new BoundingBox();
                _worldBounds = new BoundingBox();
                break;
            case OBB:
                _bounds = new OrientedBoundingBox();
                _worldBounds = new OrientedBoundingBox();
                break;
            case Sphere:
                _bounds = new BoundingSphere();
                _worldBounds = new BoundingSphere();
                break;
            default:
                break;
        }
    }

    /**
     * sortPrimitives attempts to optimize the ordering of the subsection of the array of primitives this node is
     * responsible for. The sorting is based on the most efficient method along an axis. Using the TreeComparator and
     * quick sort, the subsection of the array is sorted.
     */
    public void sortPrimitives() {
        switch (_type) {
            case AABB:
                // determine the longest length of the box, this axis will be best for sorting.
                if (((BoundingBox) _bounds).getXExtent() > ((BoundingBox) _bounds).getYExtent()) {
                    if (((BoundingBox) _bounds).getXExtent() > ((BoundingBox) _bounds).getZExtent()) {
                        _comparator.setAxis(TreeComparator.Axis.X);
                    } else {
                        _comparator.setAxis(TreeComparator.Axis.Z);
                    }
                } else {
                    if (((BoundingBox) _bounds).getYExtent() > ((BoundingBox) _bounds).getZExtent()) {
                        _comparator.setAxis(TreeComparator.Axis.Y);
                    } else {
                        _comparator.setAxis(TreeComparator.Axis.Z);
                    }
                }
                break;
            case OBB:
                // determine the longest length of the box, this axis will be best for sorting.
                if (((OrientedBoundingBox) _bounds)._extent.getX() > ((OrientedBoundingBox) _bounds)._extent.getY()) {
                    if (((OrientedBoundingBox) _bounds)._extent.getX() > ((OrientedBoundingBox) _bounds)._extent.getZ()) {
                        _comparator.setAxis(TreeComparator.Axis.X);
                    } else {
                        _comparator.setAxis(TreeComparator.Axis.Z);
                    }
                } else {
                    if (((OrientedBoundingBox) _bounds)._extent.getY() > ((OrientedBoundingBox) _bounds)._extent.getZ()) {
                        _comparator.setAxis(TreeComparator.Axis.Y);
                    } else {
                        _comparator.setAxis(TreeComparator.Axis.Z);
                    }
                }
                break;
            case Sphere:
                // sort any axis, X is fine.
                _comparator.setAxis(TreeComparator.Axis.X);
                break;
            default:
                break;
        }

        _comparator.setMesh(getMesh());
        // TODO: broken atm
        // SortUtil.qsort(_primitiveIndices, _start, _end - 1, _comparator);
    }

    /**
     * @return the Mesh referenced by _mesh
     */
    private Mesh getMesh() {
        return _mesh.get();
    }

    /**
     * @param mesh
     *            Mesh object to reference.
     * @return a new reference to the given mesh.
     */
    private WeakReference<Mesh> makeRef(final Mesh mesh) {
        return new WeakReference<Mesh>(mesh);
    }
}
