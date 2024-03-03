/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

public class AwtCopyPasteImpl implements CopyPasteImpl {

  @Override
  public String getClipBoardContents() {
    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    final Transferable clipboardContent = clipboard.getContents(null);

    if (clipboardContent != null && clipboardContent.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        return (String) clipboardContent.getTransferData(DataFlavor.stringFlavor);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public void setClipBoardContents(final String contents) {
    final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    final StringSelection string = new StringSelection(contents);
    clipboard.setContents(string, null);
  }
}
