package com.ardor3d.extension.terrain.client;

import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UrlInputSupplier implements InputSupplier<InputStream> {
	private final URL url;

	public UrlInputSupplier(URL url) {
		this.url = url;
	}

	@Override
	public InputStream getInput() throws IOException {
		return url.openStream();
	}
}
