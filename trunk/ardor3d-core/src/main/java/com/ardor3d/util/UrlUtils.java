/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {

    /**
     * Create a new URL by resolving a given relative string (such as "mydir/myfile.ext") against a given url (such as
     * "http://www.company.com/mycontent/content.html"). This method is necessary because new URL(URL, String) does not
     * handle primaryUrls that contain %2F instead of a slash.
     * 
     * @param primaryUrl
     *            the primary or base URL.
     * @param relativeLoc
     *            a String representing a relative file or path to resolve against the primary URL.
     * @return the resolved URL.
     * @throws MalformedURLException
     *             if we are unable to create the URL.
     */
    public static URL resolveRelativeURL(final URL primaryUrl, final String relativeLoc) throws MalformedURLException {
        // Because URL(base, string) does not handle correctly URLs that have %2F, we have to manually replace these.
        // So, we grab the URL as a string
        String url = primaryUrl.toString();

        // Replace any %2F (or %2f) with forward slashes
        url = url.replaceAll("\\%2[F,f]", "/");

        // And make our new URL
        return new URL(new URL(url), relativeLoc);
    }

}
