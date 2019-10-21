/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ardor3d.image.Image;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.resource.ResourceSource;
import com.ardor3d.util.resource.URLResourceSource;

public enum HttpImageCache {
    Instance;

    Comparator<ImageCacheItem> DateCompare = (a, b) -> b.lastAccessed.compareTo(a.lastAccessed);
    ConcurrentMap<String, ImageCacheItem> MemoryCache = new ConcurrentHashMap<String, ImageCacheItem>();

    File cacheDir = new File(System.getProperty("user.dir") + "/cache/image/");

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(final File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void loadFromCacheOrDownloadAsync(final URI uri, final String type, final boolean flipped,
            final Consumer<Image> onSuccess) {
        CompletableFuture.runAsync(() -> {
            final Image img = Instance.loadFromCacheOrDownload(uri, type, flipped);
            onSuccess.accept(img);
        });
    }

    public Image loadFromCacheOrDownload(final URI uri, final String type, final boolean flipped) {
        // Convert our name to a key to be used in our in memory hash and file system
        final String key = convertUrlToFileName(uri.toString(), type, flipped);

        // Check the memory cache
        ImageCacheItem cacheItem = MemoryCache.get(key);

        if (cacheItem == null) {
            // Check the file cache
            try {
                final File f = new File(cacheDir, key);
                if (f.exists()) {
                    // Load bytes from file
                    final ResourceSource src = new URLResourceSource(f.toURI().toURL(), ".ABI");
                    final Image img = ImageLoaderUtil.loadImage(src, flipped);

                    // Make sure the image is reasonably sized before we store it
                    if (img.getWidth() > 8) {
                        // Create a cache item to store in memory
                        cacheItem = new ImageCacheItem(key, ".ABI", flipped, img,
                                Instant.ofEpochMilli(f.lastModified()));

                        // Add to our cache
                        MemoryCache.put(key, cacheItem);

                        cleanupMemoryCache();
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
                cacheItem = null;
            }
        }

        // if we don't have a cached copy, we'll have to ask for one.
        if (cacheItem == null) {
            return tryGetImage(new ImageCacheItem(key, type, flipped), uri);
        }

        // if we have not asked about this item for a bit, fire off a check to see if there's an update. But send the
        // cached copy immediately.
        if (cacheItem.lastChecked.until(Instant.now(), ChronoUnit.MINUTES) > 15) {
//            System.out.println("firing check...");
            final ImageCacheItem item = cacheItem;
            CompletableFuture.runAsync(() -> tryGetImage(item, uri));
        } else {
            cacheItem.lastAccessed = Instant.now();
        }

        // return any existing cached copy.
//        System.out.println("returning cache item (1).");
        return cacheItem.value;
    }

    private Image tryGetImage(final ImageCacheItem cacheItem, final URI uri) {
        cacheItem.lastAccessed = cacheItem.lastChecked = Instant.now();

        if (cacheItem.value == null) {
            // No existing value, so simply load the image and add to memory cache
            try {
                final URLResourceSource source = new URLResourceSource(uri.toURL(), cacheItem.type);
                cacheItem.value = ImageLoaderUtil.loadImage(source, cacheItem.flipped);
                if (source.getLastModifiedValue() != 0L) {
                    cacheItem.lastModified = Instant.ofEpochMilli(source.getLastModifiedValue());
                }
                MemoryCache.put(cacheItem.id, cacheItem);
            } catch (final MalformedURLException ex) {
                ex.printStackTrace();
                return null;
            }
        } else {
            // we have a value, so this is a check instead. Let's open a connection and see if it has been modified
            try {
                final URLConnection connection = uri.toURL().openConnection();
                connection.setIfModifiedSince(cacheItem.lastModified.toEpochMilli());
                connection.connect();
                if (connection instanceof HttpURLConnection) {
                    final HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    final int responseCode = httpConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                        return cacheItem.value;
                    }
                }
            } catch (final IOException ex) {
                ex.printStackTrace();
                return cacheItem.value;
            }
        }

        cleanupMemoryCache();

        // Refresh our file cache
        updateFileCache(cacheItem);

        return cacheItem.value;
    }

    private void updateFileCache(final ImageCacheItem cacheItem) {
        try {
            if (!cacheDir.exists()) {
                cacheDir.mkdir();
            }

            // Now cache to file system
            final File temp = new File(cacheDir, cacheItem.id + ".part");
            final BinaryExporter exp = new BinaryExporter();
            exp.save(cacheItem.value, temp);

            // Delete old file if it exists
            final File finalItem = new File(cacheDir, cacheItem.id);
            if (finalItem.exists()) {
                finalItem.delete();
            }

            // And now that it is written, rename it
            temp.renameTo(finalItem);

            // Set the last write time on the file - do this last as it seems to fail on Android.
            if (cacheItem.lastModified != null) {
                finalItem.setLastModified(cacheItem.lastModified.toEpochMilli());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void clearCaches() {
        // clear our memory cache
        MemoryCache.clear();

        // clear the file cache as well.
        if (cacheDir.exists()) {
            deleteDirectory(cacheDir);
        }
    }

    public static boolean deleteDirectory(final File dir) {
        if (dir.isDirectory()) {
            final File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                final boolean success = deleteDirectory(children[i]);
                if (!success) {
                    return false;
                }
            }
        }

        // either file or an empty directory
        System.out.println("removing file or directory : " + dir.getName());
        return dir.delete();
    }

    void cleanupMemoryCache() {
        final List<ImageCacheItem> sortedList = new ArrayList<>(MemoryCache.values());
        sortedList.sort(DateCompare);

        int i = 0;
        final int maxI = sortedList.size();
        int bytes = 0;
        final int maxBytes = 5 * 1024 * 1024;

        ImageCacheItem item;
        for (; i < maxI && bytes <= maxBytes; i++) {
            item = sortedList.get(i);
            if (item.getMemorySize() + bytes > maxBytes) {
                // We want to break out at this point and remove anything pass this point
                break;
            }

            bytes += item.getMemorySize();
        }

        // Remove the excess
        if (i < maxI) {
            // Remove from cache
            for (int j = maxI - 1; j >= i; j--) {
                MemoryCache.remove(sortedList.get(j).id);
                sortedList.remove(j);
            }
        }
    }

    List<ImageCacheItem> createSortedList() {
        final List<ImageCacheItem> returnList = new ArrayList<>();

        for (final ImageCacheItem value : MemoryCache.values()) {
            returnList.add(value);
        }

        returnList.sort(DateCompare);

        return returnList;
    }

    static final Pattern NameRegex = Pattern.compile("[a-z0-9]+", Pattern.CASE_INSENSITIVE);

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String convertUrlToFileName(final String urlText, final String type, final boolean flipped) {
        try {
            // convert our url to a hash - this gives us a reasonably unique, known length string
            final String key = urlText + "_" + type + "_" + flipped;
            final MessageDigest md = MessageDigest.getInstance("md5");
            md.update(key.getBytes());
            final String part1 = bytesToHex(md.digest());
            md.reset();

            // the hash is good, but out of concern for the unlikely collision, I'm adding the hash of the url's reverse
            // as well.
            final StringBuilder sb = new StringBuilder(key);
            md.update(sb.reverse().toString().getBytes());
            final String part2 = bytesToHex(md.digest());
            return part1 + part2;
        } catch (final NoSuchAlgorithmException ex) {
            String rt = "";
            final Matcher matcher = NameRegex.matcher(urlText);
            while (matcher.find()) {
                rt += matcher.group() + "_";
            }

            // ugly default...
            return rt + "_" + type + "_" + flipped;
        }
    }

    private class ImageCacheItem {
        String id;
        String type;
        boolean flipped;

        Image value;

        Instant lastModified;
        Instant lastAccessed = Instant.now();
        Instant lastChecked = Instant.ofEpochMilli(0L);

        int memorySize = -1;

        public ImageCacheItem(final String id, final String type, final boolean flipped) {
            this.id = id;
            this.type = type;
            this.flipped = flipped;
        }

        public ImageCacheItem(final String id, final String type, final boolean flipped, final Image value,
                final Instant lastModified) {
            this(id, type, flipped);
            this.value = value;
            this.lastModified = lastModified;
        }

        int getMemorySize() {
            if (memorySize < 0 && value != null) {
                memorySize = 0;
                for (final ByteBuffer buff : value.getData()) {
                    memorySize += buff.limit();
                }
            }

            return memorySize;
        }
    }
}
