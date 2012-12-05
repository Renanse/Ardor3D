/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.ardor3d.light.DirectionalLight;
import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.light.SpotLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.LightRecord;
import com.ardor3d.renderer.state.record.LightStateRecord;

public abstract class LwjglLightStateUtil {

    public static void apply(final LightState state) {
        final RenderContext context = ContextManager.getCurrentContext();
        final LightStateRecord record = (LightStateRecord) context.getStateRecord(StateType.Light);
        context.setCurrentState(StateType.Light, state);

        if (state.isEnabled() && LightState.LIGHTS_ENABLED) {
            setLightEnabled(true, record);
            setTwoSided(state.getTwoSidedLighting(), record);
            setLocalViewer(state.getLocalViewer(), record);
            if (context.getCapabilities().isOpenGL1_2Supported()) {
                setSpecularControl(state.getSeparateSpecular(), record);
            }

            for (int i = 0, max = state.getNumberOfChildren(); i < max; i++) {
                final Light light = state.get(i);
                LightRecord lr = record.getLightRecord(i);
                // TODO: use the reference to get the lightrecord - rherlitz

                if (lr == null) {
                    lr = new LightRecord();
                    record.setLightRecord(lr, i);
                }

                if (light == null) {
                    setSingleLightEnabled(false, i, record, lr);
                } else {
                    if (light.isEnabled()) {
                        setLight(i, light, state, record, lr);
                    } else {
                        setSingleLightEnabled(false, i, record, lr);
                    }
                }
            }

            // disable lights at and above the max count in this state
            for (int i = state.getNumberOfChildren(); i < LightState.MAX_LIGHTS_ALLOWED; i++) {
                LightRecord lr = record.getLightRecord(i);

                if (lr == null) {
                    lr = new LightRecord();
                    record.setLightRecord(lr, i);
                }
                setSingleLightEnabled(false, i, record, lr);
            }

            if ((state.getLightMask() & LightState.MASK_GLOBALAMBIENT) == 0) {
                setModelAmbient(record, state.getGlobalAmbient());
            } else {
                setModelAmbient(record, ColorRGBA.BLACK_NO_ALPHA);
            }
        } else {
            setLightEnabled(false, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setLight(final int index, final Light light, final LightState state,
            final LightStateRecord record, final LightRecord lr) {
        setSingleLightEnabled(true, index, record, lr);

        if ((state.getLightMask() & LightState.MASK_AMBIENT) == 0
                && (light.getLightMask() & LightState.MASK_AMBIENT) == 0) {
            setAmbient(index, record, light.getAmbient(), lr);
        } else {
            setAmbient(index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if ((state.getLightMask() & LightState.MASK_DIFFUSE) == 0
                && (light.getLightMask() & LightState.MASK_DIFFUSE) == 0) {
            setDiffuse(index, record, light.getDiffuse(), lr);
        } else {
            setDiffuse(index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if ((state.getLightMask() & LightState.MASK_SPECULAR) == 0
                && (light.getLightMask() & LightState.MASK_SPECULAR) == 0) {
            setSpecular(index, record, light.getSpecular(), lr);
        } else {
            setSpecular(index, record, ColorRGBA.BLACK_NO_ALPHA, lr);
        }

        if (light.isAttenuate()) {
            setAttenuate(true, index, light, record, lr);

        } else {
            setAttenuate(false, index, light, record, lr);

        }

        switch (light.getType()) {
            case Directional: {
                final DirectionalLight dirLight = (DirectionalLight) light;

                final ReadOnlyVector3 direction = dirLight.getDirection();
                setPosition(index, record, -direction.getXf(), -direction.getYf(), -direction.getZf(), 0, lr);
                break;
            }
            case Point:
            case Spot: {
                final PointLight pointLight = (PointLight) light;
                final ReadOnlyVector3 location = pointLight.getLocation();
                setPosition(index, record, location.getXf(), location.getYf(), location.getZf(), 1, lr);
                break;
            }
        }

        if (light.getType() == Light.Type.Spot) {
            final SpotLight spot = (SpotLight) light;
            setSpotCutoff(index, record, spot.getAngle(), lr);
            final ReadOnlyVector3 direction = spot.getDirection();
            setSpotDirection(index, record, direction.getXf(), direction.getYf(), direction.getZf(), 0);
            setSpotExponent(index, record, spot.getExponent(), lr);
        } else {
            // set the cutoff to 180, which causes the other spot params to be
            // ignored.
            setSpotCutoff(index, record, 180, lr);
        }
    }

    private static void setSingleLightEnabled(final boolean enable, final int index, final LightStateRecord record,
            final LightRecord lr) {
        if (!record.isValid() || lr.isEnabled() != enable) {
            if (enable) {
                GL11.glEnable(GL11.GL_LIGHT0 + index);
            } else {
                GL11.glDisable(GL11.GL_LIGHT0 + index);
            }

            lr.setEnabled(enable);
        }
    }

    private static void setLightEnabled(final boolean enable, final LightStateRecord record) {
        if (!record.isValid() || record.isEnabled() != enable) {
            if (enable) {
                GL11.glEnable(GL11.GL_LIGHTING);
            } else {
                GL11.glDisable(GL11.GL_LIGHTING);
            }
            record.setEnabled(enable);
        }
    }

    private static void setTwoSided(final boolean twoSided, final LightStateRecord record) {
        if (!record.isValid() || record.isTwoSidedOn() != twoSided) {
            if (twoSided) {
                GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, GL11.GL_TRUE);
            } else {
                GL11.glLightModeli(GL11.GL_LIGHT_MODEL_TWO_SIDE, GL11.GL_FALSE);
            }
            record.setTwoSidedOn(twoSided);
        }
    }

    private static void setLocalViewer(final boolean localViewer, final LightStateRecord record) {
        if (!record.isValid() || record.isLocalViewer() != localViewer) {
            if (localViewer) {
                GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_TRUE);
            } else {
                GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, GL11.GL_FALSE);
            }
            record.setLocalViewer(localViewer);
        }
    }

    private static void setSpecularControl(final boolean separateSpecularOn, final LightStateRecord record) {
        if (!record.isValid() || record.isSeparateSpecular() != separateSpecularOn) {
            if (separateSpecularOn) {
                GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL, GL12.GL_SEPARATE_SPECULAR_COLOR);
            } else {
                GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL, GL12.GL_SINGLE_COLOR);
            }
            record.setSeparateSpecular(separateSpecularOn);
        }
    }

    private static void setModelAmbient(final LightStateRecord record, final ReadOnlyColorRGBA color) {
        if (!record.isValid() || !record.globalAmbient.equals(color)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(color.getRed());
            record.lightBuffer.put(color.getGreen());
            record.lightBuffer.put(color.getBlue());
            record.lightBuffer.put(color.getAlpha());
            record.lightBuffer.flip();
            GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, record.lightBuffer);
            record.globalAmbient.set(color);
        }
    }

    private static void setAmbient(final int index, final LightStateRecord record, final ReadOnlyColorRGBA ambient,
            final LightRecord lr) {
        if (!record.isValid() || !lr.ambient.equals(ambient)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(ambient.getRed());
            record.lightBuffer.put(ambient.getGreen());
            record.lightBuffer.put(ambient.getBlue());
            record.lightBuffer.put(ambient.getAlpha());
            record.lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0 + index, GL11.GL_AMBIENT, record.lightBuffer);
            lr.ambient.set(ambient);
        }
    }

