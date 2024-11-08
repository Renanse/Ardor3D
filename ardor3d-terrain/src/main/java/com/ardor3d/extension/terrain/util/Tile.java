
package com.ardor3d.extension.terrain.util;

import java.io.Serial;
import java.io.Serializable;
import java.text.MessageFormat;

public class Tile implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private final int x, y;

  public Tile(final int x, final int y) {
    this.x = x;
    this.y = y;
  }

  public int getX() { return x; }

  public int getY() { return y; }

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
    if (!(obj instanceof Tile other)) {
      return false;
    }
    return x == other.x && y == other.y;
  }

  @Override
  public String toString() {
    return MessageFormat.format("Tile [x={0}, y={1}]", x, y);
  }
}
