/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform sampler2D inputTex;

varying vec2 texCoord;

void main(void) {
	vec4 color = texture2D(inputTex, texCoord);
	vec3 convert = vec3(0.3086, 0.6094, 0.0820);
	
	float luminance = dot(convert, color.rgb);

	gl_FragColor = vec4(luminance, luminance, luminance, 1.0);
}