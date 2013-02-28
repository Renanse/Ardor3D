/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.util;

import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.type.ReadOnlyRectangle2;
import com.ardor3d.renderer.state.record.RendererRecord;

public abstract class JoglRendererUtil {

    public static void switchMode(final RendererRecord rendRecord, final int mode) {
        final GL gl = GLContext.getCurrentGL();

        if (!rendRecord.isMatrixValid() || rendRecord.getMatrixMode() != mode) {
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glMatrixMode(mode);
            } else {
                // TODO use PMVMatrix
            }
            rendRecord.setMatrixMode(mode);
            rendRecord.setMatrixValid(true);
        }
    }

    public static void setBoundVBO(final RendererRecord rendRecord, final int id) {
        final GL gl = GLContext.getCurrentGL();

        if (!rendRecord.isVboValid() || rendRecord.getCurrentVboId() != id) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, id);
            rendRecord.setCurrentVboId(id);
            rendRecord.setVboValid(true);
        }
    }

    public static void setBoundElementVBO(final RendererRecord rendRecord, final int id) {
        final GL gl = GLContext.getCurrentGL();

        if (!rendRecord.isElementVboValid() || rendRecord.getCurrentElementVboId() != id) {
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, id);
            rendRecord.setCurrentElementVboId(id);
            rendRecord.setElementVboValid(true);
        }
    }

    public static void applyScissors(final RendererRecord rendRecord) {
        final GL gl = GLContext.getCurrentGL();
        final Stack<ReadOnlyRectangle2> clips = rendRecord.getScissorClips();

        if (clips.size() > 0) {
            final Rectangle2 init = Rectangle2.fetchTempInstance();
            init.set(-1, -1, -1, -1);
            ReadOnlyRectangle2 r;
            boolean first = true;
            for (int i = clips.size(); --i >= 0;) {
                r = clips.get(i);

                if (r == null) {
                    break;
                }
                if (first) {
                    init.set(r);
                    first = false;
                } else {
                    init.intersect(r, init);
                }
                if (init.getWidth() <= 0 || init.getHeight() <= 0) {
                    init.setWidth(0);
                    init.setHeight(0);
                    break;
                }
            }

            if (init.getWidth() == -1) {
                setClippingEnabled(rendRecord, false);
            } else {
                setClippingEnabled(rendRecord, true);
                gl.glScissor(init.getX(), init.getY(), init.getWidth(), init.getHeight());
            }
            Rectangle2.releaseTempInstance(init);
        } else {
            // no clips, so disable
            setClippingEnabled(rendRecord, false);
        }
    }

    public static void setClippingEnabled(final RendererRecord rendRecord, final boolean enabled) {
        final GL gl = GLContext.getCurrentGL();

        if (enabled && (!rendRecord.isClippingTestValid() || !rendRecord.isClippingTestEnabled())) {
            gl.glEnable(GL.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(true);
        } else if (!enabled && (!rendRecord.isClippingTestValid() || rendRecord.isClippingTestEnabled())) {
            gl.glDisable(GL.GL_SCISSOR_TEST);
            rendRecord.setClippingTestEnabled(false);
        }
        rendRecord.setClippingTestValid(true);
    }
}
