/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ardor3d.extension.ui;

/**
 *
 * @author Ravklok
 */
public interface IComponent {
    
    public UIHud getHud();

    public int getContentWidth();

    public int getTotalWidth();

    public int getContentHeight();

    public int getTotalHeight();

    public void show();

    public void hide();

    public void updateGeometricState(final double time);

}
