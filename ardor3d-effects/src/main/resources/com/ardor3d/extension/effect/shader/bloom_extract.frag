/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform sampler2D inputTex;
uniform float exposureIntensity;
uniform float exposureCutoff;

varying vec2 texCoord;

void main(void) {
	vec4 color = texture2D(inputTex, texCoord);
	float lum = dot(vec3(0.3086, 0.6094, 0.0820), color.rgb);

	if (lum < exposureCutoff ) {
		color = vec4(0.0, 0.0, 0.0, color.a);
	}
    
	gl_FragColor = vec4(color.rgb * exposureIntensity, color.a);
}