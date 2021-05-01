/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Vector2;
import com.ardor3d.scenegraph.Spatial;

/**
 * <p>
 * This layout arranges components based on anchors - descriptions of hard connections between two
 * components. These connections are specified per component, where each component may specify a
 * dependent relationship with exactly one other component. Each relationship consists of the two
 * dependent components, the alignment points on each, and optional x/y offsets.
 * </p>
 * <p>
 * As an example, the following would setup labelA in the top left corner of the container, 5 pixels
 * from the top and 5 pixels from the bottom. Directly below that (5 pixels from the bottom of
 * labelA) is labelB, left aligned to labelA:
 *
 * <pre>
 * UIContainer container;
 * UILabel labelA, labelB
 *
 * ...
 *
 * container.setLayout(new AnchorLayout());
 * labelA.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, container, Alignment.TOP_LEFT, 5, -5));
 * labelB.setLayoutData(new AnchorLayoutData(Alignment.TOP_LEFT, labelA, Alignment.BOTTOM_LEFT, 0, -5));
 * </pre>
 *
 * </p>
 *
 * @see AnchorLayoutData
 */
public class AnchorLayout extends UILayout {

  /** map used to track anchor relationship during layout. */
  private final Map<UIComponent, AnchorRecord> _records = new HashMap<>();

  // Various min/max values set and used during a layout operation.
  private int _maxX = 0;
  private int _maxY = 0;
  private int _minX = 0;
  private int _minY = 0;

  @Override
  public void layoutContents(final UIContainer container) {
    _maxX = 0;
    _maxY = 0;
    _minX = 0;
    _minY = 0;

    // clear our records
    _records.clear();

    // add a record for the container
    _records.put(container, new AnchorRecord());

    // go through all children of a container
    for (int x = container.getNumberOfChildren(); --x >= 0;) {
      // set them all to relative position 0,0
      final Spatial child = container.getChild(x);
      if (child instanceof UIComponent) {
        final UIComponent childComp = (UIComponent) child;

        // add an anchor for the component, if missing.
        if (_records.get(childComp) == null) {
          _records.put(childComp, new AnchorRecord());
        }

        // add them to the records container by their dependencies, if any.
        if (childComp.getLayoutData() instanceof AnchorLayoutData) {
          // resets translation to 0 so we can addTranslation in applyAnchor
          childComp.setTranslation(0, 0, childComp.getTranslation().getZ());

          final AnchorLayoutData layData = (AnchorLayoutData) childComp.getLayoutData();
          AnchorRecord aRecord = _records.get(layData.getParent());
          if (aRecord == null) {
            aRecord = new AnchorRecord();
            _records.put(layData.getParent(), aRecord);
          }
          aRecord.dependants.add(childComp);
        }
      }
    }

    // Apply anchors starting at the container and working down. After traversal, remove the visited
    // records and run
    // through again.
    visit(container);
    removeVisited();
    while (!_records.isEmpty()) {
      visit((UIComponent) _records.keySet().toArray()[0]);
      removeVisited();
    }

    // clear our records to allow potential cleanup of referenced components
    _records.clear();
  }

  @Override
  public void updateMinimumSizeFromContents(final UIContainer container) {
    layoutContents(container);
    container.setLayoutMinimumContentSize(_maxX - _minX, _maxY - _minY);
  }

  private void visit(final UIComponent toVisit) {
    // get the anchor record (if any) for this component
    final AnchorRecord aRecord = _records.get(toVisit);
    if (aRecord != null) {
      final Rectangle2 rect = new Rectangle2();
      for (int x = aRecord.dependants.size(); --x >= 0;) {
        final UIComponent dep = aRecord.dependants.get(x);
        if (dep.getLayoutData() instanceof AnchorLayoutData) {
          dep.getRelativeComponentBounds(rect);
          AnchorLayout.applyAnchor(dep, (AnchorLayoutData) dep.getLayoutData());

          // update min/max
          final int posX = Math.round(dep.getTranslation().getXf()) + rect.getWidth() - rect.getX();
          final int negX = Math.round(dep.getTranslation().getXf()) - rect.getX();
          final int posY = Math.round(dep.getTranslation().getYf()) + rect.getHeight() - rect.getY();
          final int negY = Math.round(dep.getTranslation().getYf()) - rect.getY();
          if (posX > _maxX) {
            _maxX = posX;
          }
          if (posY > _maxY) {
            _maxY = posY;
          }
          if (negX < _minX) {
            _minX = negX;
          }
          if (negY < _minY) {
            _minY = negY;
          }
        }

        visit(dep);
      }

      aRecord.visited = true;
    }
  }

  private static void applyAnchor(final UIComponent comp, final AnchorLayoutData layData) {
    final Vector2 offsetsA = AnchorLayout.getOffsets(comp, layData.getMyPoint(), null);
    final Vector2 offsetsB = AnchorLayout.getOffsets(layData.getParent(), layData.getParentPoint(), null);
    comp.addTranslation(offsetsB.getX() - offsetsA.getX() + layData.getXOffset(),
        offsetsB.getY() - offsetsA.getY() + layData.getYOffset(), 0);
    if (!comp.hasAncestor(layData.getParent())) {
      comp.addTranslation(layData.getParent().getTranslation());
    }
  }

  private static Vector2 getOffsets(final UIComponent comp, final Alignment point, Vector2 store) {
    if (store == null) {
      store = new Vector2();
    }

    final Rectangle2 rect = new Rectangle2();
    comp.getRelativeComponentBounds(rect);
    switch (point) {
      case TOP_LEFT:
        store.set(rect.getX(), rect.getHeight() + rect.getY());
        break;
      case TOP:
        store.set(rect.getWidth() / 2f + rect.getX(), rect.getHeight() + rect.getY());
        break;
      case TOP_RIGHT:
        store.set(rect.getWidth() + rect.getX(), rect.getHeight() + rect.getY());
        break;
      case LEFT:
        store.set(rect.getX(), rect.getHeight() / 2f + rect.getY());
        break;
      case MIDDLE:
        store.set(rect.getWidth() / 2f + rect.getX(), rect.getHeight() / 2f + rect.getY());
        break;
      case RIGHT:
        store.set(rect.getWidth() + rect.getX(), rect.getHeight() / 2f + rect.getY());
        break;
      case BOTTOM_LEFT:
        store.set(rect.getX(), rect.getY());
        break;
      case BOTTOM:
        store.set(rect.getWidth() / 2f + rect.getX(), rect.getY());
        break;
      case BOTTOM_RIGHT:
        store.set(rect.getWidth() + rect.getX(), rect.getY());
        break;
    }

    return store;
  }

  private void removeVisited() {
    final Iterator<UIComponent> it = _records.keySet().iterator();
    while (it.hasNext()) {
      final UIComponent comp = it.next();
      final AnchorRecord aRecord = _records.get(comp);
      if (aRecord != null && aRecord.visited) {
        it.remove();
      }
    }
  }

  private class AnchorRecord {
    private transient boolean visited = false;
    private transient final ArrayList<UIComponent> dependants = new ArrayList<>();
  }
}
