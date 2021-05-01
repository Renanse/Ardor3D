/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import java.util.HashMap;

public class BinaryClassObject {

  // When exporting, use nameFields field, importing use aliasFields.
  public HashMap<String, BinaryClassField> _nameFields;
  public HashMap<Byte, BinaryClassField> _aliasFields;

  public byte[] _alias;
  public String _className;

}
