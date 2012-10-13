
package com.ardor3d.extension.terrain.util;

import java.io.Serializable;
import java.net.URL;

public class TileLocator implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Tile tile;
    private final int sourceId;
    private final int clipmapLevel;
    private final URL url;

    public TileLocator(final Tile tile, final int sourceId, final int clipmapLevel, final URL url) {
        this.tile = tile;
        this.sourceId = sourceId;
        this.clipmapLevel = clipmapLevel;
        this.url = url;
    }

    public Tile getTile() {
        return tile;
    }

    public int getSourceId() {
        return sourceId;
    }

    public int getClipmapLevel() {
        return clipmapLevel;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + clipmapLevel;
        result = prime * result + sourceId;
        result = prime * result + (tile == null ? 0 : tile.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TileLocator)) {
            return false;
        }
        final TileLocator other = (TileLocator) obj;
        if (clipmapLevel != other.clipmapLevel) {
            return false;
        }
        if (sourceId != other.sourceId) {
            return false;
        }
        if (tile == null) {
            if (other.tile != null) {
                return false;
            }
        } else if (!tile.equals(other.tile)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TileLocator [clipmapLevel=" + clipmapLevel + ", sourceId=" + sourceId + ", tile=" + tile + ", url="
                + url + "]";
    }
}
