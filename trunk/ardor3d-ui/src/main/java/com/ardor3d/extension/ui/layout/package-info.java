/**
 * Provides classes that position and resize components within a given UI container.  
 * Layouts can be reused across multiple UI containers, but if done, layout should be done 
 * on a single thread as individual layouts may use member fields in a way that is not thread safe.
 */

package com.ardor3d.extension.ui.layout;