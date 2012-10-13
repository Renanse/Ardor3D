
package com.ardor3d.extension.terrain.util;

import java.io.Serializable;

public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int x, y;

    public Tile(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * result + x;
        result += 31 * result + y;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Tile)) {
            return false;
        }
        final Tile other = (Tile) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        return "Tile [x=" + x + ", y=" + y + "]";
    }
}
