/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.scenegraph.RenderDelegate;

/**
 * Node defines an internal node of a scene graph. The internal node maintains a collection of children and handles
 * merging said children into a single bound to allow for very fast culling of multiple nodes. Node allows for any
 * number of children to be attached.
 */
public class Node extends Spatial {
    private static final Logger logger = Logger.getLogger(Node.class.getName());

    /** This node's children. */
    protected final List<Spatial> _children;

    /**
     * Constructs a new Spatial.
     */
    public Node() {
        super();
        _children = Collections.synchronizedList(new ArrayList<Spatial>(1));
    }

    /**
     * Constructs a new <code>Node</code> with a given name.
     * 
     * @param name
     *            the name of the node. This is required for identification purposes.
     */
    public Node(final String name) {
        this(name, Collections.synchronizedList(new ArrayList<Spatial>(1)));
    }

    /**
     * Constructs a new <code>Node</code> with a given name.
     * 
     * @param name
     *            the name of the node. This is required for identification purposes.
     * @param children
     *            the list to use for storing children. Defaults to a synchronized ArrayList, but using this
     *            constructor, you can select a different kind of list.
     */
    public Node(final String name, final List<Spatial> children) {
        super(name);
        _children = children;
    }

    /**
     * 
     * <code>attachChild</code> attaches a child to this node. This node becomes the child's parent. The current number
     * of children maintained is returned. <br>
     * If the child already had a parent it is detached from that former parent.
     * 
     * @param child
     *            the child to attach to this node.
     * @return the number of children maintained by this node.
     */
    public int attachChild(final Spatial child) {
        if (child != null) {
            if (child == this || (child instanceof Node && hasAncestor((Node) child))) {
                throw new IllegalArgumentException("Child is already part of this hierarchy.");
            }
            if (child.getParent() != this) {
                if (child.getParent() != null) {
                    child.getParent().detachChild(child);
                }
                child.setParent(this);
                _children.add(child);
                child.markDirty(DirtyType.Attached);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Child (" + child.getName() + ") attached to this" + " node (" + getName() + ")");
                }
            }
        }

