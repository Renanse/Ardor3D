
package com.ardor3d.extension.model.collada.jdom.data;

import org.jdom.Element;

import com.ardor3d.scenegraph.Node;

public class ControllerStore {
    public final Node ardorParentNode;
    public final Element instanceController;

    public ControllerStore(final Node ardorParentNode, final Element instanceController) {
        this.ardorParentNode = ardorParentNode;
        this.instanceController = instanceController;
    }
}
