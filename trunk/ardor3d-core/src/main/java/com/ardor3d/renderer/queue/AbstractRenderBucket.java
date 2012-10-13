/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.queue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.SortUtil;

public class AbstractRenderBucket implements RenderBucket {

    protected Comparator<Spatial> _comparator;

    protected Spatial[] _currentList, _tempList;
    protected int _currentListSize;

    protected Stack<Spatial[]> _listStack = new Stack<Spatial[]>();
    protected Stack<Spatial[]> _listStackPool = new Stack<Spatial[]>();
    protected Stack<Integer> _listSizeStack = new Stack<Integer>();

    public AbstractRenderBucket() {
        _currentList = new Spatial[32];
        _tempList = new Spatial[_currentList.length];
    }

    public void add(final Spatial spatial) {
        spatial._queueDistance = Double.NEGATIVE_INFINITY;
        if (_currentListSize >= _currentList.length) {
            // grow if necessary
            final Spatial[] temp = new Spatial[_currentListSize * 2];
            System.arraycopy(_currentList, 0, temp, 0, _currentListSize);
            _currentList = temp;
            if (_tempList.length < temp.length) {
                _tempList = new Spatial[temp.length];
            }
        }
        _currentList[_currentListSize++] = spatial;
    }

    public void remove(final Spatial spatial) {
        int index = 0;
        for (int i = 0; i < _currentListSize; i++) {
            if (_currentList[index] == spatial) {
                break;
            }
            index++;
        }
        for (int i = index; i < _currentListSize - 1; i++) {
            _currentList[i] = _currentList[i + 1];
        }

        _currentListSize--;
    }

    public void clear() {
        if (_currentListSize > 0) {
            Arrays.fill(_currentList, 0, _currentListSize, null);
            _currentListSize = 0;
        }
    }

    public void render(final Renderer renderer) {
        for (int i = 0; i < _currentListSize; i++) {
            _currentList[i].draw(renderer);
        }
    }

    public void sort() {
        // only sort if we have more than one item in our bucket.
        if (_currentListSize > 1) {
            if (_currentListSize <= SortUtil.SHELL_SORT_THRESHOLD) {
                // shell sort
                SortUtil.shellSort(_currentList, 0, _currentListSize - 1, _comparator);
            } else {
                // copy in our list for use in the merge sort.
                System.arraycopy(_currentList, 0, _tempList, 0, _currentListSize);

                // merge sort
                SortUtil.msort(_tempList, _currentList, 0, _currentListSize - 1, _comparator);

                // null fill to remove references
                Arrays.fill(_tempList, 0, _currentListSize, null);
            }
        }
    }

    public void pushBucket() {
        _listStack.push(_currentList);
        if (_listStackPool.isEmpty()) {
            _currentList = new Spatial[32];
        } else {
            _currentList = _listStackPool.pop();
        }

        _listSizeStack.push(_currentListSize);
        _currentListSize = 0;
    }

    public void popBucket() {
        if (_currentList != null) {
            _listStackPool.push(_currentList);
        }
        _currentList = _listStack.pop();
        _currentListSize = _listSizeStack.pop();
    }

    /**
     * Calculates the distance from a spatial to the camera. Distance is a squared distance.
     * 
     * @param spat
     *            Spatial to check distance.
     * @return Distance from Spatial to current context's camera.
     */
    protected double distanceToCam(final Spatial spat) {
        if (spat._queueDistance != Double.NEGATIVE_INFINITY) {
            return spat._queueDistance;
        }

        final Camera cam = Camera.getCurrentCamera();

        if (spat.getWorldBound() != null && Vector3.isValid(spat.getWorldBound().getCenter())) {
            spat._queueDistance = spat.getWorldBound().distanceToEdge(cam.getLocation());
        } else {
            final ReadOnlyVector3 spatPosition = spat.getWorldTranslation();
            if (!Vector3.isValid(spatPosition)) {
                spat._queueDistance = Double.POSITIVE_INFINITY;
            } else {
                spat._queueDistance = cam.getLocation().distance(spatPosition);
            }
        }

        return spat._queueDistance;
    }

}