        return _children.size();
    }

    /**
     * 
     * <code>attachChildAt</code> attaches a child to this node at an index. This node becomes the child's parent. The
     * current number of children maintained is returned. <br>
     * If the child already had a parent it is detached from that former parent.
     * 
     * @param child
     *            the child to attach to this node.
     * @return the number of children maintained by this node.
     */
    public int attachChildAt(final Spatial child, final int index) {
        if (child != null) {
            if (child == this || (child instanceof Node && hasAncestor((Node) child))) {
                throw new IllegalArgumentException("Child is already part of this hierarchy.");
            }
            if (child.getParent() != this) {
                if (child.getParent() != null) {
                    child.getParent().detachChild(child);
                }
                child.setParent(this);
                _children.add(index, child);
                child.markDirty(DirtyType.Attached);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Child (" + child.getName() + ") attached to this" + " node (" + getName() + ")");
                }
            }
        }

        return _children.size();
    }

    /**
     * <code>detachChild</code> removes a given child from the node's list. This child will no longe be maintained.
     * 
     * @param child
     *            the child to remove.
     * @return the index the child was at. -1 if the child was not in the list.
     */
    public int detachChild(final Spatial child) {
        if (child == null) {
            return -1;
        }
        if (child.getParent() == this) {
            final int index = _children.indexOf(child);
            if (index != -1) {
                detachChildAt(index);
            }
            return index;
        }

        return -1;
    }

    /**
     * <code>detachChild</code> removes a given child from the node's list. This child will no longe be maintained. Only
     * the first child with a matching name is removed.
     * 
     * @param childName
     *            the child to remove.
     * @return the index the child was at. -1 if the child was not in the list.
     */
    public int detachChildNamed(final String childName) {
        if (childName == null) {
            return -1;
        }
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);
            if (childName.equals(child.getName())) {
                detachChildAt(i);
                return i;
            }
        }
        return -1;
    }

    /**
     * 
     * <code>detachChildAt</code> removes a child at a given index. That child is returned for saving purposes.
     * 
     * @param index
     *            the index of the child to be removed.
     * @return the child at the supplied index.
     */
    public Spatial detachChildAt(final int index) {
        final Spatial child = _children.remove(index);
        if (child != null) {
            child.setParent(null);
            markDirty(child, DirtyType.Detached);
            if (child.getListener() != null) {
                child.setListener(null);
            }
            if (logger.isLoggable(Level.INFO)) {
                logger.fine("Child removed.");
            }
        }
        return child;
    }

    /**
     * 
     * <code>detachAllChildren</code> removes all children attached to this node.
     */
    public void detachAllChildren() {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            detachChildAt(i);
        }
        logger.fine("All children removed.");
    }

    /**
     * Get the index of the specified spatial.
     * 
     * @param sp
     *            spatial to retrieve index for.
     * @return the index
     */
    public int getChildIndex(final Spatial sp) {
        return _children.indexOf(sp);
    }

    /**
     * Returns all children to this node.
     * 
     * @return a list containing all children to this node
     */
    public List<Spatial> getChildren() {
        return _children;
    }

    /**
     * Swaps two children.
     * 
     * @param index1
     * @param index2
     */
    public void swapChildren(final int index1, final int index2) {
        final Spatial c2 = _children.get(index2);
        final Spatial c1 = _children.remove(index1);
        _children.add(index1, c2);
        _children.remove(index2);
        _children.add(index2, c1);
    }

    @Override
    protected void updateChildren(final double time) {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial pkChild = getChild(i);
            if (pkChild != null) {
                pkChild.updateGeometricState(time, false);
            }
        }
    }

    /**
     * 
     * <code>getChild</code> returns a child at a given index.
     * 
     * @param i
     *            the index to retrieve the child from.
     * @return the child at a specified index.
     */
    public Spatial getChild(final int i) {
        if (_children.size() > (i)) {
            return _children.get(i);
        } else {
            return null;
        }
    }

    /**
     * <code>getChild</code> returns the first child found with exactly the given name (case sensitive.) If our children
     * are Nodes, we will search their children as well.
     * 
     * @param name
     *            the name of the child to retrieve. If null, we'll return null.
     * @return the child if found, or null.
     */
    public Spatial getChild(final String name) {
        if (name == null) {
            return null;
        }
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);
            if (name.equals(child.getName())) {
                return child;
            } else if (child instanceof Node) {
                final Spatial out = ((Node) child).getChild(name);
                if (out != null) {
                    return out;
                }
            }
        }
        return null;
    }

    /**
     * determines if the provided Spatial is contained in the children list of this node.
     * 
     * @param spat
     *            the child object to look for.
     * @return true if the object is contained, false otherwise.
     */
    public boolean hasChild(final Spatial spat) {
        if (_children.contains(spat)) {
            return true;
        }

        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);
            if (child instanceof Node && ((Node) child).hasChild(spat)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * <code>getNumberOfChildren</code> returns the number of children this node maintains.
     * 
     * @return the number of children this node maintains.
     */
    public int getNumberOfChildren() {
        return _children.size();
    }

    @Override
    protected void propagateDirtyDown(final EnumSet<DirtyType> dirtyTypes) {
        super.propagateDirtyDown(dirtyTypes);

        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);

            child.propagateDirtyDown(dirtyTypes);
        }
    }

    @Override
    public void updateWorldTransform(final boolean recurse) {
        super.updateWorldTransform(recurse);

        if (recurse) {
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                _children.get(i).updateWorldTransform(true);
            }
        }
    }

    @Override
    protected void updateWorldRenderStates(final boolean recurse, final RenderState.StateStack stack) {
        super.updateWorldRenderStates(recurse, stack);

        if (recurse) {
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                _children.get(i).updateWorldRenderStates(true, stack);
            }
        }
    }

    /**
     * <code>draw</code> calls the onDraw method for each child maintained by this node.
     * 
     * @see com.ardor3d.scenegraph.Spatial#draw(com.ardor3d.renderer.Renderer)
     * @param r
     *            the renderer to draw to.
     */
    @Override
    public void draw(final Renderer r) {

        final RenderDelegate delegate = getCurrentRenderDelegate();
        if (delegate == null) {
            Spatial child;
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                child = _children.get(i);
                if (child != null) {
                    child.onDraw(r);
                }
            }
        } else {
            // Queue as needed
            if (!r.isProcessingQueue()) {
                if (r.checkAndAdd(this)) {
                    return;
                }
            }

            delegate.render(this, r);
        }
    }

    /**
     * <code>updateWorldBound</code> merges the bounds of all the children maintained by this node. This will allow for
     * faster culling operations.
     * 
     * @see com.ardor3d.scenegraph.Spatial#updateWorldBound(boolean)
     */
    @Override
    public void updateWorldBound(final boolean recurse) {
        BoundingVolume worldBound = null;
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);
            if (child != null) {
                if (recurse) {
                    child.updateWorldBound(true);
                }
                if (worldBound != null) {
                    // merge current world bound with child world bound
                    worldBound.mergeLocal(child.getWorldBound());

                    // simple check to catch NaN issues
                    if (!Vector3.isValid(worldBound.getCenter())) {
                        throw new Ardor3dException("WorldBound center is invalid after merge between " + this + " and "
                                + child);
                    }
                } else {
                    // set world bound to first non-null child world bound
                    if (child.getWorldBound() != null) {
                        worldBound = child.getWorldBound().clone(_worldBound);
                    }
                }
            }
        }
        _worldBound = worldBound;
        clearDirty(DirtyType.Bounding);
    }

    @Override
    public void acceptVisitor(final Visitor visitor, final boolean preexecute) {
        if (preexecute) {
            visitor.visit(this);
        }

        Spatial child;
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            child = _children.get(i);
            if (child != null) {
                child.acceptVisitor(visitor, preexecute);
            }
        }

        if (!preexecute) {
            visitor.visit(this);
        }
    }

    @Override
    public void sortLights() {
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial pkChild = getChild(i);
            if (pkChild != null) {
                pkChild.sortLights();
            }
        }
    }

    @Override
    public Node makeCopy(final boolean shareGeometricData) {
        // get copy of basic spatial info
        final Node node = (Node) super.makeCopy(shareGeometricData);

        // add copy of children
        for (final Spatial child : getChildren()) {
            final Spatial copy = child.makeCopy(shareGeometricData);
            node.attachChild(copy);
        }

        // return
        return node;
    }

    @Override
    public Node makeInstanced() {
        // get copy of basic spatial info
        final Node node = (Node) super.makeInstanced();
        // add copy of children
        for (final Spatial child : getChildren()) {
            final Spatial copy = child.makeInstanced();
            node.attachChild(copy);
        }
        return node;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Node> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.writeSavableList(new ArrayList<Spatial>(_children), "children", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final List<Spatial> cList = capsule.readSavableList("children", null);
        _children.clear();
        if (cList != null) {
            _children.addAll(cList);
        }

        // go through children and set parent to this node
        for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
            final Spatial child = _children.get(i);
            child._parent = this;
        }
    }
}
