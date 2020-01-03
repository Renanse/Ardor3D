
package com.ardor3d.extension.terrain.client;

import java.nio.ByteBuffer;
import java.util.Set;

import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

/**
 * Feeds texture data to a TextureCache
 */
public interface TextureSource {
    /**
     * Called to initialize and setup the texture clipmap.
     *
     * @param mapID
     *            Map to get configuration for.
     * @return
     * @throws Exception
     */
    TextureConfiguration getConfiguration() throws Exception;

    /**
     * Returns which tiles contain data in the requested region. May return null to indicate all tiles are invalid, or
     * that this source does not support determining valid tiles.
     *
     * @param clipmapLevel
     * @param tileX
     * @param tileY
     * @param numTilesX
     * @param numTilesY
     * @return
     * @throws Exception
     */
    default Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        return null;
    }

    /**
     * Returns which tiles should be marked as invalid and updated in the requested region. May return null to indicate
     * no tiles are invalid, or this source does not support invalid tiles.
     *
     * @param clipmapLevel
     * @param tileX
     * @param tileY
     * @param numTilesX
     * @param numTilesY
     * @return
     * @throws Exception
     */
    default Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        return null;
    }

    /**
     * Returns the contributing source id for the requested tile.
     *
     * @param clipmapLevel
     * @param tile
     * @return
     */
    default int getContributorId(final int clipmapLevel, final Tile tile) {
        return 0;
    }

    /**
     * Request for texture data for a tile.
     *
     * @param clipmapLevel
     * @param tile
     * @return
     * @throws Exception
     */
    ByteBuffer getTile(int clipmapLevel, final Tile tile) throws Exception;

    /**
     * @return a display name for this source.
     */
    String getName();

    /**
     * @param value
     *            new display name for this source.
     */
    void setName(String value);

    /**
     * @return a color used to tint clipmap textures produced from this source at render time.
     */
    ReadOnlyColorRGBA getTintColor();

    /**
     * @param value
     *            a color used to tint clipmap textures produced from this source at render time.
     */
    void setTintColor(ReadOnlyColorRGBA value);
}
