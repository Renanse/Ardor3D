/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * This fragment shader is purely for demonstration.  It colors the faces based on their normal vectors.
 */

uniform sampler2D texture;

varying vec3 transformedLightDirection;
varying vec3 N;

void main(void) {
	// simplest lighting possible to get similar effect as non-gpu version
	float lighting = max(dot(normalize(N),normalize(transformedLightDirection)), 0.0) * 0.65 + 0.1;
	
    gl_FragColor = texture2D(texture, gl_TexCoord[0].st) * vec4(lighting);
}
