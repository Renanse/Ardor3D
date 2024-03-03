/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.UrlUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class URLResourceSource implements ResourceSource {
  private static final Logger logger = Logger.getLogger(URLResourceSource.class.getName());

  private URL _url;
  private String _type;

  // used to make equals more efficient
  private transient String _urlToString = null;

  private long _lastModifiedValue;

  /**
   * Construct a new URLResourceSource. Must set URL separately.
   */
  public URLResourceSource() {}

  /**
   * Construct a new URLResourceSource from a specific URL.
   *
   * @param sourceUrl
   *          The url to load the resource from. Must not be null. If the URL has a valid URL filename
   *          (see {@link URL#getFile()}) and an extension (eg. http://url/myFile.png) then the
   *          extension (.png in this case) is used as the type.
   */
  public URLResourceSource(final URL sourceUrl) {
    assert (sourceUrl != null) : "sourceUrl must not be null";
    setURL(sourceUrl);

    // add type, if present
    final String fileName = _url.getFile();
    if (fileName != null) {
      final int dot = fileName.lastIndexOf('.');
      if (dot >= 0) {
        _type = fileName.substring(dot);
      } else {
        _type = ResourceSource.UNKNOWN_TYPE;
      }
    }
  }

  /**
   * Construct a new URLResourceSource from a specific URL and type.
   *
   * @param sourceUrl
   *          The url to load the resource from. Must not be null.
   * @param type
   *          our type. Usually a file extension such as .png. Required for generic loading when
   *          multiple resource handlers could be used.
   */
  public URLResourceSource(final URL sourceUrl, final String type) {
    assert (sourceUrl != null) : "sourceUrl must not be null";
    setURL(sourceUrl);

    _type = type;
  }

  @Override
  public ResourceSource getRelativeSource(final String name) {
    try {
      final URL srcURL = UrlUtils.resolveRelativeURL(_url, "./" + name);
      if (srcURL != null) {
        // check if the URL can be opened
        // just force it to try to grab info
        srcURL.openStream().close();
        // Ok satisfied... return
        return new URLResourceSource(srcURL);

      }
    } catch (final MalformedURLException ex) {} catch (final IOException ex) {}
    if (URLResourceSource.logger.isLoggable(Level.FINEST)) {
      URLResourceSource.logger.logp(Level.FINEST, getClass().getName(), "getRelativeSource(String)",
          "Unable to find relative file {0} from {1}.", new Object[] {name, _url});
    }
    return null;
  }

  public void setURL(final URL url) {
    _url = url;
    _urlToString = url != null ? url.toString() : null;
  }

  public URL getURL() { return _url; }

  @Override
  public String getName() { return _urlToString; }

  @Override
  public String getType() { return _type; }

  public void setType(final String type) { _type = type; }

  /**
   * @return the last modified date on the underlying URL connection - will be 0 until openStream has
   *         been called.
   */
  public long getLastModifiedValue() { return _lastModifiedValue; }

  @Override
  public InputStream openStream() throws IOException {
    final URLConnection connection = _url.openConnection();
    UrlUtils.injectAuthenticator(connection);
    connection.connect();
    _lastModifiedValue = connection.getLastModified();
    return connection.getInputStream();
  }

  /**
   * @return the string representation of this URLResourceSource.
   */
  @Override
  public String toString() {
    return "URLResourceSource [url=" + _urlToString + ", type=" + _type + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_type == null) ? 0 : _type.hashCode());
    result = prime * result + ((_urlToString == null) ? 0 : _urlToString.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof URLResourceSource)) {
      return false;
    }
    final URLResourceSource other = (URLResourceSource) obj;
    if (_type == null) {
      if (other._type != null) {
        return false;
      }
    } else if (!_type.equals(other._type)) {
      return false;
    }
    if (_url == null) {
      if (other._url != null) {
        return false;
      }
    } else if (!_urlToString.equals(other._urlToString)) {
      return false;
    }
    return true;
  }

  @Override
  public Class<?> getClassTag() { return URLResourceSource.class; }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    final String protocol = capsule.readString("protocol", null);
    final String host = capsule.readString("host", null);
    final String file = capsule.readString("file", null);
    if (file != null) {
      // see if we would like to divert this to a new location.
      final ResourceSource src =
          ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, URLDecoder.decode(file, "UTF-8"));
      if (src instanceof URLResourceSource) {
        setURL(((URLResourceSource) src)._url);
        _type = ((URLResourceSource) src)._type;
        return;
      }
    }

    if (protocol != null && host != null && file != null) {
      setURL(new URL(protocol, host, file));
    }

    _type = capsule.readString("type", null);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_url.getProtocol(), "protocol", null);
    capsule.write(_url.getHost(), "host", null);
    capsule.write(_url.getFile(), "file", null);

    capsule.write(_type, "type", null);
  }
}
