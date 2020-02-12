/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */
 
uniform sampler2DShadow shadowMap0;
uniform sampler2DShadow shadowMap1;
uniform sampler2DShadow shadowMap2;
uniform sampler2DShadow shadowMap3;

varying float zDist; 
uniform vec4 sampleDist;
uniform vec4 shadowColor;
//uniform float _shadowSize;

float offset_lookup(const in sampler2DShadow map,
                     const in vec4 loc,
                     const in vec2 offset,
                     const in float shadowSize)
{
	return shadow2DProj(map, vec4(loc.xy + offset * shadowSize * loc.w, loc.z, loc.w)).x;
}

float shadowLookup(const in sampler2DShadow shadowmap, const in vec4 sCoord, const in float shadowSize) {
	vec2 offset = mod(sCoord.xy, 0.5);
	offset.y += offset.x;  // y ^= x in floating point

	if (offset.y > 1.1) {
		offset.y = 0.0;
	}
	offset = vec2(0.0);

	return (offset_lookup(shadowmap, sCoord, offset +
                             vec2(-1.5, 0.5), shadowSize) +
               offset_lookup(shadowmap, sCoord, offset +
                             vec2(0.5, 0.5), shadowSize) +
               offset_lookup(shadowmap, sCoord, offset +
                             vec2(-1.5, -1.5), shadowSize) +
               offset_lookup(shadowmap, sCoord, offset +
                             vec2(0.5, -1.5), shadowSize) ) * 0.25;
}

float shadowLookup33(const in sampler2DShadow shadowmap, const in vec4 sCoord, const in float shadowSize) {
	float x,y;
	float shadow = 0.0;
	for (y = -1.5; y <= 1.5; y += 1.0) {
		for (x = -1.5; x <= 1.5; x += 1.0) {
			shadow += offset_lookup(shadowmap, sCoord, vec2(x,y), shadowSize);
		}
	}
	
	shadow /= 16.0;
	
	return shadow;
}

void main()
{  
	float shade = 0.0;
	if (zDist < sampleDist.x) {
		shade = shadowLookup33(shadowMap0, gl_TexCoord[0], 1.0/1024.0);
	} else if (zDist < sampleDist.y)  {
		shade = shadowLookup33(shadowMap1, gl_TexCoord[1], 1.0/1024.0);
    } else if (zDist < sampleDist.z)  {
		shade = shadowLookup(shadowMap2, gl_TexCoord[2], 1.0/1024.0);
    } else if (zDist < sampleDist.w)  {
    	shade = shadow2DProj(shadowMap3, gl_TexCoord[3]).x;
    }

    gl_FragColor = vec4(shadowColor.rgb, shadowColor.a * shade);
}
