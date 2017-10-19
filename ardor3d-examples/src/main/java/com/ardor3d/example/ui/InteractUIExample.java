/**
 * Copyright (c) 2008-2017 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.ui;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.InteractManager.UpdateLogic;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.filter.PlaneBoundaryFilter;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.extension.interact.widget.BasicFilterList;
import com.ardor3d.extension.interact.widget.IFilterList;
import com.ardor3d.extension.interact.widget.MovePlanarWidget;
import com.ardor3d.extension.interact.widget.MovePlanarWidget.MovePlane;
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIComboBox;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.event.SelectionListener;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.model.DefaultComboBoxModel;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.Key;
import com.ardor3d.input.jogl.JoglNewtKeyboardWrapper;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Tube;
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.google.common.collect.Lists;

/**
 * An example illustrating the use of the interact framework.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.interact.InteractUIExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/interact_InteractUIExample.jpg", //
maxHeapMemory = 64)
public class InteractUIExample extends ExampleBase {

    private UIHud hud;
    private InteractManager manager;
    private MovePlanarWidget moveWidget;
    private InsertMarkerUIWidget insertWidget;
    private ColorSelectUIWidget colorWidget;
    private PulseControlUIWidget pulseWidget;

    final Vector3 tempVec = new Vector3();

    public static void main(final String[] args) {
        start(InteractUIExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        manager.update(timer);
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        super.renderExample(renderer);
        manager.render(renderer);
        renderer.renderBuckets();
        hud.draw(renderer);
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Interact Example");
        hud = new UIHud(_canvas);

        final Camera camera = _canvas.getCanvasRenderer().getCamera();
        camera.setLocation(15, 11, -9);
        camera.lookAt(0, 0, 0, Vector3.UNIT_Y);

        // setup our interact controls
        addControls();

        // create a floor to act as a reference.
        addFloor();

        // create a few way-markers to start things off
        initPath();

        manager.addUpdateLogic(new UpdateLogic() {

            @Override
            public void update(final double time, final InteractManager manager) {
                manager.fireTargetDataUpdated();
            }
        });

        // disable auto-repeat in jogl, if we're using that, so that spacebar can be held down
        if (_physicalLayer.getKeyboardWrapper() instanceof JoglNewtKeyboardWrapper) {
            final JoglNewtKeyboardWrapper key = (JoglNewtKeyboardWrapper) _physicalLayer.getKeyboardWrapper();
            key.setSkipAutoRepeatEvents(true);
        }
    }

    private void addFloor() {
        final Box floor = new Box("floor", Vector3.ZERO, 100, 5, 100);
        floor.setTranslation(0, -5, 0);
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("models/obj/pitcher.jpg", Texture.MinificationFilter.Trilinear, true));
        floor.setRenderState(ts);
        floor.getSceneHints().setPickingHint(PickingHint.Pickable, false);
        floor.setModelBound(new BoundingBox());
        _root.attachChild(floor);
        _root.updateGeometricState(0);
    }

    LinkedList<Spatial> path = Lists.newLinkedList();

    private void initPath() {
        final Spatial marker1 = createMarker();
        marker1.setName("marker1");
        final Spatial marker2 = createMarkerAfter(marker1);
        marker2.setName("marker2");
        createMarkerBefore(marker2).setName("marker3");

        // auto select the joint
        _root.updateGeometricState(0);
        manager.setSpatialTarget(marker1);
    }

    private void removeMarker(final Spatial ref) {
        final int index = path.indexOf(ref);
        if (path.remove(ref)) {
            ref.removeFromParent();
            manager.setSpatialTarget(null);
            if (path.size() == 0) {
                manager.setSpatialTarget(null);
            } else if (path.size() <= index) {
                manager.setSpatialTarget(path.get(index - 1));
            } else {
                manager.setSpatialTarget(path.get(index));
            }
        }
    }

    private Spatial createMarker() {
        final Tube t = new Tube("marker", 1, 0.25, .25);
        t.setModelBound(new BoundingBox());
        t.updateGeometricState(0);
        t.addTranslation(0, .25, 0);
        t.getSceneHints().setPickingHint(PickingHint.Pickable, true);
        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.AmbientAndDiffuse);
        t.setRenderState(ms);
        final MarkerData data = new MarkerData();
        t.setUserData(data);
        t.addController(new SpatialController<Spatial>() {
            private double _scaleTime = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                final double pulseSpeed = ((MarkerData) t.getUserData()).pulseSpeed;
                if (pulseSpeed != 0.0) {
                    _scaleTime = _scaleTime + (_timer.getTimePerFrame() * pulseSpeed);
                    final double scale = MathUtils.sin(_scaleTime) * .99 + 1.0;
                    t.setScale(scale);
                } else {
                    t.setScale(1.0);
                }
            }
        });
        _root.attachChild(t);
        path.add(t);
        return t;
    }

    private Spatial createMarkerAfter(final Spatial ref) {
        final Spatial marker = createMarker();

        // copy transform (orientation and position) of ref
        marker.setTranslation(ref.getTranslation());
        marker.setRotation(ref.getRotation());

        // check if we're moving into place between two points, or just after last point
        final int indexOfRef = path.indexOf(ref);
        if (indexOfRef == path.size() - 2) {
            // we're adding after last point, so no need to move in list
            // just translate us in the forward z direction of the last node
            final Vector3 fwd = marker.getRotation().applyPost(Vector3.UNIT_Z, null);
            marker.addTranslation(fwd.multiplyLocal(8.0));
        } else {
            // we're adding a point between two others - get our other ref
            final Spatial postRef = path.get(indexOfRef + 1);

            // move new marker into list between the other two points
            path.remove(marker);
            path.add(indexOfRef + 1, marker);

            // translate and orient between points
            marker.setTranslation(ref.getTranslation().add(postRef.getTranslation(), null).divideLocal(2.0));

            final Quaternion rotHelper1 = new Quaternion();
            rotHelper1.fromRotationMatrix(ref.getRotation());
            final Quaternion rotHelper2 = new Quaternion();
            rotHelper2.fromRotationMatrix(postRef.getRotation());
            marker.setRotation(rotHelper1.slerp(rotHelper2, .5, null));
        }

        manager.setSpatialTarget(marker);

        return marker;
    }

    private Spatial createMarkerBefore(final Spatial ref) {
        final int indexOfRef = path.indexOf(ref);
        if (indexOfRef <= 0) {
            return null;
        }

        return createMarkerAfter(path.get(indexOfRef - 1));
    }

    private void addControls() {
        // create our manager
        manager = new InteractManager(new MarkerState());
        manager.setupInput(_canvas, _physicalLayer, _logicalLayer);

        hud.setupInput(_physicalLayer, manager.getLogicalLayer());
        hud.setMouseManager(_mouseManager);

        final BasicFilterList filterList = new BasicFilterList();

        // add some widgets.
        insertWidget = new InsertMarkerUIWidget(filterList);
        manager.addWidget(insertWidget);

        colorWidget = new ColorSelectUIWidget(filterList);
        manager.addWidget(colorWidget);

        pulseWidget = new PulseControlUIWidget(filterList);
        manager.addWidget(pulseWidget);

        moveWidget = new MovePlanarWidget(filterList).withPlane(MovePlane.XZ).withDefaultHandle(.33, .33,
                ColorRGBA.YELLOW);
        manager.addWidget(moveWidget);

        // set the default as current
        manager.setActiveWidget(moveWidget);

        // add triggers to change which widget is active
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyReleasedCondition(Key.ONE), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        manager.setActiveWidget(moveWidget);
                    }
                }));
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyReleasedCondition(Key.TWO), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        manager.setActiveWidget(insertWidget);
                    }
                }));
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyReleasedCondition(Key.THREE), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        manager.setActiveWidget(colorWidget);
                    }
                }));
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyReleasedCondition(Key.FOUR), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        manager.setActiveWidget(pulseWidget);
                    }
                }));
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyPressedCondition(Key.SPACE), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        showMenu();
                    }
                }));
        manager.getLogicalLayer().registerTrigger(
                new InputTrigger(new KeyReleasedCondition(Key.SPACE), new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        hideMenu();
                    }
                }));

        // add some filters
        manager.addFilter(new PlaneBoundaryFilter(new Plane(Vector3.UNIT_Y, 0)));
    }

    UIPieMenu menu;

    protected void showMenu() {
        if (menu == null) {
            menu = new UIPieMenu(hud, 70, 200);
            menu.setTotalArcLength(MathUtils.PI);
            menu.setStartAngle(-90 * MathUtils.DEG_TO_RAD);
            // menu.setRotation(new Matrix3().fromAngleAxis(MathUtils.DEG_TO_RAD * 60, Vector3.UNIT_Z));

            final UIPieMenu addPie = new UIPieMenu(hud);
            menu.addItem(new UIPieMenuItem("Add Node...", null, addPie, 100));
            addPie.addItem(new UIPieMenuItem("Before", null, true, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    createMarkerBefore(spat);
                }
            }));
            addPie.addItem(new UIPieMenuItem("After", null, true, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    createMarkerAfter(spat);
                }
            }));

            final UIPieMenu remPie = new UIPieMenu(hud);
            remPie.addItem(new UIPieMenuItem("Node", null, true, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    removeMarker(spat);
                }
            }));
            menu.addItem(new UIPieMenuItem("Delete...", null, remPie, 100));

            menu.setCenterItem(new UIPieMenuItem("Cancel", null, true, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    return;
                }
            }));

            menu.addItem(new UIPieMenuItem("Reset Node", null, true, new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                }
            }));
            menu.updateMinimumSizeFromContents();
            menu.layout();
        }

        hud.closePopupMenus();

        final Spatial spat = manager.getSpatialTarget();
        if (spat == null) {
            return;
        }

        hud.showSubPopupMenu(menu);

        tempVec.zero();
        tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
        tempVec.setZ(0);
        menu.showAt((int) tempVec.getX(), (int) tempVec.getY());
        _mouseManager.setPosition((int) tempVec.getX(), (int) tempVec.getY());
        if (menu.getCenterItem() != null) {
            menu.getCenterItem().mouseEntered((int) tempVec.getX(), (int) tempVec.getY(), null);
        }
    }

    protected void hideMenu() {
        hud.closePopupMenus();
    }

    @Override
    protected void processPicks(final PrimitivePickResults pickResults) {
        final PickData pick = pickResults.findFirstIntersectingPickData();
        if (pick != null) {
            final Pickable target = pick.getTarget();
            if (target instanceof Spatial) {
                manager.setSpatialTarget((Spatial) target);
                System.out.println("Setting target to: " + ((Spatial) target).getName());
                return;
            }
        }
        manager.setSpatialTarget(null);
    }

    class InsertMarkerUIWidget extends AbstractInteractWidget {

        UIPanel uiPanel;

        public InsertMarkerUIWidget(final IFilterList filterList) {
            super(filterList);

            createFrame();
        }

        private void createFrame() {

            final RowLayout rowLay = new RowLayout(true);
            final UIPanel centerPanel = new UIPanel(rowLay);
            centerPanel.setBackdrop(new EmptyBackdrop());
            centerPanel.setLayoutData(BorderLayoutData.CENTER);

            AddButton(centerPanel, "+", new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    createMarkerBefore(spat);
                }
            });

            AddButton(centerPanel, "-", new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    removeMarker(spat);
                }
            });

            AddButton(centerPanel, "+", new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    final Spatial spat = manager.getSpatialTarget();
                    if (spat == null) {
                        return;
                    }
                    createMarkerAfter(spat);
                }
            });

            uiPanel = new UIPanel();
            uiPanel.add(centerPanel);
            uiPanel.pack();

            _handle = uiPanel;
        }

        private void AddButton(final UIContainer parent, final String label, final ActionListener actionListener) {
            final UIButton button = new UIButton(label);
            button.setPadding(Insets.EMPTY);
            button.setMargin(Insets.EMPTY);
            button.setMaximumContentSize(22, 22);
            button.setMinimumContentSize(22, 22);
            button.addActionListener(actionListener);
            parent.add(button);
        }

        @Override
        public void render(final Renderer renderer, final InteractManager manager) {
            final Spatial spat = manager.getSpatialTarget();
            if (spat == null) {
                return;
            }

            tempVec.zero();
            tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
            tempVec.setZ(0);
            tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -10, 0);
            _handle.setTranslation(tempVec);
            _handle.updateWorldTransform(true);
        }

        @Override
        public void receivedControl(final InteractManager manager) {
            super.receivedControl(manager);
            final Spatial spat = manager.getSpatialTarget();
            if (spat != null) {
                hud.add(uiPanel);
            }
        }

        @Override
        public void lostControl(final InteractManager manager) {
            super.lostControl(manager);
            hud.remove(uiPanel);
        }

        @Override
        public void targetChanged(final InteractManager manager) {
            super.targetChanged(manager);
            if (manager.getActiveWidget() == this) {
                final Spatial spat = manager.getSpatialTarget();
                if (spat == null) {
                    hud.remove(uiPanel);
                } else {
                    hud.add(uiPanel);
                }
            }
        }
    }

    class ColorSelectUIWidget extends AbstractInteractWidget {

        UIPanel uiPanel;
        ColorRGBA unconsumedColor;

        public ColorSelectUIWidget(final IFilterList filterList) {
            super(filterList);

            createFrame();
        }

        private void createFrame() {

            final UIPanel centerPanel = new UIPanel();
            centerPanel.setBackdrop(new EmptyBackdrop());
            centerPanel.setLayoutData(BorderLayoutData.CENTER);

            final UIComboBox combo = new UIComboBox(new DefaultComboBoxModel("White", "Black", "Red", "Green", "Blue",
                    "Yellow", "Magenta", "Cyan"));
            combo.setMinimumContentWidth(100);
            combo.addSelectionListener(new SelectionListener<UIComboBox>() {
                @Override
                public void selectionChanged(final UIComboBox component, final Object newValue) {
                    try {
                        final Field field = ColorRGBA.class.getField(newValue.toString().toUpperCase());
                        final ColorRGBA color = (ColorRGBA) field.get(null);
                        if (manager.getSpatialState() instanceof MarkerState) {
                            unconsumedColor = color;
                        }

                    } catch (final Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            centerPanel.add(combo);

            uiPanel = new UIPanel();
            uiPanel.add(centerPanel);
            uiPanel.pack();

            _handle = uiPanel;
        }

        @Override
        public void render(final Renderer renderer, final InteractManager manager) {
            final Spatial spat = manager.getSpatialTarget();
            if (spat == null) {
                return;
            }

            tempVec.zero();
            tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
            tempVec.setZ(0);
            tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -20, 0);
            _handle.setTranslation(tempVec);
            _handle.updateWorldTransform(true);
        }

        @Override
        public void receivedControl(final InteractManager manager) {
            super.receivedControl(manager);
            final Spatial spat = manager.getSpatialTarget();
            if (spat != null) {
                hud.add(uiPanel);
            }
        }

        @Override
        public void lostControl(final InteractManager manager) {
            super.lostControl(manager);
            hud.remove(uiPanel);
        }

        @Override
        public void processInput(final Canvas source, final TwoInputStates inputStates,
                final AtomicBoolean inputConsumed, final InteractManager manager) {
            super.processInput(source, inputStates, inputConsumed, manager);
            if (unconsumedColor != null) {
                ((MarkerState) manager.getSpatialState()).data.color.set(unconsumedColor);
                inputConsumed.set(true);
                unconsumedColor = null;
            }
        }

        @Override
        public void targetChanged(final InteractManager manager) {
            super.targetChanged(manager);
            if (manager.getActiveWidget() == this) {
                final Spatial spat = manager.getSpatialTarget();
                if (spat == null) {
                    hud.remove(uiPanel);
                } else {
                    hud.add(uiPanel);
                }
            }
        }
    }

    class PulseControlUIWidget extends AbstractInteractWidget {

        UIPanel uiPanel;
        Double unconsumedPulse;

        public PulseControlUIWidget(final IFilterList filterList) {
            super(filterList);

            createFrame();
        }

        private void createFrame() {

            final UIPanel centerPanel = new UIPanel();
            centerPanel.setBackdrop(new EmptyBackdrop());
            centerPanel.setLayoutData(BorderLayoutData.CENTER);

            final UISlider slider = new UISlider(Orientation.Horizontal, 0, 100, 0);
            slider.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    if (manager.getSpatialTarget() == null) {
                        return;
                    }
                    if (manager.getSpatialState() instanceof MarkerState) {
                        unconsumedPulse = slider.getValue() * 0.05;
                    }
                }
            });
            slider.setMinimumContentWidth(100);
            centerPanel.add(slider);

            uiPanel = new UIPanel();
            uiPanel.add(centerPanel);
            uiPanel.pack();

            _handle = uiPanel;
        }

        @Override
        public void render(final Renderer renderer, final InteractManager manager) {
            final Spatial spat = manager.getSpatialTarget();
            if (spat == null) {
                return;
            }

            tempVec.zero();
            tempVec.set(Camera.getCurrentCamera().getScreenCoordinates(spat.getWorldTransform().applyForward(tempVec)));
            tempVec.setZ(0);
            tempVec.subtractLocal(uiPanel.getContentWidth() / 2, -20, 0);
            _handle.setTranslation(tempVec);
            _handle.updateWorldTransform(true);
        }

        @Override
        public void receivedControl(final InteractManager manager) {
            super.receivedControl(manager);
            final Spatial spat = manager.getSpatialTarget();
            if (spat != null) {
                hud.add(uiPanel);
            }
        }

        @Override
        public void lostControl(final InteractManager manager) {
            super.lostControl(manager);
            hud.remove(uiPanel);
        }

        @Override
        public void processInput(final Canvas source, final TwoInputStates inputStates,
                final AtomicBoolean inputConsumed, final InteractManager manager) {
            super.processInput(source, inputStates, inputConsumed, manager);
            if (unconsumedPulse != null) {
                ((MarkerState) manager.getSpatialState()).data.pulseSpeed = unconsumedPulse;
                inputConsumed.set(true);
                unconsumedPulse = null;
            }
        }

        @Override
        public void targetChanged(final InteractManager manager) {
            super.targetChanged(manager);
            if (manager.getActiveWidget() == this) {
                final Spatial spat = manager.getSpatialTarget();
                if (spat == null) {
                    hud.remove(uiPanel);
                } else {
                    hud.add(uiPanel);
                }
            }
        }
    }

    class MarkerData {
        public ColorRGBA color = new ColorRGBA(ColorRGBA.WHITE);
        public double pulseSpeed;

        public void copy(final MarkerData source) {
            color.set(source.color);
            pulseSpeed = source.pulseSpeed;
        }
    }

    class MarkerState extends SpatialState {
        public final MarkerData data = new MarkerData();

        @Override
        public void applyState(final Spatial target) {
            super.applyState(target);
            if (target.getUserData() instanceof MarkerData) {
                final MarkerData tData = (MarkerData) target.getUserData();
                if (tData.pulseSpeed != data.pulseSpeed) {
                    tData.pulseSpeed = data.pulseSpeed;
                }

                if (!tData.color.equals(data.color)) {
                    tData.color.set(data.color);
                    target.acceptVisitor(new Visitor() {
                        @Override
                        public void visit(final Spatial spatial) {
                            if (spatial instanceof Mesh) {
                                final Mesh mesh = (Mesh) spatial;
                                mesh.setDefaultColor(tData.color);
                                mesh.setSolidColor(tData.color);
                            }
                        }
                    }, true);
                }
            }
        }

        @Override
        public void copyState(final Spatial source) {
            super.copyState(source);
            if (source.getUserData() instanceof MarkerData) {
                data.copy((MarkerData) source.getUserData());
            }
        }
    }
}
