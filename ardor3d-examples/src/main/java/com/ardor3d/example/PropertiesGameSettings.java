/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * <code>PropertiesGameSettings</code> handles loading and saving a properties file that defines the
 * display settings. A property file is identified during creation of the object. The properties
 * file should have the following format:
 *
 * <PRE>
 * &lt;CODE&gt;
 * FREQ=60
 * RENDERER=LWJGL
 * WIDTH=1280
 * HEIGHT=1024
 * DEPTH=32
 * FULLSCREEN=false
 * &lt;/CODE&gt;
 * </PRE>
 */
public class PropertiesGameSettings {
  private static final Logger logger = Logger.getLogger(PropertiesGameSettings.class.getName());

  /**
   * The default width, used if there is a problem with the properties file.
   */
  static int DEFAULT_WIDTH = 640;
  /**
   * The default height, used if there is a problem with the properties file.
   */
  static int DEFAULT_HEIGHT = 480;
  /**
   * The default depth, used if there is a problem with the properties file.
   */
  static int DEFAULT_DEPTH = 16;
  /**
   * The default frequency, used if there is a problem with the properties file.
   */
  static int DEFAULT_FREQUENCY = 60;
  /**
   * The default fullscreen flag, used if there is a problem with the properties file.
   */
  static boolean DEFAULT_FULLSCREEN = false;
  /**
   * The default renderer flag, used if there is a problem with the properties file.
   */
  static String DEFAULT_RENDERER = "LWJGL";

  static boolean DEFAULT_VERTICAL_SYNC = true;
  static int DEFAULT_DEPTH_BITS = 8;
  static int DEFAULT_ALPHA_BITS = 0;
  static int DEFAULT_STENCIL_BITS = 0;
  static int DEFAULT_SAMPLES = 0;
  static boolean DEFAULT_MUSIC = true;
  static boolean DEFAULT_SFX = true;
  static int DEFAULT_FRAMERATE = -1;

  protected boolean isNew = true;

  // These are all objects so it is very clear when they have been
  // explicitly set.
  protected static Integer defaultWidth = null;
  protected static Integer defaultHeight = null;
  protected static Integer defaultDepth = null;
  protected static Integer defaultFrequency = null;
  protected static Boolean defaultFullscreen = null;
  protected static String defaultRenderer = null;
  protected static Boolean defaultVerticalSync = null;
  protected static Integer defaultDepthBits = null;
  protected static Integer defaultAlphaBits = null;
  protected static Integer defaultStencilBits = null;
  protected static Integer defaultSamples = null;
  protected static Boolean defaultMusic = null;
  protected static Boolean defaultSFX = null;
  protected static Integer defaultFramerate = null;
  protected static String defaultSettingsWidgetImage = null;
  private static boolean defaultsAssigned = false;

  // property object
  private final Properties prop;

  // the file that contains our properties.
  private final String filename;

  private static boolean dfltsInitted = false;

  /**
   * Constructor creates the <code>PropertiesGameSettings</code> object for use.
   * 
   * @param personalFilename
   *          the properties file to use, read from filesystem. Must not be null.
   * @param dfltsFilename
   *          the properties file to use, read from CLASSPATH. Null to not seek any runtime defaults
   *          file.
   * @throws Ardor3dException
   *           if the personalFilename is null.
   */
  public PropertiesGameSettings(final String personalFilename, final String dfltsFilename) {
    if (null == personalFilename) {
      throw new Ardor3dException("Must give a valid filename");
    }
    if (!dfltsInitted) {
      dfltsInitted = true;
      // default* setting values are static, therefore, regardless of
      // how many GameSettings we instantiate, the defaults are
      // assigned only once.
      assignDefaults(dfltsFilename);
    }

    filename = personalFilename;
    isNew = !(new File(filename).isFile());
    prop = new Properties();
    load();
  }

  public void clear() {
    prop.clear();
  }

  /**
   * <code>get</code> takes an arbitrary string as a key and returns any value associated with it,
   * null if none.
   * 
   * @param key
   *          the key to use for data retrieval.
   * @return the string associated with the key, null if none.
   */
  public String get(final String key) {
    return prop.getProperty(key);
  }

