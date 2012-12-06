/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.util.List;

import com.ardor3d.bounding.CollisionTree;
import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.PickingHint;

public abstract class PickingUtil {
    /**
     * Finds a pick using the given ray starting at the scenegraph given as spatial. Results are stored in the given
     * results value.
     * 
     * NB: Spatials with CullHint ALWAYS will be skipped.
     * 
     * @param spatial
     * @param ray
     * @param results
     */
    public static void findPick(final Spatial spatial, final Ray3 ray, final PickResults results) {
        findPick(spatial, ray, results, true);
    }

    /**
     * Finds a pick using the given ray starting at the scenegraph given as spatial. Results are stored in the given
     * results value.
     * 
     * @param spatial
     * @param ray
     * @param results
     * @param ignoreCulled
     *            if true, Spatials with CullHint ALWAYS will be skipped.
     */
    public static void findPick(final Spatial spatial, final Ray3 ray, final PickResults results,
            final boolean ignoreCulled) {
        if (spatial == null || !spatial.getSceneHints().isPickingHintEnabled(PickingHint.Pickable)
                || (ignoreCulled && spatial.getSceneHints().getCullHint() == CullHint.Always)
                || spatial.getWorldBound() == null || !spatial.getWorldBound().intersects(ray)) {
            return;
        }

        if (spatial instanceof Pickable) {
            results.addPick(ray, (Pickable) spatial);
        } else if (spatial instanceof Node) {
            final Node node = (Node) spatial;
            for (int i = node.getNumberOfChildren() - 1; i >= 0; i--) {
                findPick(node.getChild(i), ray, results, ignoreCulled);
            }
        }
    }

    public static void findCollisions(final Spatial spatial, final Spatial scene, final CollisionResults results) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !scene.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getWorldBound().intersects(scene.getWorldBound())) {
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    PickingUtil.findCollisions(node.getChild(i), scene, results);
                }
            }
        } else if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;

            if (mesh.getWorldBound().intersects(scene.getWorldBound())) {
                if (scene instanceof Node) {
                    final Node parent = (Node) scene;
                    for (int i = 0; i < parent.getNumberOfChildren(); i++) {
                        PickingUtil.findCollisions(mesh, parent.getChild(i), results);
                    }
                } else {
                    results.addCollision(mesh, (Mesh) scene);
                }
            }
        }
    }

    /**
     * This function checks for intersection between this mesh and the given one. On the first intersection, true is
     * returned.
     * 
     * @param toCheck
     *            The intersection testing mesh.
     * @return True if they intersect.
     */
    public static boolean hasPrimitiveCollision(final Mesh testMesh, final Mesh toCheck) {
        if (!testMesh.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        final CollisionTree thisCT = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree checkCT = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (thisCT == null || checkCT == null) {
            return false;
        }

        final ReadOnlyTransform worldTransform = testMesh.getWorldTransform();
        thisCT.getBounds().transform(worldTransform, thisCT.getWorldBounds());
        return thisCT.intersect(checkCT);
    }

    /**
     * This function finds all intersections between this mesh and the checking one. The intersections are stored as
     * PrimitiveKeys.
     * 
     * @param toCheck
     *            The Mesh to check.
     * @param testIndex
     *            The array of PrimitiveKeys intersecting in this mesh.
     * @param otherIndex
     *            The array of PrimitiveKeys intersecting in the given mesh.
     */
    public static void findPrimitiveCollision(final Mesh testMesh, final Mesh toCheck,
            final List<PrimitiveKey> testIndex, final List<PrimitiveKey> otherIndex) {
        if (!testMesh.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !toCheck.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return;
        }

        final CollisionTree myTree = CollisionTreeManager.getInstance().getCollisionTree(testMesh);
        final CollisionTree otherTree = CollisionTreeManager.getInstance().getCollisionTree(toCheck);

        if (myTree == null || otherTree == null) {
            return;
        }

        myTree.getBounds().transform(testMesh.getWorldTransform(), myTree.getWorldBounds());

        myTree.intersect(otherTree, testIndex, otherIndex);
    }

    public static boolean hasCollision(final Spatial spatial, final Spatial scene, final boolean checkPrimitives) {
        if (spatial == scene || spatial.getWorldBound() == null
                || !spatial.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)
                || !scene.getSceneHints().isPickingHintEnabled(PickingHint.Collidable)) {
            return false;
        }

        if (spatial instanceof Node) {
            final Node node = (Node) spatial;

            if (node.getWorldBound().intersects(scene.getWorldBound())) {
                if (node.getNumberOfChildren() == 0 && !checkPrimitives) {
                    return true;
                }
                // further checking needed.
                for (int i = 0; i < node.getNumberOfChildren(); i++) {
                    if (PickingUtil.hasCollision(node.getChild(i), scene, checkPrimitives)) {
                        return true;
                    }
                }
            }
        } else if (spatial instanceof Mesh) {
            final Mesh mesh = (Mesh) spatial;

            if (mesh.getWorldBound().intersects(scene.getWorldBound())) {
                if (scene instanceof Node) {
                    final Node parent = (Node) scene;
                    for (int i = 0; i < parent.getNumberOfChildren(); i++) {
                        if (PickingUtil.hasCollision(mesh, parent.getChild(i), checkPrimitives)) {
                            return true;
                        }
                    }

                    return false;
                }

                if (!checkPrimitives) {
                    return true;
                }

                return PickingUtil.hasPrimitiveCollision(mesh, (Mesh) scene);
            }

            return false;

        }

        return false;
    }
}
