/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform float textureDensity;
uniform vec3 eyePosition;

varying vec2 vVertex; 			// our vertex position relative to our eye.

void main(void){	
	gl_TexCoord[0] = gl_MultiTexCoord0;

	// Sent to fragment program for texture clipmap generation.
	// This is the signed distance from our eye to the vertex/pixel, modified by our 
	// pixel density.  A higher density means the texture is packed more tightly 
	// around the viewer. We accomplish this by increasing the distance to the pixel.
	vVertex = (gl_Vertex.xz - eyePosition.xz) * textureDensity;

    gl_Position = ftransform();
}
