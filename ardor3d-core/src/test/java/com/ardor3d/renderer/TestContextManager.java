/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.util.Constants;

/**
 * Covers {@link ContextManager#addContext}'s diagnostic warning: when more than one distinct GL
 * context is registered while {@link Constants#useMultipleContexts} is off, per-context GL resources
 * (VAOs, FBOs, ...) silently collapse to a single shared value, which is a rendering-corruption
 * hazard. The engine should say so, once.
 */
public class TestContextManager {

  private final List<LogRecord> _records = new ArrayList<>();
  private final Logger _logger = Logger.getLogger(ContextManager.class.getName());
  private Handler _handler;
  private Level _priorLevel;
  private boolean _priorUseParent;

  @Before
  public void setUp() {
    // ContextManager is all-static shared state; isolate each test from the others.
    ContextManager.contextStore.clear();
    ContextManager.currentContext = null;
    ContextManager._warnedSingleContextMode = false;

    _records.clear();
    _handler = new Handler() {
      @Override
      public void publish(final LogRecord record) {
        _records.add(record);
      }

      @Override
      public void flush() {}

      @Override
      public void close() {}
    };
    _priorLevel = _logger.getLevel();
    _priorUseParent = _logger.getUseParentHandlers();
    _logger.addHandler(_handler);
    _logger.setLevel(Level.ALL);
    _logger.setUseParentHandlers(false);
  }

  @After
  public void tearDown() {
    _logger.removeHandler(_handler);
    _logger.setLevel(_priorLevel);
    _logger.setUseParentHandlers(_priorUseParent);
    ContextManager.contextStore.clear();
    ContextManager.currentContext = null;
    ContextManager._warnedSingleContextMode = false;
  }

  private long warningCount() {
    return _records.stream().filter(r -> r.getLevel() == Level.WARNING).count();
  }

  @Test
  public void singleContextDoesNotWarn() {
    ContextManager.addContext("canvasA", new RenderContext("canvasA"));
    assertEquals("A single registered context is normal and must never warn.", 0, warningCount());
  }

  @Test
  public void reAddingSameKeyDoesNotWarn() {
    final Object key = "canvasA";
    ContextManager.addContext(key, new RenderContext(key));
    // Re-registering the same canvas key models a context recreation (e.g. on resize), not a
    // second coexisting context, so it must not trip the warning.
    ContextManager.addContext(key, new RenderContext(key));
    assertEquals("Re-registering one canvas key is recreation, not a second context.", 0, warningCount());
  }

  @Test
  public void secondDistinctContextWarnsOnceInSingleContextMode() {
    // Only meaningful in the default single-context mode; skip if the JVM enabled multi-context.
    Assume.assumeFalse(Constants.useMultipleContexts);

    ContextManager.addContext("canvasA", new RenderContext("canvasA"));
    assertEquals("First context must not warn.", 0, warningCount());

    ContextManager.addContext("canvasB", new RenderContext("canvasB"));
    assertEquals("A second distinct context without useMultipleContexts must warn.", 1, warningCount());

    // The diagnostic is one-time: further contexts must not spam the log.
    ContextManager.addContext("canvasC", new RenderContext("canvasC"));
    assertEquals("The unsafe-multi-context warning must fire at most once.", 1, warningCount());
  }

  @Test
  public void multipleContextsDoNotWarnWhenModeEnabled() {
    // When the per-context tracking is correctly enabled, multiple contexts are normal.
    Assume.assumeTrue(Constants.useMultipleContexts);

    ContextManager.addContext("canvasA", new RenderContext("canvasA"));
    ContextManager.addContext("canvasB", new RenderContext("canvasB"));
    assertEquals("Multiple contexts are correct when useMultipleContexts is on.", 0, warningCount());
  }
}
