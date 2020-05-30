/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.MainThread;
import com.ardor3d.util.Timer;

/**
 * Does the work needed in a given frame.
 */
public final class FrameHandler {
  private static final Logger logger = Logger.getLogger(FrameHandler.class.toString());

  /**
   * Thread synchronization of the updaters list is delegated to the CopyOnWriteArrayList.
   */
  private final CopyOnWriteArrayList<Updater> _updaters;

  /**
   * Canvases is both protected by an intrinsic lock and by the fact that it is a
   * CopyOnWriteArrayList. This is because it is necessary to check the size of the list and then
   * fetch an iterator that is guaranteed to iterate over that number of elements. See
   * {@link #updateFrame()} for the code that does this.
   */
  @GuardedBy("this")
  private final CopyOnWriteArrayList<Canvas> _canvases;
  private final Timer _timer;

  /**
   * Number of seconds we'll wait for a latch to count down to 0. Default is 5.
   */
  private long _timeoutSeconds = 5;

  public FrameHandler(final Timer timer) {
    _timer = timer;
    _updaters = new CopyOnWriteArrayList<>();
    _canvases = new CopyOnWriteArrayList<>();
  }

  @MainThread
  public void updateFrame() {
    // calculate tpf
    // update updaters
    // draw canvases

    _timer.update();

    // using the CopyOnWriteArrayList synchronization here, since that means
    // that we don't have to hold any locks while calling Updater.update(double),
    // and also makes the code simple. An updater that is registered after the below
    // loop has started will be updated at the next call to updateFrame().
    for (final Updater updater : _updaters) {
      updater.update(_timer);
    }

    int numCanvases;
    Iterator<Canvas> iterator;

    // make sure that there is no race condition with addCanvas - getting the iterator and
    // the number of canvases currently in the list in a synchronized section, and ensuring that
    // the addCanvas() method is also synchronized on this, means that they will
    // both remain valid outside the section later, when we call the probably long-running, alien
    // draw() methods. Since 'canvases' is a CopyOnWriteArrayList, the iterator is guaranteed to
    // be valid outside the synchronized section, and getting them both inside the synchronized section
    // means that the number of canvases read will be the same as the number of elements the iterator
    // will iterate over.
    synchronized (this) {
      numCanvases = _canvases.size();
      iterator = _canvases.iterator();
    }

    final CountDownLatch latch = new CountDownLatch(numCanvases);

    while (iterator.hasNext()) {
      iterator.next().draw(latch);
    }

    try {
      // wait for all canvases to be drawn - the reason for using the latch is that
      // in some cases (AWT, for instance), the thread that calls canvas.draw() is not the
      // one that holds the OpenGL context, which means that drawing is simply queued.
      // When the actual OpenGL rendering has been done, the OpenGL thread will countdown
      // on the latch, and once all the canvases have finished rendering, this method
      // will return.
      final boolean success = latch.await(_timeoutSeconds, TimeUnit.SECONDS);

      if (!success) {
        logger.logp(Level.SEVERE, FrameHandler.class.toString(), "updateFrame", "Timeout while waiting for renderers");
        // FIXME: should probably reset update flag in canvases?
      }
    } catch (final InterruptedException e) {
      // restore updated status
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Add an updater to the frame handler.
   * <p>
   * The frame handler calls the {@link Updater#update(com.ardor3d.util.ReadOnlyTimer) update} method
   * of each updater that has been added to it once per frame, before rendering begins.
   * <p>
   * <strong>Note:</strong> that is the frame handler has already been initialized then the updater
   * will <em>not</em> have it's {@code init} method called automatically, it is up to the client code
   * to perform any initialization explicitly under this scenario.
   *
   * @param updater
   *          the updater to add.
   */
  public void addUpdater(final Updater updater) {
    _updaters.addIfAbsent(updater);
  }

  /**
   * Remove an updater from the frame handler.
   *
   * @param updater
   *          the updater to remove.
   * @return {@code true} if the updater was removed, {@code false} otherwise (which will happen if,
   *         for example, the updater had not previously been added to the frame handler).
   */
  public boolean removeUpdater(final Updater updater) {
    return _updaters.remove(updater);
  }

  /**
   * Add a canvas to the frame handler.
   * <p>
   * The frame handler calls the {@link Canvas#draw(java.util.concurrent.CountDownLatch)} draw} method
   * of each canvas that has been added to it once per frame, after updating is complete.
   * <p>
   * <strong>Note:</strong> that if the frame handler has already been initialized then the canvas
   * will <em>not</em> have it's {@code init} method called automatically, it is up to the client code
   * to perform any initialization explicitly under this scenario.
   *
   * @param canvas
   *          the canvas to add.
   */
  public synchronized void addCanvas(final Canvas canvas) {
    _canvases.addIfAbsent(canvas);
  }

  /**
   * Remove a canvas from the frame handler.
   *
   * @param canvas
   *          the canvas to remove.
   * @return {@code true} if the canvas was removed, {@code false} otherwise (which will happen if,
   *         for example, the canvas had not previously been added to the frame handler).
   */
  public synchronized boolean removeCanvas(final Canvas canvas) {
    return _canvases.remove(canvas);
  }

  public void init() {
    // TODO: this can lead to problems with canvases and updaters added after init() has been called
    // once...
    for (final Canvas canvas : _canvases) {
      canvas.init();
    }

    for (final Updater updater : _updaters) {
      updater.init();
    }
  }

  public long getTimeoutSeconds() { return _timeoutSeconds; }

  public void setTimeoutSeconds(final long timeoutSeconds) { _timeoutSeconds = timeoutSeconds; }

  public Timer getTimer() { return _timer; }
}
