/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.io.IOException;

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Holds data related to asset info.
 */
public class AssetData implements Savable {

    private String author;
    private String authoringTool;
    private String comments;
    private String copyright;
    private String sourceData;
    private String created;
    private String keywords;
    private String modified;
    private String revision;
    private String subject;
    private String title;
    private String unitName;
    private float unitMeter;
    private ReadOnlyVector3 upAxis;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getAuthoringTool() {
        return authoringTool;
    }

    public void setAuthoringTool(final String authoringTool) {
        this.authoringTool = authoringTool;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(final String copyright) {
        this.copyright = copyright;
    }

    public String getSourceData() {
        return sourceData;
    }

    public void setSourceData(final String sourceData) {
        this.sourceData = sourceData;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(final String created) {
        this.created = created;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(final String revision) {
        this.revision = revision;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(final String unitName) {
        this.unitName = unitName;
    }

    public float getUnitMeter() {
        return unitMeter;
    }

    public void setUnitMeter(final float unitMeter) {
        this.unitMeter = unitMeter;
    }

    /**
     * @return the up axis as defined in the &lt;asset> tag, or null if not existing.
     */
    public ReadOnlyVector3 getUpAxis() {
        return upAxis;
    }

    public void setUpAxis(final ReadOnlyVector3 upAxis) {
        this.upAxis = upAxis;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends AssetData> getClassTag() {
        return getClass();
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        author = capsule.readString("author", null);
        authoringTool = capsule.readString("authoringTool", null);
        comments = capsule.readString("comments", null);
        copyright = capsule.readString("copyright", null);
        sourceData = capsule.readString("sourceData", null);
        created = capsule.readString("created", null);
        keywords = capsule.readString("keywords", null);
        modified = capsule.readString("modified", null);
        revision = capsule.readString("revision", null);
        subject = capsule.readString("subject", null);
        title = capsule.readString("title", null);
        unitName = capsule.readString("unitName", null);
        unitMeter = capsule.readFloat("unitMeter", 0f);
        upAxis = (ReadOnlyVector3) capsule.readSavable("upAxis", null);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(author, "author", null);
        capsule.write(authoringTool, "authoringTool", null);
        capsule.write(comments, "comments", null);
        capsule.write(copyright, "copyright", null);
        capsule.write(sourceData, "sourceData", null);
        capsule.write(created, "created", null);
        capsule.write(keywords, "keywords", null);
        capsule.write(modified, "modified", null);
        capsule.write(revision, "revision", null);
        capsule.write(subject, "subject", null);
        capsule.write(title, "title", null);
        capsule.write(unitName, "unitName", null);
        capsule.write(unitMeter, "unitMeter", 0f);
        if (upAxis instanceof Savable) {
            capsule.write((Savable) upAxis, "upAxis", null);
        }
    }
}
