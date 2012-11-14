/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ardor3d.extension.ui.backdrop.EmptyBackdrop;
import com.ardor3d.extension.ui.backdrop.UIBackdrop;
import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.border.UIBorder;
import com.ardor3d.extension.ui.layout.UILayoutData;
import com.ardor3d.extension.ui.skin.SkinManager;
import com.ardor3d.extension.ui.text.StyleConstants;
import com.ardor3d.extension.ui.text.UIKeyHandler;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.BlendEquation;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.google.common.collect.Maps;

/**
 * Base UI class. All UI components/widgets/controls extend this class.
 * 
 * TODO: alert/dirty needed for font style changes.
 */
public abstract class UIComponent extends Node implements UIKeyHandler, IComponent {
    /** If true, use opacity settings to blend the components. Default is false. */
    private static boolean _useTransparency = false;

    /** The opacity of the component currently being rendered. */
    private static float _currentOpacity = 1f;

    /** The internal contents portion of this component. */
    private final Dimension _contentsSize = new Dimension(10, 10);
    /** The absolute minimum size of the internal contents portion of this component. */
    private final Dimension _minimumContentsSize = new Dimension(10, 10);
    /** The absolute maximum size of the internal contents portion of this component. */
    private final Dimension _maximumContentsSize = new Dimension(10000, 10000);
    /** A spacing between the component's border and its inner content area. */
    private Insets _padding = new Insets(0, 0, 0, 0);
    /** A border around this component. */
    private UIBorder _border = new EmptyBorder();
    /** A spacing between the component's border and other content outside this component. Used during layout. */
    private Insets _margin = new Insets(0, 0, 0, 0);

    /** The renderer responsible for drawing this component's backdrop. */
    private UIBackdrop _backdrop = new EmptyBackdrop();

    /** White */
    public static ReadOnlyColorRGBA DEFAULT_FOREGROUND_COLOR = ColorRGBA.WHITE;
    /** The color of any text or other such foreground elements. Inherited from parent if null. */
    private ColorRGBA _foregroundColor = null;

    /** Text to display using UITooltip when hovered over. Inherited from parent if null. */
    protected String _tooltipText = null;
    /** The amount of time (in ms) before we should show the tooltip on this component. */
    protected int _tooltipPopTime = 1000;

    /** The default font family (Vera) used when font family field and all parents font fields are null. */
    private static String _defaultFontFamily = "Vera";

    /** The default font size (18) used when font size field and all parents font size fields are 0. */
    private static int _defaultFontSize = 18;

    /** The default font styles to use. */
    private static Map<String, Object> _defaultFontStyles = Maps.newHashMap();
    /** The font styles to use for text on this component, if needed. */
    private Map<String, Object> _fontStyles = Maps.newHashMap();

    /** Optional information used by a parent container's layout. */
    private UILayoutData _layoutData = null;

    /** If true, this component is drawn. */
    private boolean _visible = true;
    /** If false, this component may optionally disable input to it and its children (as applicable). */
    private boolean _enabled = true;
    /** If true, we will consume any mouse events that are sent to this component. (default is false) */
    private boolean _consumeMouseEvents = false;
    /** If true, we will consume any key events that are sent to this component. (default is false) */
    private boolean _consumeKeyEvents = false;

    /** The opacity of this component. 0 is fully transparent and 1 is fully opaque. The default is 1. */
    private float _opacity = 1.0f;

    /** If we are selected for key focus, we'll redirect that focus to this target if not null. */
    private UIComponent _keyFocusTarget = null;

    /**
     * flag to indicate if a component has touched its content area. Used to determine blending operations for child
     * components.
     */
    private boolean _virginContentArea = false;

    /** The current future task set to show a tooltip in the near future. */
    private FutureTask<Void> _showTask;

    /** The system time when the tool tip should show up next (if currently being timed.) */
    private long _toolDone;

    /** Blend states to use when drawing components as cached container contents. */
    private static BlendState _srcRGBmaxAlphaBlend = UIComponent.createSrcRGBMaxAlphaBlend();
    private static BlendState _blendRGBmaxAlphaBlend = UIComponent.createBlendRGBMaxAlphaBlend();

    protected void applySkin() {
        SkinManager.applyCurrentSkin(this);
        updateMinimumSizeFromContents();
    }

    /**
     * @return true if this component should be considered "enabled"... a concept that is interpreted by each individual
     *         component type.
     */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * @param enabled
     *            true if this component should be considered "enabled"... a concept that is interpreted by each
     *            individual component type.
     */
    public void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    /**
     * @return true if this component should be drawn.
     */
    @Override
    public boolean isVisible() {
        return _visible;
    }

    /**
     * @param visible
     *            true if this component should be drawn.
     */
    @Override
    public void setVisible(final boolean visible) {
        _visible = visible;
    }

