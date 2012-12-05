/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

uniform sampler2D inputTex;
uniform sampler2D colorRampTex;

uniform float redWeight;
uniform float greenWeight;
uniform float blueWeight;

varying vec2 texCoord;

void main(void) {
	vec4 color = texture2D(inputTex, texCoord);
	vec3 convert = vec3(redWeight, greenWeight, blueWeight);
	
	float luminance = dot(convert, color.rgb);

	vec4 finalColor = texture2D( colorRampTex, vec2(luminance, .5) );
	finalColor.a = color.a; 
	gl_FragColor = finalColor;
}