    private static void setDiffuse(final int index, final LightStateRecord record, final ReadOnlyColorRGBA diffuse,
            final LightRecord lr) {
        if (!record.isValid() || !lr.diffuse.equals(diffuse)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(diffuse.getRed());
            record.lightBuffer.put(diffuse.getGreen());
            record.lightBuffer.put(diffuse.getBlue());
            record.lightBuffer.put(diffuse.getAlpha());
            record.lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0 + index, GL11.GL_DIFFUSE, record.lightBuffer);
            lr.diffuse.set(diffuse);
        }
    }

    private static void setSpecular(final int index, final LightStateRecord record, final ReadOnlyColorRGBA specular,
            final LightRecord lr) {
        if (!record.isValid() || !lr.specular.equals(specular)) {
            record.lightBuffer.clear();
            record.lightBuffer.put(specular.getRed());
            record.lightBuffer.put(specular.getGreen());
            record.lightBuffer.put(specular.getBlue());
            record.lightBuffer.put(specular.getAlpha());
            record.lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0 + index, GL11.GL_SPECULAR, record.lightBuffer);
            lr.specular.set(specular);
        }
    }

    private static void setPosition(final int index, final LightStateRecord record, final float positionX,
            final float positionY, final float positionZ, final float positionW, final LightRecord lr) {
        // From OpenGL Docs:
        // The light position is transformed by the contents of the current top
        // of the ModelView matrix stack when you specify the light position
        // with a call to glLightfv(GL_LIGHT_POSITION,...). If you later change
        // the ModelView matrix, such as when the view changes for the next
        // frame, the light position isn't automatically retransformed by the
        // new contents of the ModelView matrix. If you want to update the
        // light's position, you must again specify the light position with a
        // call to glLightfv(GL_LIGHT_POSITION,...).

        // XXX: This is a hack until we get a better lighting model up
        final ReadOnlyMatrix4 modelViewMatrix = Camera.getCurrentCamera().getModelViewMatrix();

        if (!record.isValid() || lr.position.getXf() != positionX || lr.position.getYf() != positionY
                || lr.position.getZf() != positionZ || lr.position.getWf() != positionW
                || !lr.modelViewMatrix.equals(modelViewMatrix)) {

            record.lightBuffer.clear();
            record.lightBuffer.put(positionX);
            record.lightBuffer.put(positionY);
            record.lightBuffer.put(positionZ);
            record.lightBuffer.put(positionW);
            record.lightBuffer.flip();
            GL11.glLight(GL11.GL_LIGHT0 + index, GL11.GL_POSITION, record.lightBuffer);

            lr.position.set(positionX, positionY, positionZ, positionW);
            if (!Camera.getCurrentCamera().isFrameDirty()) {
                lr.modelViewMatrix.set(modelViewMatrix);
            }
        }
    }

    private static void setSpotDirection(final int index, final LightStateRecord record, final float directionX,
            final float directionY, final float directionZ, final float value) {
        // From OpenGL Docs:
        // The light position is transformed by the contents of the current top
        // of the ModelView matrix stack when you specify the light position
        // with a call to glLightfv(GL_LIGHT_POSITION,...). If you later change
        // the ModelView matrix, such as when the view changes for the next
        // frame, the light position isn't automatically retransformed by the
        // new contents of the ModelView matrix. If you want to update the
        // light's position, you must again specify the light position with a
        // call to glLightfv(GL_LIGHT_POSITION,...).
        record.lightBuffer.clear();
        record.lightBuffer.put(directionX);
        record.lightBuffer.put(directionY);
        record.lightBuffer.put(directionZ);
        record.lightBuffer.put(value);
        record.lightBuffer.flip();
        GL11.glLight(GL11.GL_LIGHT0 + index, GL11.GL_SPOT_DIRECTION, record.lightBuffer);
    }

    private static void setConstant(final int index, final float constant, final LightRecord lr, final boolean force) {
        if (force || constant != lr.getConstant()) {
            GL11.glLightf(GL11.GL_LIGHT0 + index, GL11.GL_CONSTANT_ATTENUATION, constant);
            lr.setConstant(constant);
        }
    }

    private static void setLinear(final int index, final float linear, final LightRecord lr, final boolean force) {
        if (force || linear != lr.getLinear()) {
            GL11.glLightf(GL11.GL_LIGHT0 + index, GL11.GL_LINEAR_ATTENUATION, linear);
            lr.setLinear(linear);
        }
    }

    private static void setQuadratic(final int index, final float quad, final LightRecord lr, final boolean force) {
        if (force || quad != lr.getQuadratic()) {
            GL11.glLightf(GL11.GL_LIGHT0 + index, GL11.GL_QUADRATIC_ATTENUATION, quad);
            lr.setQuadratic(quad);
        }
    }

    private static void setAttenuate(final boolean attenuate, final int index, final Light light,
            final LightStateRecord record, final LightRecord lr) {
        if (attenuate) {
            setConstant(index, light.getConstant(), lr, !record.isValid());
            setLinear(index, light.getLinear(), lr, !record.isValid());
            setQuadratic(index, light.getQuadratic(), lr, !record.isValid());
        } else {
            setConstant(index, 1, lr, !record.isValid());
            setLinear(index, 0, lr, !record.isValid());
            setQuadratic(index, 0, lr, !record.isValid());
        }
        lr.setAttenuate(attenuate);
    }

    private static void setSpotExponent(final int index, final LightStateRecord record, final float exponent,
            final LightRecord lr) {
        if (!record.isValid() || lr.getSpotExponent() != exponent) {
            GL11.glLightf(GL11.GL_LIGHT0 + index, GL11.GL_SPOT_EXPONENT, exponent);
            lr.setSpotExponent(exponent);
        }
    }

    private static void setSpotCutoff(final int index, final LightStateRecord record, final float cutoff,
            final LightRecord lr) {
        if (!record.isValid() || lr.getSpotCutoff() != cutoff) {
            GL11.glLightf(GL11.GL_LIGHT0 + index, GL11.GL_SPOT_CUTOFF, cutoff);
            lr.setSpotCutoff(cutoff);
        }
    }
}