    /**
     * Used primarily during rendering to determine how alpha blending should be done.
     * 
     * @return true if nothing has been drawn by this component or its ancestors yet that would affect its content area.
     */
    public boolean hasVirginContentArea() {
        if (!_virginContentArea) {
            return false;
        } else if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).hasVirginContentArea();
        } else {
            return true;
        }
    }

    /**
     * @param virgin
     *            true if nothing has been drawn by this component yet that would affect its content area.
     */
    public void setVirginContentArea(final boolean virgin) {
        _virginContentArea = virgin;
    }

    /**
     * Add a new font style to this component. Will be inherited by children if they do not have the same key.
     * 
     * @param styleKey
     *            style key
     * @param styleValue
     *            the value to associate with the key.
     */
    public void addFontStyle(final String styleKey, final Object styleValue) {
        _fontStyles.put(styleKey, styleValue);
        fireStyleChanged();
    }

    /**
     * Removes a font style locally from this component, if present.
     * 
     * @param styleKey
     *            style key
     * @return the style value we were mapped to, or null if none (or was mapped to null).
     */
    public Object clearFontStyle(final String styleKey) {
        final Object removedValue = _fontStyles.remove(styleKey);
        fireStyleChanged();
        return removedValue;
    }

    /**
     * @return the locally set font styles.
     */
    public Map<String, Object> getLocalFontStyles() {
        return _fontStyles;
    }

    /**
     * @return our combined font styles, merged downward from root, including defaults.
     */
    public Map<String, Object> getFontStyles() {
        final Map<String, Object> styles;
        if (getParent() != null && getParent() instanceof UIComponent) {
            styles = ((UIComponent) getParent()).getFontStyles();
        } else {
            styles = Maps.newHashMap(UIComponent._defaultFontStyles);
            styles.put(StyleConstants.KEY_COLOR, UIComponent.DEFAULT_FOREGROUND_COLOR);
        }
        styles.putAll(_fontStyles);

        if (_foregroundColor != null) {
            styles.put(StyleConstants.KEY_COLOR, _foregroundColor);
        }
        return styles;
    }

    /**
     * @param styles
     *            the font styles to use (as needed) for this component. Note that this can be inherited by child
     *            components if this is a container class and styles is null.
     */
    public void setFontStyles(final Map<String, Object> styles) {
        if (styles == null) {
            _fontStyles = null;
        } else {
            _fontStyles.clear();
            _fontStyles.putAll(styles);
        }
        fireStyleChanged();
    }

    /**
     * @return the set layout data object or null if none has been set.
     */
    public UILayoutData getLayoutData() {
        return _layoutData;
    }

    /**
     * @param layoutData
     *            the layout data object set on this component. The object would provide specific layout directions to
     *            the layout class of the container this component is placed in.
     */
    public void setLayoutData(final UILayoutData layoutData) {
        _layoutData = layoutData;
    }

    /**
     * @return the width of this entire component as a whole, including all margins, borders, padding and content (in
     *         the component's coordinate space.)
     */
    public int getLocalComponentWidth() {
        return _contentsSize.getWidth() + getTotalLeft() + getTotalRight();
    }

    /**
     * @return the height of this entire component as a whole, including all margins, borders, padding and content (in
     *         the component's coordinate space.)
     */
    public int getLocalComponentHeight() {
        return _contentsSize.getHeight() + getTotalTop() + getTotalBottom();
    }

    /**
     * 
     * @param store
     *            the object to store our results in. If null, a new Rectangle2 is created and returned.
     * @return
     */
    public Rectangle2 getRelativeMinComponentBounds(final Rectangle2 store) {
        Rectangle2 rVal = store;
        if (rVal == null) {
            rVal = new Rectangle2();
        }
        final int height = getMinimumLocalComponentHeight();
        final int width = getMinimumLocalComponentWidth();
        return getRelativeComponentBounds(rVal, width, height);
    }

    /**
     * 
     * @param store
     *            the object to store our results in. If null, a new Rectangle2 is created and returned.
     * @return
     */
    public Rectangle2 getRelativeMaxComponentBounds(final Rectangle2 store) {
        Rectangle2 rVal = store;
        if (rVal == null) {
            rVal = new Rectangle2();
        }
        final int height = getMaximumLocalComponentHeight();
        final int width = getMaximumLocalComponentWidth();
        return getRelativeComponentBounds(rVal, width, height);
    }

    /**
     * 
     * @param store
     *            the object to store our results in. If null, a new Rectangle2 is created and returned.
     * @return the current bounds of this component, in the coordinate space of its parent.
     */
    public Rectangle2 getRelativeComponentBounds(final Rectangle2 store) {
        Rectangle2 rVal = store;
        if (rVal == null) {
            rVal = new Rectangle2();
        }
        final int height = getLocalComponentHeight();
        final int width = getLocalComponentWidth();
        return getRelativeComponentBounds(rVal, width, height);
    }

    private Rectangle2 getRelativeComponentBounds(final Rectangle2 store, final int width, final int height) {
        final ReadOnlyTransform local = getTransform();
        if (local.isIdentity() || local.getMatrix().isIdentity()) {
            store.set(0, 0, width, height);
        } else {
            float minX, maxX, minY, maxY;
            final Vector3 t = Vector3.fetchTempInstance();

            t.set(width, height, 0);
            local.applyForwardVector(t);
            minX = Math.min(t.getXf(), 0);
            maxX = Math.max(t.getXf(), 0);
            minY = Math.min(t.getYf(), 0);
            maxY = Math.max(t.getYf(), 0);

            t.set(0, height, 0);
            local.applyForwardVector(t);
            minX = Math.min(t.getXf(), minX);
            maxX = Math.max(t.getXf(), maxX);
            minY = Math.min(t.getYf(), minY);
            maxY = Math.max(t.getYf(), maxY);

            t.set(width, 0, 0);
            local.applyForwardVector(t);
            minX = Math.min(t.getXf(), minX);
            maxX = Math.max(t.getXf(), maxX);
            minY = Math.min(t.getYf(), minY);
            maxY = Math.max(t.getYf(), maxY);

            Vector3.releaseTempInstance(t);
            store.set(Math.round(minX), Math.round(minY), Math.round(maxX - minX), Math.round(maxY - minY));
        }

        return store;
    }

    /**
     * Sets the width and height of this component by forcing the content area to be of a proper size such that when the
     * padding, margin and border are added, the total component size match those given.
     * 
     * @param width
     *            the new width of the component
     * @param height
     *            the new height of the component
     */
    public void setLocalComponentSize(final int width, final int height) {
        setLocalComponentWidth(width);
        setLocalComponentHeight(height);
    }

    /**
     * @return the width contained in _minimumContentsSize + the margin, border and padding values for left and right.
     */
    public int getMinimumLocalComponentWidth() {
        return _minimumContentsSize.getWidth() + getTotalLeft() + getTotalRight();
    }

    /**
     * @return the height contained in _minimumContentsSize + the margin, border and padding values for top and bottom.
     */
    public int getMinimumLocalComponentHeight() {
        return _minimumContentsSize.getHeight() + getTotalTop() + getTotalBottom();
    }

    /**
     * @return the width contained in _maximumContentsSize + the margin, border and padding values for left and right.
     */
    public int getMaximumLocalComponentWidth() {
        return _maximumContentsSize.getWidth() + getTotalLeft() + getTotalRight();
    }

    /**
     * @return the height contained in _maximumContentsSize + the margin, border and padding values for top and bottom.
     */
    public int getMaximumLocalComponentHeight() {
        return _maximumContentsSize.getHeight() + getTotalTop() + getTotalBottom();
    }

    /**
     * Sets the width and height of the content area of this component.
     * 
     * @param width
     *            the new width of the content area
     * @param height
     *            the new height of the content area
     */
    public void setContentSize(final int width, final int height) {
        setContentWidth(width);
        setContentHeight(height);
    }

    /**
     * Sets the height of the content area of this component to that given, as long as we're between min and max content
     * height.
     * 
     * @param height
     *            the new height
     */
    public void setContentHeight(final int height) {
        _contentsSize.setHeight(MathUtils.clamp(height, _minimumContentsSize.getHeight(), _maximumContentsSize
                .getHeight()));
    }

    /**
     * Sets the width of the content area of this component to that given, as long as we're between min and max content
     * width.
     * 
     * @param width
     *            the new width
     */
    public void setContentWidth(final int width) {
        _contentsSize
                .setWidth(MathUtils.clamp(width, _minimumContentsSize.getWidth(), _maximumContentsSize.getWidth()));
    }

    /**
     * Sets the current component height to that given, as long as it would not violate min and max content height.
     * 
     * @param height
     *            the new height
     */
    public void setLocalComponentHeight(final int height) {
        setContentHeight(height - getTotalTop() - getTotalBottom());
    }

    /**
     * Sets the current component width to that given, as long as it would not violate min and max content width.
     * 
     * @param width
     *            the new width
     */
    public void setLocalComponentWidth(final int width) {
        setContentWidth(width - getTotalLeft() - getTotalRight());
    }

    /**
     * @return the width of the content area of this component.
     */
    @Override
    public int getContentWidth() {
        return _contentsSize.getWidth();
    }

    @Override
    public int getTotalWidth() {
        int totalW = getContentWidth();
        if (getBorder() != null) {
            totalW += getBorder().getLeft() + getBorder().getRight();
        }
        return totalW;
    }
    /**
     * @return the height of the content area of this component.
     */
    @Override
    public int getContentHeight() {
        return _contentsSize.getHeight();
    }
    
    @Override
    public int getTotalHeight() {
        int totalH = getContentHeight();
        if (getBorder() != null) {
            totalH += getBorder().getTop() + getBorder().getBottom();
        }
        return totalH;
    }
    /**
     * Sets the maximum content size of this component to the values given.
     * 
     * @param width
     * @param height
     */
    public void setMaximumContentSize(final int width, final int height) {
        _maximumContentsSize.set(width, height);
        validateContentSize();
    }

    /**
     * Sets the maximum content width of this component to the value given.
     * 
     * @param width
     */
    public void setMaximumContentWidth(final int width) {
        _maximumContentsSize.setWidth(width);
    }

    /**
     * Sets the maximum content height of this component to the value given.
     * 
     * @param height
     */
    public void setMaximumContentHeight(final int height) {
        _maximumContentsSize.setHeight(height);
        validateContentSize();
    }

    /**
     * Sets the minimum content size of this component to the values given.
     * 
     * @param width
     * @param height
     */
    public void setMinimumContentSize(final int width, final int height) {
        _minimumContentsSize.set(width, height);
        validateContentSize();
    }

    /**
     * Sets the minimum content width of this component to the value given.
     * 
     * @param width
     */
    public void setMinimumContentWidth(final int width) {
        _minimumContentsSize.setWidth(width);
        validateContentSize();
    }

    /**
     * Sets the minimum content height of this component to the value given.
     * 
     * @param height
     */
    public void setMinimumContentHeight(final int height) {
        _minimumContentsSize.setHeight(height);
        validateContentSize();
    }

    public boolean isConsumeKeyEvents() {
        return _consumeKeyEvents;
    }

    public void setConsumeKeyEvents(final boolean consume) {
        _consumeKeyEvents = consume;
    }

    public boolean isConsumeMouseEvents() {
        return _consumeMouseEvents;
    }

    public void setConsumeMouseEvents(final boolean consume) {
        _consumeMouseEvents = consume;
    }

    /**
     * Ensures content size is between min and max.
     */
    protected void validateContentSize() {
        final int width = MathUtils.clamp(_contentsSize.getWidth(), _minimumContentsSize.getWidth(),
                _maximumContentsSize.getWidth());
        final int height = MathUtils.clamp(_contentsSize.getHeight(), _minimumContentsSize.getHeight(),
                _maximumContentsSize.getHeight());
        _contentsSize.set(width, height);
    }

    /**
     * Sets the size of the content area of this component to the current width and height set on _minimumContentsSize
     * (if component is set to allow such resizing.)
     */
    public void compact() {
        setContentSize(_minimumContentsSize.getWidth(), _minimumContentsSize.getHeight());
    }

    /**
     * Attempt to force this component to fit in the given rectangle.
     * 
     * @param width
     * @param height
     */
    public void fitComponentIn(final int width, final int height) {
        final ReadOnlyTransform local = getTransform();
        if (local.isIdentity() || local.getMatrix().isIdentity()) {
            setLocalComponentSize(width, height);
            return;
        }

        final Vector3 temp = Vector3.fetchTempInstance();
        temp.set(1, 0, 0);
        local.applyForwardVector(temp);
        if (Math.abs(temp.getX()) >= 0.99999) {
            setLocalComponentSize(width, height);
        } else if (Math.abs(temp.getY()) >= 0.99999) {
            setLocalComponentSize(height, width);
        } else {
            final Rectangle2 rect = getRelativeMinComponentBounds(null);
            final float ratio = Math.min((float) width / rect.getWidth(), (float) height / rect.getHeight());

            setLocalComponentSize(Math.round(getMinimumLocalComponentWidth() * ratio), Math
                    .round(getMinimumLocalComponentHeight() * ratio));
        }
    }

    /**
     * Override this to perform actual layout.
     */
    public void layout() {
        // Let our containers know we are sullied...
        fireComponentDirty();
    }

    /**
     * @return the sum of the bottom side of this component's margin, border and padding (if they are set).
     */
    public int getTotalBottom() {
        int bottom = 0;
        if (getMargin() != null) {
            bottom += getMargin().getBottom();
        }
        if (getBorder() != null) {
            bottom += getBorder().getBottom();
        }
        if (getPadding() != null) {
            bottom += getPadding().getBottom();
        }
        return bottom;
    }

    /**
     * @return the sum of the top side of this component's margin, border and padding (if they are set).
     */
    public int getTotalTop() {
        int top = 0;
        if (getMargin() != null) {
            top += getMargin().getTop();
        }
        if (getBorder() != null) {
            top += getBorder().getTop();
        }
        if (getPadding() != null) {
            top += getPadding().getTop();
        }
        return top;
    }

    /**
     * @return the sum of the left side of this component's margin, border and padding (if they are set).
     */
    public int getTotalLeft() {
        int left = 0;
        if (getMargin() != null) {
            left += getMargin().getLeft();
        }
        if (getBorder() != null) {
            left += getBorder().getLeft();
        }
        if (getPadding() != null) {
            left += getPadding().getLeft();
        }
        return left;
    }

    /**
     * @return the sum of the right side of this component's margin, border and padding (if they are set).
     */
    public int getTotalRight() {
        int right = 0;
        if (getMargin() != null) {
            right += getMargin().getRight();
        }
        if (getBorder() != null) {
            right += getBorder().getRight();
        }
        if (getPadding() != null) {
            right += getPadding().getRight();
        }
        return right;
    }

    /**
     * @return the current border set on this component, if any.
     */
    public UIBorder getBorder() {
        return _border;
    }

    /**
     * @param border
     *            the border we wish this component to use. May be null.
     */
    public void setBorder(final UIBorder border) {
        _border = border;
    }

    /**
     * @return the current backdrop set on this component, if any.
     */
    public UIBackdrop getBackdrop() {
        return _backdrop;
    }

    /**
     * @param backdrop
     *            the backdrop we wish this component to use. May be null.
     */
    public void setBackdrop(final UIBackdrop backDrop) {
        _backdrop = backDrop;
    }

    /**
     * @return the current margin set on this component, if any.
     */
    public Insets getMargin() {
        return _margin;
    }

    /**
     * @param margin
     *            the new margin (a spacing between the component's border and other components) to set on this
     *            component. Copied into the component and is allowed to be null.
     */
    public void setMargin(final Insets margin) {
        if (margin == null) {
            _margin = null;
        } else if (_margin == null) {
            _margin = new Insets(margin);
        } else {
            _margin.set(margin);
        }
    }

    /**
     * @return the current margin set on this component, if any.
     */
    public Insets getPadding() {
        return _padding;
    }

    /**
     * @param padding
     *            the new padding (a spacing between the component's border and its inner content area) to set on this
     *            component. Copied into the component and is allowed to be null.
     */
    public void setPadding(final Insets padding) {
        if (padding == null) {
            _padding = null;
        } else if (_padding == null) {
            _padding = new Insets(padding);
        } else {
            _padding.set(padding);
        }
    }

    /**
     * @return true if our parent is a UIHud.
     */
    public boolean isAttachedToHUD() {
        return getParent() instanceof UIHud;
    }

    /**
     * @return the first instance of UIComponent found in this Component's UIComponent ancestry that is attached to the
     *         hud, or null if none are found. Returns "this" component if it is directly attached to the hud.
     */
    public UIComponent getTopLevelComponent() {
        if (isAttachedToHUD()) {
            return this;
        }
        final Node parent = getParent();
        if (parent instanceof UIComponent) {
            return ((UIComponent) parent).getTopLevelComponent();
        } else {
            return null;
        }
    }

    /**
     * @return the first instance of UIHud found in this Component's UIComponent ancestry or null if none are found.
     */
    @Override
    public UIHud getHud() {
        final Node parent = getParent();
        if (parent instanceof UIHud) {
            return (UIHud) parent;
        } else if (parent instanceof UIComponent) {
            return ((UIComponent) parent).getHud();
        } else {
            return null;
        }
    }

    /**
     * Override to provide an action to take when this component or its top level component are attached to a UIHud.
     */
    public void attachedToHud() {}

    /**
     * Override to provide an action to take just before this component or its top level component are removed from a
     * UIHud.
     */
    public void detachedFromHud() {}

    /**
     * @return current screen x coordinate of this component's origin (usually its lower left corner.)
     */
    public int getHudX() {
        return Math.round(getWorldTranslation().getXf());
    }

    /**
     * @return current screen y coordinate of this component's origin (usually its lower left corner.)
     */
    public int getHudY() {
        return Math.round(getWorldTranslation().getYf());
    }

    /**
     * Sets the screen x,y coordinate of this component's origin (usually its lower left corner.)
     * 
     * @param x
     * @param y
     */
    public void setHudXY(final int x, final int y) {
        final double newX = getHudX() - getTranslation().getX() + x;
        final double newY = getHudY() - getTranslation().getY() + y;
        setTranslation(newX, newY, getTranslation().getZ());
    }

    /**
     * @param x
     *            the new screen x coordinate of this component's origin (usually its lower left corner.)
     */
    public void setHudX(final int x) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(getHudX() - translation.getX() + x, translation.getY(), translation.getZ());
    }

    /**
     * @param y
     *            the new screen y coordinate of this component's origin (usually its lower left corner.)
     */
    public void setHudY(final int y) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(translation.getX(), getHudY() - translation.getY() + y, translation.getZ());
    }

    /**
     * @return a local x translation from the parent component's content area.
     */
    public int getLocalX() {
        return (int) Math.round(getTranslation().getX());
    }

    /**
     * @return a local y translation from the parent component's content area.
     */
    public int getLocalY() {
        return (int) Math.round(getTranslation().getY());
    }

    /**
     * Set the x,y translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param x
     * @param y
     */
    public void setLocalXY(final int x, final int y) {
        setTranslation(x, y, getTranslation().getZ());
        fireComponentDirty();
    }

    /**
     * Set the x translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param x
     */
    public void setLocalX(final int x) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(x, translation.getY(), translation.getZ());
        fireComponentDirty();
    }

    /**
     * Set the y translation from the lower left corner of our parent's content area to the origin of this component.
     * 
     * @param y
     */
    public void setLocalY(final int y) {
        final ReadOnlyVector3 translation = getTranslation();
        setTranslation(translation.getX(), y, translation.getZ());
        fireComponentDirty();
    }

    /**
     * @return the currently set foreground color on this component. Does not inherit from ancestors or default, so may
     *         return null.
     */
    public ReadOnlyColorRGBA getLocalForegroundColor() {
        return _foregroundColor;
    }

    /**
     * @return the foreground color associated with this component. If none has been set, we will ask our parent
     *         component and so on. If no component is found in our ancestry with a foreground color, we will use
     *         {@link #DEFAULT_FOREGROUND_COLOR}
     */
    public ReadOnlyColorRGBA getForegroundColor() {
        ReadOnlyColorRGBA foreColor = _foregroundColor;
        if (foreColor == null) {
            if (getParent() != null && getParent() instanceof UIComponent) {
                foreColor = ((UIComponent) getParent()).getForegroundColor();
            } else {
                foreColor = UIComponent.DEFAULT_FOREGROUND_COLOR;
            }
        }
        return foreColor;
    }

    /**
     * @param color
     *            the foreground color of this component.
     */
    public void setForegroundColor(final ReadOnlyColorRGBA color) {
        if (color == null) {
            _foregroundColor = null;
        } else if (_foregroundColor == null) {
            _foregroundColor = new ColorRGBA(color);
        } else {
            _foregroundColor.set(color);
        }
        fireStyleChanged();
        fireComponentDirty();
    }

    /**
     * @return this component's tooltip text. If none has been set, we will ask our parent component and so on. returns
     *         null if no tooltips are found.
     */
    public String getTooltipText() {
        if (_tooltipText != null) {
            return _tooltipText;
        } else if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).getTooltipText();
        } else {
            return null;
        }
    }

    /**
     * @param text
     *            the tooltip text of this component.
     */
    public void setTooltipText(final String text) {
        _tooltipText = text;
    }

    /**
     * @return the amount of time in ms to wait before showing a tooltip for this component.
     */
    public int getTooltipPopTime() {
        return _tooltipPopTime;
    }

    /**
     * @param ms
     *            the amount of time in ms to wait before showing a tooltip for this component. This is only granular to
     *            a tenth of a second (or 100ms)
     */
    public void setTooltipPopTime(final int ms) {
        _tooltipPopTime = ms;
    }

    /**
     * Check if our tooltip timer is active and cancel it.
     */
    protected void cancelTooltipTimer() {
        if (_showTask != null && !_showTask.isDone()) {
            _showTask.cancel(true);
            _showTask = null;
        }
    }

    /**
     * @param hudX
     *            the x screen coordinate
     * @param hudY
     *            the y screen coordinate
     * @return true if the given screen coordinates fall inside the margin area of this component (or in other words, is
     *         at the border level or deeper.)
     */
    public boolean insideMargin(final int hudX, final int hudY) {
        final Vector3 vec = Vector3.fetchTempInstance();
        vec.set(hudX, hudY, 0);
        getWorldTransform().applyInverse(vec);

        final double x = vec.getX() - getMargin().getLeft();
        final double y = vec.getY() - getMargin().getBottom();
        Vector3.releaseTempInstance(vec);

        return x >= 0 && x < getLocalComponentWidth() - getMargin().getLeft() - getMargin().getRight() && y >= 0
                && y < getLocalComponentHeight() - getMargin().getBottom() - getMargin().getTop();
    }

    /**
     * @param hudX
     *            the x screen coordinate
     * @param hudY
     *            the y screen coordinate
     * @return this component (or an appropriate child coordinate in the case of a container) if the given screen
     *         coordinates fall inside the margin area of this component.
     */
    public UIComponent getUIComponent(final int hudX, final int hudY) {
        if (getSceneHints().isPickingHintEnabled(PickingHint.Pickable) && isVisible() && insideMargin(hudX, hudY)) {
            return this;
        }
        return null;
    }

    @Override
    public void updateWorldTransform(final boolean recurse) {
        updateWorldTransform(recurse, true);
    }

    /**
     * Allow skipping updating our own world transform.
     * 
     * @param recurse
     * @param self
     */
    protected void updateWorldTransform(final boolean recurse, final boolean self) {
        if (self) {
            if (_parent != null) {
                if (_parent instanceof UIComponent) {
                    final UIComponent gPar = (UIComponent) _parent;

                    // grab our parent's world transform
                    final Transform t = Transform.fetchTempInstance();
                    t.set(_parent.getWorldTransform());

                    // shift our origin by total left/bottom
                    final Vector3 v = Vector3.fetchTempInstance();
                    v.set(gPar.getTotalLeft(), gPar.getTotalBottom(), 0);
                    t.applyForwardVector(v);
                    t.translate(v);
                    Vector3.releaseTempInstance(v);

                    // apply our local transform
                    t.multiply(_localTransform, _worldTransform);
                    Transform.releaseTempInstance(t);
                } else {
                    _parent.getWorldTransform().multiply(_localTransform, _worldTransform);
                }
            } else {
                _worldTransform.set(_localTransform);
            }
        }

        if (recurse) {
            for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
                getChild(i).updateWorldTransform(true);
            }
        }
        clearDirty(DirtyType.Transform);
    }

    @Override
    public void draw(final Renderer r) {
        // Don't draw if we are not visible.
        if (!isVisible()) {
            return;
        }

        boolean clearAlpha = false;
        // If we are drawing this component as part of cached container contents, we need to alter the blending to get a
        // texture with the correct color and alpha.
        if (UIContainer.isDrawingStandin()) {
            if (getParent() instanceof UIComponent && !((UIComponent) getParent()).hasVirginContentArea()) {
                // we are drawing a sub component onto a surface that already has color, so do a alpha based color blend
                // and use the max alpha value.
                ContextManager.getCurrentContext().enforceState(UIComponent._blendRGBmaxAlphaBlend);
            } else {
                // we are drawing a top level component onto an empty texture surface, so use the source color modulated
                // by the source alpha and the source alpha value.
                ContextManager.getCurrentContext().enforceState(UIComponent._srcRGBmaxAlphaBlend);
            }
            clearAlpha = true;
        }

        // Call any predraw operation
        predrawComponent(r);

        // Draw the component backdrop
        if (getBackdrop() != null) {
            _virginContentArea = false;
            getBackdrop().draw(r, this);
        } else {
            _virginContentArea = true;
        }

        // draw the component border
        if (getBorder() != null) {
            getBorder().draw(r, this);
        }

        // draw the component, generally editing the content area.
        drawComponent(r);

        // Call any postdraw operation
        postdrawComponent(r);

        // Clear enforced blend, if set.
        if (clearAlpha) {
            ContextManager.getCurrentContext().clearEnforcedState(StateType.Blend);
        }
    }

    /**
     * @param defaultFont
     *            the Font to use as "default" across all UI components that do not have one set.
     */
    public static void setDefaultFontFamily(final String family) {
        UIComponent._defaultFontFamily = family;
    }

    /**
     * @return the default Font family. Defaults to "Vera".
     */
    public static String getDefaultFontFamily() {
        return UIComponent._defaultFontFamily;
    }

    /**
     * @param size
     *            the Font size to use as "default" across all UI components that do not have one set.
     */
    public static void setDefaultFontSize(final int size) {
        UIComponent._defaultFontSize = size;
    }

    /**
     * @return the default Font size (height). Defaults to 18.
     */
    public static int getDefaultFontSize() {
        return UIComponent._defaultFontSize;
    }

    /**
     * @param defaultStyles
     *            the Font styles to use as "default" across all UI components that do not have one set.
     */
    public static void setDefaultFontStyles(final Map<String, Object> defaultStyles) {
        if (defaultStyles == null) {
            UIComponent._defaultFontStyles = Maps.newHashMap();
        } else {
            UIComponent._defaultFontStyles = Maps.newHashMap(defaultStyles);
        }
    }

    /**
     * @return the default Font styles.
     */
    public static Map<String, Object> getDefaultFontStyles() {
        return UIComponent._defaultFontStyles;
    }

    /**
     * @return a blend state that does alpha blending and writes the max alpha value (source or destination) back to the
     *         color buffer.
     */
    private static BlendState createSrcRGBMaxAlphaBlend() {
        final BlendState state = new BlendState();
        state.setBlendEnabled(true);
        state.setSourceFunctionRGB(SourceFunction.SourceAlpha);
        state.setDestinationFunctionRGB(DestinationFunction.Zero);
        state.setBlendEquationRGB(BlendEquation.Add);
        state.setSourceFunctionAlpha(SourceFunction.SourceAlpha);
        state.setDestinationFunctionAlpha(DestinationFunction.DestinationAlpha);
        state.setBlendEquationAlpha(BlendEquation.Max);
        return state;
    }

    /**
     * @return a blend state that does alpha blending and writes the max alpha value (source or destination) back to the
     *         color buffer.
     */
    private static BlendState createBlendRGBMaxAlphaBlend() {
        final BlendState state = new BlendState();
        state.setBlendEnabled(true);
        state.setSourceFunctionRGB(SourceFunction.SourceAlpha);
        state.setDestinationFunctionRGB(DestinationFunction.OneMinusSourceAlpha);
        state.setBlendEquationRGB(BlendEquation.Add);
        state.setSourceFunctionAlpha(SourceFunction.SourceAlpha);
        state.setDestinationFunctionAlpha(DestinationFunction.DestinationAlpha);
        state.setBlendEquationAlpha(BlendEquation.Max);
        return state;
    }

    /**
     * @return the opacity level set on this component in [0,1], where 0 means completely transparent and 1 is
     *         completely opaque. If useTransparency is false, this will always return 1.
     */
    public float getCombinedOpacity() {
        if (UIComponent._useTransparency) {
            if (getParent() instanceof UIComponent) {
                return _opacity * ((UIComponent) getParent()).getCombinedOpacity();
            } else {
                return _opacity;
            }
        } else {
            return 1.0f;
        }
    }

    /**
     * @return the opacity set on this component directly, not accounting for parent opacity levels.
     */
    public float getLocalOpacity() {
        return _opacity;
    }

    /**
     * Set the opacity level of this component.
     * 
     * @param opacity
     *            value in [0,1], where 0 means completely transparent and 1 is completely opaque.
     */
    public void setOpacity(final float opacity) {
        _opacity = opacity;
    }

    /**
     * Tell all ancestors that use standins, if any, that they need to update any cached graphical representation.
     */
    public void fireComponentDirty() {
        if (getParent() instanceof UIComponent) {
            ((UIComponent) getParent()).fireComponentDirty();
        }
    }

    /**
     * Let subcomponents know that style has been changed.
     */
    public void fireStyleChanged() {}

    /**
     * @return true if all components should use their opacity value to blend against other components (and/or the 3d
     *         background scene.)
     */
    public static boolean isUseTransparency() {
        return UIComponent._useTransparency;
    }

    /**
     * @param useTransparency
     *            true if all components should use their opacity value to blend against the 3d scene (and each other)
     */
    public static void setUseTransparency(final boolean useTransparency) {
        UIComponent._useTransparency = useTransparency;
    }

    /**
     * @return the currently rendering component's opacity level. Used by the renderer to alter alpha values based of
     *         component elements.
     */
    public static float getCurrentOpacity() {
        return UIComponent._currentOpacity;
    }

    /**
     * Ask this component to update its minimum allowed size, based on its contents.
     */
    protected void updateMinimumSizeFromContents() {}

    /**
     * Perform any pre-draw operations on this component.
     * 
     * @param renderer
     */
    protected void predrawComponent(final Renderer renderer) {
        UIComponent._currentOpacity = getCombinedOpacity();
    }

    /**
     * Perform any post-draw operations on this component.
     * 
     * @param renderer
     */
    protected void postdrawComponent(final Renderer renderer) {}

    /**
     * Draw this component's contents using the given renderer.
     * 
     * @param renderer
     */
    protected void drawComponent(final Renderer renderer) {}

    // *******************
    // ** INPUT methods
    // *******************

    /**
     * Called when a mouse cursor enters this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     */
    public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        scheduleToolTip();
    }

    /**
     * 
     */
    private void scheduleToolTip() {
        final UIHud hud = getHud();
        if (hud != null && getTooltipText() != null) {
            final Callable<Void> show = new Callable<Void>() {
                public Void call() throws Exception {

                    while (true) {
                        if (System.currentTimeMillis() >= _toolDone) {
                            break;
                        }
                        Thread.sleep(100);
                    }

                    final UITooltip ttip = hud.getTooltip();

                    // set contents and size
                    ttip.getLabel().setText(getTooltipText());
                    ttip.updateMinimumSizeFromContents();
                    ttip.getLabel().compact();
                    ttip.compact();
                    ttip.layout();

                    // set position based on CURRENT mouse location.
                    int x = hud.getLastMouseX();
                    int y = hud.getLastMouseY();

                    // Try to keep tooltip on screen.
                    if (Camera.getCurrentCamera() != null) {
                        final int displayWidth = Camera.getCurrentCamera().getWidth();
                        final int displayHeight = Camera.getCurrentCamera().getHeight();

                        if (x < 0) {
                            x = 0;
                        } else if (x + ttip.getLocalComponentWidth() > displayWidth) {
                            x = displayWidth - ttip.getLocalComponentWidth();
                        }
                        if (y < 0) {
                            y = 0;
                        } else if (y + ttip.getLocalComponentHeight() > displayHeight) {
                            y = displayHeight - ttip.getLocalComponentHeight();
                        }
                    }
                    ttip.setHudXY(x, y);

                    // fire off that we're dirty
                    ttip.fireComponentDirty();
                    ttip.updateGeometricState(0, true);

                    // show
                    ttip.setVisible(true);
                    return null;
                }
            };
            cancelTooltipTimer();
            resetToolTipTime();
            _showTask = new FutureTask<Void>(show);
            final Thread t = new Thread() {
                @Override
                public void run() {
                    if (_showTask != null && !_showTask.isDone()) {
                        _showTask.run();
                    }
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Called when a mouse cursor leaves this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     */
    public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
        cancelTooltipTimer();
        final UIHud hud = getHud();

        if (hud != null) {
            hud.getTooltip().setVisible(false);
        }
    }

    /**
     * Called when a mouse button is pressed while the cursor is over this component.
     * 
     * @param button
     *            the button that was pressed
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mousePressed(final MouseButton button, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (!_consumeMouseEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mousePressed(button, state);
        } else {
            return _consumeMouseEvents;
        }
    }

    /**
     * Called when a mouse button is released while the cursor is over this component.
     * 
     * @param button
     *            the button that was released
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseReleased(final MouseButton button, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseReleased(button, state);
        } else {
            return _consumeMouseEvents;
        }
    }

    /**
     * Called when a mouse button is pressed and released in close proximity while the cursor is over this component.
     * 
     * @param button
     *            the button that was released
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseClicked(final MouseButton button, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseClicked(button, state);
        } else {
            return _consumeMouseEvents;
        }
    }

    /**
     * Called when a mouse is moved while the cursor is over this component.
     * 
     * @param mouseX
     *            mouse x coordinate.
     * @param mouseY
     *            mouse y coordinate.
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseMoved(final int mouseX, final int mouseY, final InputState state) {
        resetToolTipTime();

        // default is to offer event to parent, if it is a UIComponent
        if (!_consumeMouseEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseMoved(mouseX, mouseY, state);
        } else {
            return _consumeMouseEvents;
        }
    }

    private void resetToolTipTime() {
        _toolDone = System.currentTimeMillis() + getTooltipPopTime();
    }

    /**
     * Called when the mouse wheel is moved while the cursor is over this component.
     * 
     * @param wheelDx
     *            the last change of the wheel
     * @param state
     *            the current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean mouseWheel(final int wheelDx, final InputState state) {
        // default is to offer event to parent, if it is a UIComponent
        if (!_consumeMouseEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).mouseWheel(wheelDx, state);
        } else {
            return _consumeMouseEvents;
        }
    }

    /**
     * Called when this component has focus and a key is pressed.
     * 
     * @param key
     *            the key pressed.
     * @param the
     *            current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean keyPressed(final Key key, final InputState state) {
        if (!_consumeKeyEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).keyPressed(key, state);
        } else {
            return _consumeKeyEvents;
        }
    }

    /**
     * Called when this component has focus and a key is held down over more than 1 input cycle.
     * 
     * @param key
     *            the key held.
     * @param the
     *            current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean keyHeld(final Key key, final InputState state) {
        if (!_consumeKeyEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).keyHeld(key, state);
        } else {
            return _consumeKeyEvents;
        }
    }

    /**
     * Called when this component has focus and a key is released.
     * 
     * @param key
     *            the key released.
     * @param the
     *            current tracked state of the input system.
     * @return true if we want to consider the event "consumed" by the UI system.
     */
    public boolean keyReleased(final Key key, final InputState state) {
        if (!_consumeKeyEvents && getParent() instanceof UIComponent) {
            return ((UIComponent) getParent()).keyReleased(key, state);
        } else {
            return _consumeKeyEvents;
        }
    }

    /**
     * Called by the hud when a component is given focus.
     */
    public void gainedFocus() {}

    /**
     * Called by the hud when a component loses focus.
     */
    public void lostFocus() {}

    /**
     * Looks up the scenegraph for a Hud and asks it to set us as the currently focused component.
     */
    public void requestFocus() {
        final UIHud hud = getHud();
        if (hud != null) {
            hud.setFocusedComponent(this);
        }
    }

    /**
     * @return a component we defer to for key focus. Default is null.
     */
    public UIComponent getKeyFocusTarget() {
        return _keyFocusTarget;
    }

    /**
     * @param target
     *            a component to set as the actual focused component if this component receives focus.
     */
    public void setKeyFocusTarget(final UIComponent target) {
        _keyFocusTarget = target;
    }
    
    public boolean isMinimized() {
        return false;
    }
    
    @Override
    public void show() {
        setVisible();
        activateInteraction();
    }

    @Override
    public void hide() {
        setInVisible();
        removeInteraction();
    }
 
    @Override
    public void updateGeometricState(final double time) {
        super.updateGeometricState(time);
    }
}