/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform sampler3D texture;
uniform float levels;
uniform vec2 sliceOffset[16];
//uniform float maxLevel;
uniform float validLevels;
uniform float textureSize; 
uniform float texelSize; 
uniform float showDebug;

varying vec2 vVertex;

 
vec4 texture3DBilinear( const in sampler3D textureSampler, const in vec3 uv, const in vec2 offset )
{
    vec4 tl = texture3D(textureSampler, uv);
    vec4 tr = texture3D(textureSampler, uv + vec3(texelSize, 0, 0));
    vec4 bl = texture3D(textureSampler, uv + vec3(0, texelSize, 0));
    vec4 br = texture3D(textureSampler, uv + vec3(texelSize , texelSize, 0));

    vec2 f = fract( uv.xy * textureSize ); // get the decimal part
    vec4 tA = mix( tl, tr, f.x ); // will interpolate the red dot in the image
    vec4 tB = mix( bl, br, f.x ); // will interpolate the blue dot in the image
    return mix( tA, tB, f.y ); // will interpolate the green dot in the image
}


void main()
{  
	float unit = (max(abs(vVertex.x), abs(vVertex.y)));
	
	unit = floor(unit);
	unit = log2(unit);
	unit = floor(unit);
	
//	unit = max(unit, maxLevel);
	if (unit > validLevels) {
		discard;
	}
//	unit = min(unit, validLevels);

	unit = max(unit, 0.0);
	
	vec2 offset = sliceOffset[int(unit)];	
	float frac = unit;
	frac = exp2(frac);	
	frac *= 4.0; //Magic number	
	vec2 texCoord = vVertex/vec2(frac);
	vec2 fadeCoord = texCoord;
	texCoord += vec2(0.5);
	texCoord *= vec2(1.0 - texelSize);
	texCoord += offset;

	float unit2 = unit + 1.0;
	unit2 = min(unit2, validLevels);
	vec2 offset2 = sliceOffset[int(unit2)];	
	float frac2 = unit2;
	frac2 = exp2(frac2);	
	frac2 *= 4.0; //Magic number	
	vec2 texCoord2 = vVertex/vec2(frac2);
	texCoord2 += vec2(0.5);
	texCoord2 *= vec2(1.0 - texelSize);
	texCoord2 += offset2;
	  	
	unit /= levels;	
	unit = clamp(unit, 0.0, 0.99);

	unit2 /= levels;	
	unit2 = clamp(unit2, 0.0, 0.99);

	//vec4 tex = texture3D(texture, vec3(texCoord.x, texCoord.y, unit));
	vec4 tex = texture3DBilinear(texture, vec3(texCoord.x, texCoord.y, unit), offset);
	vec4 tex2 = texture3DBilinear(texture, vec3(texCoord2.x, texCoord2.y, unit2), offset2);

	float fadeVal1 = abs(fadeCoord.x)*2.05;
	float fadeVal2 = abs(fadeCoord.y)*2.05;
	float fadeVal = max(fadeVal1, fadeVal2);
	fadeVal = max(0.0, fadeVal-0.8)*5.0;
	fadeVal = min(1.0, fadeVal);
    gl_FragColor = mix(tex, tex2, fadeVal)+vec4(fadeVal*showDebug);
}
