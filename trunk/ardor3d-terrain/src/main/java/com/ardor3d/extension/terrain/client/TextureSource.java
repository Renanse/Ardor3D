
package com.ardor3d.extension.terrain.client;

import java.nio.ByteBuffer;
import java.util.Set;

import com.ardor3d.extension.terrain.util.Tile;

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
     * Returns which tiles that contain data in the requested region.
     * 
     * @param clipmapLevel
     * @param tileX
     * @param tileY
     * @param numTilesX
     * @param numTilesY
     * @return
     * @throws Exception
     */
    Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, int numTilesX, int numTilesY)
            throws Exception;

    /**
     * Returns which tiles that should be marked as invalid and updated in the requested region.
     * 
     * @param clipmapLevel
     * @param tileX
     * @param tileY
     * @param numTilesX
     * @param numTilesY
     * @return
     * @throws Exception
     */
    Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, int numTilesX, int numTilesY)
            throws Exception;

    /**
     * Returns the contributing source id for the requested tile.
     * 
     * @param clipmapLevel
     * @param tile
     * @return
     */
    int getContributorId(int clipmapLevel, Tile tile);

    /**
     * Request for texture data for a tile.
     * 
     * @param clipmapLevel
     * @param tile
     * @return
     * @throws Exception
     */
    ByteBuffer getTile(int clipmapLevel, final Tile tile) throws Exception;
}