  public String get(final String name, final String defaultValue) {
    final String value = get(name);
    return (value == null) ? defaultValue : value;
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public int getAlphaBits() {
    final String s = prop.getProperty("ALPHA_BITS");
    return (s == null) ? defaultAlphaBits : Integer.parseInt(s);
  }

  public boolean getBoolean(final String name, final boolean defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : Boolean.parseBoolean(stringValue);
  }

  public byte[] getByteArray(final String name, final byte[] defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : stringValue.getBytes();
  }

  /**
   * This is only getting the "default" value, which may not be changed by end-users.
   */
  public String getDefaultSettingsWidgetImage() { return defaultSettingsWidgetImage; }

  /**
   * <code>getDepth</code> returns the depth as read from the properties file. If the properties file
   * does not contain depth or was not read properly, the default depth is returned.
   * 
   * @return the depth determined by the properties file, or the default.
   */
  public int getDepth() {
    final String d = prop.getProperty("DEPTH");
    if (null == d) {
      return defaultDepth;
    }

    return Integer.parseInt(d);
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public int getDepthBits() {
    final String s = prop.getProperty("DEPTH_BITS");
    return (s == null) ? defaultDepthBits : Integer.parseInt(s);
  }

  public double getDouble(final String name, final double defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : Double.parseDouble(stringValue);
  }

  public float getFloat(final String name, final float defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : Float.parseFloat(stringValue);
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public int getFramerate() {
    final String s = prop.getProperty("FRAMERATE");
    return (s == null) ? defaultFramerate : Integer.parseInt(s);
  }

  /**
   * <code>getFrequency</code> returns the frequency of the monitor as read from the properties file.
   * If the properties file does not contain frequency or was not read properly the default frequency
   * is returned.
   * 
   * @return the frequency determined by the properties file, or the default.
   */
  public int getFrequency() {
    final String f = prop.getProperty("FREQ");
    if (null == f) {
      return defaultFrequency;
    }

    return Integer.parseInt(f);
  }

  /**
   * Legacy method.
   * 
   * @deprecated Use method isFullscreen instead.
   * @see #isFullscreen()
   */
  @Deprecated
  public boolean getFullscreen() { return isFullscreen(); }

  /**
   * <code>getHeight</code> returns the height as read from the properties file. If the properties
   * file does not contain height or was not read properly, the default height is returned.
   * 
   * @return the height determined by the properties file, or the default.
   */
  public int getHeight() {
    final String h = prop.getProperty("HEIGHT");
    if (null == h) {
      return defaultHeight;
    }

    return Integer.parseInt(h);
  }

  public int getInt(final String name, final int defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : Integer.parseInt(stringValue);
  }

  public long getLong(final String name, final long defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : Long.parseLong(stringValue);
  }

  public Object getObject(final String name, final Object defaultValue) {
    final String stringValue = get(name);
    return (stringValue == null) ? defaultValue : stringValue;
  }

  /**
   * 
   * <code>getRenderer</code> returns the requested rendering API, or the default.
   * 
   * @return the rendering API or the default.
   */
  public String getRenderer() {
    final String renderer = prop.getProperty("RENDERER");
    if (null == renderer) {
      return defaultRenderer;
    }

    return renderer;
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public int getSamples() {
    final String s = prop.getProperty("SAMPLES");
    return (s == null) ? defaultSamples : Integer.parseInt(s);
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public int getStencilBits() {
    final String s = prop.getProperty("STENCIL_BITS");
    return (s == null) ? defaultStencilBits : Integer.parseInt(s);
  }

  /**
   * <code>getWidth</code> returns the width as read from the properties file. If the properties file
   * does not contain width or was not read properly, the default width is returned.
   * 
   * @return the width determined by the properties file, or the default.
   */
  public int getWidth() {
    final String w = prop.getProperty("WIDTH");
    if (null == w) {
      return defaultWidth;
    }

    return Integer.parseInt(w);
  }

  /**
   * <code>isFullscreen</code> returns the fullscreen flag as read from the properties file. If the
   * properties file does not contain the fullscreen flag or was not read properly, the default value
   * is returned.
   * 
   * @return the fullscreen flag determined by the properties file, or the default.
   */
  public boolean isFullscreen() {
    final String f = prop.getProperty("FULLSCREEN");
    if (null == f) {
      return defaultFullscreen;
    }

    return Boolean.valueOf(prop.getProperty("FULLSCREEN"));
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public boolean isMusic() {
    final String s = prop.getProperty("MUSIC");
    return (s == null) ? defaultMusic : Boolean.parseBoolean(s);
  }

  public boolean isNew() { return isNew; }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public boolean isSFX() {
    final String s = prop.getProperty("SFX");
    return (s == null) ? defaultSFX : Boolean.parseBoolean(s);
  }

  /**
   * If the properties file does not contain the setting or was not read properly, the default value
   * is returned.
   * 
   * @throws InternalError
   *           in all cases
   */
  public boolean isVerticalSync() {
    final String s = prop.getProperty("VERTICAL_SYNC");
    return (s == null) ? defaultVerticalSync : Boolean.parseBoolean(s);
  }

  /**
   * <code>load</code> attempts to load the properties file defined during instantiation and put all
   * properties in the table. If there is a problem loading or reading the file, false is returned. If
   * all goes well, true is returned.
   * 
   * @return the success of the load, true indicated success and false indicates failure.
   */
  public boolean load() {
    FileInputStream fin = null;
    try {
      fin = new FileInputStream(filename);
    } catch (final FileNotFoundException e) {
      logger.warning("Could not load properties. Creating a new one.");
      return false;
    }

    try {
      prop.load(fin);
      fin.close();
    } catch (final IOException e) {
      logger.warning("Could not load properties. Creating a new one.");
      return false;
    }

    // confirm that the properties file has all the data we need.
    if (null == prop.getProperty("WIDTH") || null == prop.getProperty("HEIGHT") || null == prop.getProperty("DEPTH")
        || null == prop.getProperty("FULLSCREEN")) {
      logger.warning("Properties file not complete.");
      return false;
    }

    logger.finer("Read properties");
    return true;
  }

  /**
   * Removes specified property, if present.
   */
  public void remove(final String name) {
    prop.remove(name);
  }

  /**
   * Persists current property mappings to designated file, overwriting if file already present.
   * 
   * @throws IOException
   *           for I/O failures
   */
  public void save() throws IOException {
    final FileOutputStream fout = new FileOutputStream(filename);
    prop.store(fout, "Game Settings written by " + getClass().getName() + " at " + new java.util.Date());

    fout.close();
    logger.finer("Saved properties");
  }

  /**
   * <code>save(int, int, int, int, boolean, String)</code> overwrites the properties file with the
   * given parameters.
   * 
   * @param width
   *          the width of the resolution.
   * @param height
   *          the height of the resolution.
   * @param depth
   *          the bits per pixel.
   * @param freq
   *          the frequency of the monitor.
   * @param fullscreen
   *          use fullscreen or not.
   * @deprecated
   * @return true if save was successful, false otherwise.
   */
  @Deprecated
  public boolean save(final int width, final int height, final int depth, final int freq, final boolean fullscreen,
      final String renderer) {

    prop.clear();
    setWidth(width);
    setHeight(height);
    setDepth(depth);
    setFrequency(freq);
    setFullscreen(fullscreen);
    setRenderer(renderer);

    try {
      save();
    } catch (final IOException e) {
      logger.warning("Could not save properties: " + e);
      return false;
    }
    return true;
  }

  /**
   * Sets a property.
   */
  public void set(final String name, final String value) {
    prop.setProperty(name, value);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setAlphaBits(final int alphaBits) {
    setInt("ALPHA_BITS", alphaBits);
  }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setBoolean(final String name, final boolean value) {
    set(name, Boolean.toString(value));
  }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setByteArray(final String name, final byte[] value) {
    set(name, new String(value));
  }

  public void setDepth(final int depth) {
    setInt("DEPTH", depth);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setDepthBits(final int depthBits) {
    setInt("DEPTH_BITS", depthBits);
  }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setDouble(final String name, final double value) {
    set(name, Double.toString(value));
  }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setFloat(final String name, final float value) {
    set(name, Float.toString(value));
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setFramerate(final int framerate) {
    setInt("FRAMERATE", framerate);
  }

  public void setFrequency(final int freq) {
    setInt("FREQ", freq);
  }

  public void setFullscreen(final boolean fullscreen) {
    setBoolean("FULLSCREEN", fullscreen);
  }

  public void setHeight(final int height) {
    setInt("HEIGHT", height);
  }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setInt(final String name, final int value) {
    set(name, Integer.toString(value));
  }

  public void setIsNew(final boolean isNew) { this.isNew = isNew; }

  /**
   * @see #set(String, String)
   * @throws RuntimeSetting
   *           for IO failure
   */
  public void setLong(final String name, final long value) {
    set(name, Long.toString(value));
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setMusic(final boolean music) {
    setBoolean("MUSIC", music);
  }

  /**
   * Not implemented. Properties can not store an arbitrary Object in human-readable format. Use
   * set(String, String) instead.
   * 
   * @see #set(String, String)
   * @throws InternalError
   *           in all cases
   */
  public void setObject(final String name, final Object value) {
    throw new InternalError(getClass().getName() + " Can't store arbitrary objects.  "
        + "If there is a toString() method for your Object, and it is " + "Properties-compatible, use "
        + getClass().getName() + ".set(String, String).");
  }

  public void setRenderer(final String renderer) {
    set("RENDERER", renderer);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setSamples(final int samples) {
    setInt("SAMPLES", samples);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setSFX(final boolean sfx) {
    setBoolean("SFX", sfx);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setStencilBits(final int stencilBits) {
    setInt("STENCIL_BITS", stencilBits);
  }

  /**
   * @throws InternalError
   *           in all cases
   */
  public void setVerticalSync(final boolean verticalSync) {
    setBoolean("VERTICAL_SYNC", verticalSync);
  }

  public void setWidth(final int width) {
    setInt("WIDTH", width);
  }

  /**
   * save() method which throws only a RuntimeExceptin.
   * 
   * @throws RuntimeSetting
   *           for IO failure
   * @see #save()
   */
  public void wrappedSave() {
    try {
      save();
    } catch (final IOException ioe) {
      logger.log(Level.WARNING, "Failed to persist properties", ioe);
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Sets default* static variables according to GameSettings.DEFAULT_* values and an optional
   * .properties file. Note that we are talking about <b>defaults</b> here, not user-specific
   * settings.
   * <P/>
   * This method should be called once the game name is known to the subclass. To override any default
   * with your subclass (as opposed to by using a .properties file), just set the static variable
   * before or after calling this method (before or after depends on the precedence you want among
   * programmatic and declarative DEFAULT_*, default* settings).
   * <P/>
   * Add new setting names by making your own method which does its own thing and calls
   * AbstractGameSettings.assignDefaults(propfilename).
   * <P/>
   * Property file paths are relative to CLASSPATH element roots.
   * 
   * @param propFileName
   *          Properties file read as CLASSPATH resource. If you give null, no properties file will be
   *          loaded.
   */
  protected static void assignDefaults(final String propFileName) {
    if (defaultsAssigned) {
      logger.fine("Skipping repeat invocation of assignDefaults()");
      return;
    }
    logger.fine("Initializing static default* setting variables");

    // hansen.playground.logging.LoggerInformation.getInfo();
    // System.exit(0);

    defaultsAssigned = true;
    if (defaultWidth == null) {
      defaultWidth = DEFAULT_WIDTH;
    }
    if (defaultHeight == null) {
      defaultHeight = DEFAULT_HEIGHT;
    }
    if (defaultDepth == null) {
      defaultDepth = DEFAULT_DEPTH;
    }
    if (defaultFrequency == null) {
      defaultFrequency = DEFAULT_FREQUENCY;
    }
    if (defaultFullscreen == null) {
      defaultFullscreen = DEFAULT_FULLSCREEN;
    }
    if (defaultRenderer == null) {
      defaultRenderer = DEFAULT_RENDERER;
    }
    if (defaultVerticalSync == null) {
      defaultVerticalSync = DEFAULT_VERTICAL_SYNC;
    }
    if (defaultDepthBits == null) {
      defaultDepthBits = DEFAULT_DEPTH_BITS;
    }
    if (defaultAlphaBits == null) {
      defaultAlphaBits = DEFAULT_ALPHA_BITS;
    }
    if (defaultStencilBits == null) {
      defaultStencilBits = DEFAULT_STENCIL_BITS;
    }
    if (defaultSamples == null) {
      defaultSamples = DEFAULT_SAMPLES;
    }
    if (defaultMusic == null) {
      defaultMusic = DEFAULT_MUSIC;
    }
    if (defaultSFX == null) {
      defaultSFX = DEFAULT_SFX;
    }
    if (defaultFramerate == null) {
      defaultFramerate = DEFAULT_FRAMERATE;
    }
    InputStream istream = null;
    if (propFileName != null) {
      istream = ResourceLocatorTool.getClassPathResourceAsStream(PropertiesGameSettings.class, propFileName);
    }
    if (istream == null) {
      logger.fine("No customization properties file found");
      return;
    }
    logger.fine("Customizing defaults according to '" + propFileName + "'");
    final Properties p = new Properties();
    try {
      p.load(istream);
    } catch (final IOException ioe) {
      logger.log(Level.WARNING,
          "Failed to load customizations from '" + propFileName + "'.  Continuing without customizations.", ioe);
      return;
    }
    Integer i;
    String s;
    Boolean b;
    i = loadInteger("DEFAULT_WIDTH", p);
    if (i != null) {
      defaultWidth = i.intValue();
    }
    i = loadInteger("DEFAULT_HEIGHT", p);
    if (i != null) {
      defaultHeight = i.intValue();
    }
    i = loadInteger("DEFAULT_DEPTH", p);
    if (i != null) {
      defaultDepth = i.intValue();
    }
    i = loadInteger("DEFAULT_FREQUENCY", p);
    if (i != null) {
      defaultFrequency = i.intValue();
    }
    b = loadBoolean("DEFAULT_FULLSCREEN", p);
    if (b != null) {
      defaultFullscreen = b.booleanValue();
    }
    s = p.getProperty("DEFAULT_RENDERER");
    if (s != null) {
      defaultRenderer = s;
    }
    b = loadBoolean("DEFAULT_VERTICAL_SYNC", p);
    if (b != null) {
      defaultVerticalSync = b.booleanValue();
    }
    i = loadInteger("DEFAULT_DEPTH_BITS", p);
    if (i != null) {
      defaultDepthBits = i.intValue();
    }
    i = loadInteger("DEFAULT_ALPHA_BITS", p);
    if (i != null) {
      defaultAlphaBits = i.intValue();
    }
    i = loadInteger("DEFAULT_STENCIL_BITS", p);
    if (i != null) {
      defaultStencilBits = i.intValue();
    }
    i = loadInteger("DEFAULT_SAMPLES", p);
    if (i != null) {
      defaultSamples = i.intValue();
    }
    b = loadBoolean("DEFAULT_MUSIC", p);
    if (b != null) {
      defaultMusic = b.booleanValue();
    }
    b = loadBoolean("DEFAULT_SFX", p);
    if (b != null) {
      defaultSFX = b.booleanValue();
    }
    i = loadInteger("DEFAULT_FRAMERATE", p);
    if (i != null) {
      defaultFramerate = i.intValue();
    }
    s = p.getProperty("SETTINGS_WIDGET_IMAGE");
    if (s != null) {
      defaultSettingsWidgetImage = s;
    }
  }

  public static Boolean loadBoolean(final String name, final Properties props) {
    final String s = props.getProperty(name);
    if (s == null) {
      return null;
    }
    return Boolean.valueOf(s);
  }

  public static Integer loadInteger(final String name, final Properties props) {
    final String s = props.getProperty(name);
    if (s == null) {
      return null;
    }
    try {
      return Integer.valueOf(s);
    } catch (final NumberFormatException nfe) {}
    logger.warning("Malformatted value in game properties file: " + s);
    return null;
  }

  /**
   * @param inName
   *          Must be non-null
   * @return normalized name. All lower-case with no shell meta-characters.
   */
  protected static String normalizeName(final String inName) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inName.length(); i++) {
      final char c = inName.charAt(i);
      sb.append((Character.isLetter(c) || Character.isDigit(c) || c == '-' || c == '_') ? c : '_');
    }
    return sb.toString().toUpperCase();
  }
}
