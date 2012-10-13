
package com.ardor3d.example.renderer.utils.atlas;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import com.ardor3d.extension.atlas.AtlasNode;
import com.ardor3d.extension.atlas.AtlasPacker;
import com.ardor3d.extension.atlas.AtlasRectangle;

public class AtlasPainter extends JPanel {
    private static final long serialVersionUID = 1L;

    private final AtlasPacker packer;

    public AtlasPainter(final AtlasPacker packer) {
        this.packer = packer;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        final Graphics2D g2 = (Graphics2D) g;
        g2.translate(30, 30);
        recursiveDrawNode(g2, packer.getRootNode());
    }

    private void recursiveDrawNode(final Graphics2D g2, final AtlasNode node) {
        if (node == null) {
            return;
        }
        final AtlasRectangle im = node.getRectangle();
        if (node.isSet()) {
            g2.setColor(Color.red);
        } else {
            g2.setColor(Color.green);
        }
        g2.fillRect(im.getX(), im.getY(), im.getWidth(), im.getHeight());
        if (node.isSet()) {
            g2.setColor(Color.red.darker());
        } else {
            g2.setColor(Color.green.darker());
        }
        g2.drawRect(im.getX(), im.getY(), im.getWidth(), im.getHeight());
        recursiveDrawNode(g2, node.getChild(0));
        recursiveDrawNode(g2, node.getChild(1));
    }
}
