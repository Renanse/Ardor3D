/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The DOMSerializer was based primarily off the DOMSerializer.java class from the "Java and XML" 3rd Edition book by
 * Brett McLaughlin, and Justin Edelson. Some modifications were made to support formatting of elements and attributes.
 * 
 */
public class DOMSerializer {

    /** Indentation to use (default is no indentation) */
    private String _indent = "";

    /** Line separator to use (default is for Windows) */
    private String _lineSeparator = "\n";

    /** Encoding for output (default is UTF-8) */
    private String _encoding = "UTF8";

    /** Attributes will be displayed on seperate lines */
    private final boolean _displayAttributesOnSeperateLine = true;

    public void setLineSeparator(final String lineSeparator) {
        _lineSeparator = lineSeparator;
    }

    public void setEncoding(final String encoding) {
        _encoding = encoding;
    }

    public void setIndent(final int numSpaces) {
        final StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < numSpaces; i++) {
            buffer.append('\t');
        }
        _indent = buffer.toString();
    }

    public void serialize(final Document doc, final OutputStream out) throws IOException {
        final Writer writer = new OutputStreamWriter(out, _encoding);
        serialize(doc, writer);
    }

    public void serialize(final Document doc, final File file) throws IOException {
        final Writer writer = new FileWriter(file);
        serialize(doc, writer);
    }

    public void serialize(final Document doc, final Writer writer) throws IOException {
        // Start serialization recursion with no indenting
        serializeNode(doc, writer, "");
        writer.flush();
    }

    private void serializeNode(final Node node, final Writer writer, final String indentLevel) throws IOException {
        // Determine action based on node type
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                final Document doc = (Document) node;
                /**
                 * DOM Level 2 code writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                 */
                writer.write("<?xml version=\"");
                writer.write(doc.getXmlVersion());
                writer.write("\" encoding=\"UTF-8\" standalone=\"");
                if (doc.getXmlStandalone()) {
                    writer.write("yes");
                } else {
                    writer.write("no");
                }
                writer.write("\"");
                writer.write("?>");
                writer.write(_lineSeparator);

                // recurse on each top-level node
                final NodeList nodes = node.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        serializeNode(nodes.item(i), writer, "");
                    }
                }
                break;
            case Node.ELEMENT_NODE:
                final String name = node.getNodeName();
                // writer.write(indentLevel + "<" + name);
                writer.write("<" + name);
                final NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    final Node current = attributes.item(i);
                    String attributeSeperator = " ";
                    if (_displayAttributesOnSeperateLine && i != 0) {
                        attributeSeperator = _lineSeparator + indentLevel + _indent;
                    }
                    // Double indentLevel to match parent element and then one indention to format below parent
                    final String attributeStr = attributeSeperator + current.getNodeName() + "=\"";
                    writer.write(attributeStr);
                    print(writer, current.getNodeValue());
                    writer.write("\"");
                }
                writer.write(">");

                // recurse on each child
                final NodeList children = node.getChildNodes();
                if (children != null) {
                    if ((children.item(0) != null) && (children.item(0).getNodeType() == Node.ELEMENT_NODE)) {
                        // writer.write(lineSeparator);
                    }

                    for (int i = 0; i < children.getLength(); i++) {
                        serializeNode(children.item(i), writer, indentLevel + _indent);
                    }

                    if ((children.item(0) != null)
                            && (children.item(children.getLength() - 1).getNodeType() == Node.ELEMENT_NODE)) {
                        ;// writer.write(indentLevel);
                    }
                }

                writer.write("</" + name + ">");
                // writer.write(lineSeparator);
                break;
            case Node.TEXT_NODE:
                print(writer, node.getNodeValue());
                break;
            case Node.CDATA_SECTION_NODE:
                writer.write("<![CDATA[");
                print(writer, node.getNodeValue());
                writer.write("]]>");
                break;
            case Node.COMMENT_NODE:
                writer.write(indentLevel + "<!-- " + node.getNodeValue() + " -->");
                writer.write(_lineSeparator);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                writer.write("<?" + node.getNodeName() + " " + node.getNodeValue() + "?>");
                writer.write(_lineSeparator);
                break;
            case Node.ENTITY_REFERENCE_NODE:
                writer.write("&" + node.getNodeName() + ";");
                break;
            case Node.DOCUMENT_TYPE_NODE:
                final DocumentType docType = (DocumentType) node;
                final String publicId = docType.getPublicId();
                final String systemId = docType.getSystemId();
                final String internalSubset = docType.getInternalSubset();
                writer.write("<!DOCTYPE " + docType.getName());
                if (publicId != null) {
                    writer.write(" PUBLIC \"" + publicId + "\" ");
                } else {
                    writer.write(" SYSTEM ");
                }
                writer.write("\"" + systemId + "\"");
                if (internalSubset != null) {
                    writer.write(" [" + internalSubset + "]");
                }
                writer.write(">");
                writer.write(_lineSeparator);
                break;
        }
    }

    private void print(final Writer writer, final String s) throws IOException {

        if (s == null) {
            return;
        }
        for (int i = 0, len = s.length(); i < len; i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '<':
                    writer.write("&lt;");
                    break;
                case '>':
                    writer.write("&gt;");
                    break;
                case '&':
                    writer.write("&amp;");
                    break;
                case '\r':
                    writer.write("&#xD;");
                    break;
                default:
                    writer.write(c);
            }
        }
    }

}
