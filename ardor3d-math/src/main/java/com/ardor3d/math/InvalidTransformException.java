
package com.ardor3d.math;

import java.io.Serial;

public class InvalidTransformException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public InvalidTransformException() {
    super();
  }

  public InvalidTransformException(final String desc) {
    super(desc);
  }

  public InvalidTransformException(final Throwable cause) {
    super(cause);
  }

  public InvalidTransformException(final String desc, final Throwable cause) {
    super(desc, cause);
  }
}
