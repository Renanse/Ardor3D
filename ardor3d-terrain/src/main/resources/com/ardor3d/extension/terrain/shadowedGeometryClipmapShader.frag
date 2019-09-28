/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */
 
uniform sampler3D texture;
uniform sampler2DShadow shadowMap0;
uniform sampler2DShadow shadowMap1;
uniform sampler2DShadow shadowMap2;
uniform sampler2DShadow shadowMap3;

uniform float levels;
uniform vec2 sliceOffset[16];
uniform float minLevel;
uniform float validLevels;
uniform float textureSize; 
uniform float texelSize; 
uniform float showDebug; 

uniform vec4 sampleDist;
uniform vec4 shadowColor;

varying vec2 vVertex;
varying vec3 eyeSpacePosition;

varying vec4 diffuse,ambient;

vec4 texture3DBilinear( const in sampler3D textureSampler, const in vec3 uv )
{
    vec4 tl = texture3D(textureSampler, uv);
    vec4 tr = texture3D(textureSampler, uv + vec3(texelSize, 0, 0));
    vec4 bl = texture3D(textureSampler, uv + vec3(0, texelSize, 0));
    vec4 br = texture3D(textureSampler, uv + vec3(texelSize , texelSize, 0));

    vec2 f = fract( uv.xy * textureSize ); // +texelSize on ATI?
    
    vec4 tA = mix( tl, tr, f.x );
    vec4 tB = mix( bl, br, f.x );
    
    return mix( tA, tB, f.y );
}

void main()
{  
	// get our texture size as a power of 2.
	float log2texSize = log2(textureSize);
	
	// determine the closest power of two lower than the max X or Y distance to the vertex from the eye. 
	float maxDistance = floor(max(abs(vVertex.x), abs(vVertex.y)));
	float unit = floor(log2(maxDistance));
	
	// Determine our clipmap unit by subtracting our texture's size as pow 2.
	//
	// We remove 2 from our size before this: 
	//  -  One for the fact that we center our clipmap, so we are dealing with half distance
	//  -  Another because we want to be in a given clipmap up UNTIL hitting the given size.
	//
	// For example, if our texture is size 128, our 0th clipmap will cover (-64, 64).  128 is
	// 2^7.  For a vertex that is 32 units away, we'd have unit = log2(32) = 5, then we'd 
	// subtract (7 - 2) from that, giving us a final unit of 0.  At distance 64, the resulting
	// unit is 1.  [log(64) - (7 - 2) = 6 - 5 = 1.]
	unit -= (log2texSize - 2);
	
	// Now make sure that our unit falls within in an appropriate range.
	unit = clamp(unit, minLevel, validLevels);
	
	// We now calculate 2 texture coordinates - this is to allow us to blend between levels
	
	// Determine our first texcoord - divide our distance vector by our unit texture size,
	// This will give ous a range of [-.5, .5].  We add .5 to shift us to [0, 1]
	vec2 texCoord = vVertex/vec2(exp2(unit+log2texSize));
	vec2 fadeCoord = texCoord; // save our [-.5, .5] tex coord for later use
	texCoord += vec2(0.5);
	texCoord *= vec2(1.0 - texelSize);
	texCoord += sliceOffset[int(unit)];

	// figure the next farthest out unit for blending
	float unit2 = unit + 1.0;
	unit2 = min(unit2, validLevels);

	// Determine our second texcoord - divide our distance vector by our unit texture size,
	// This will give us a range of [-.5, .5].  We add .5 to shift us to [0, 1]
	vec2 texCoord2 = vVertex/vec2(exp2(unit2+log2texSize));
	texCoord2 += vec2(0.5);
	texCoord2 *= vec2(1.0 - texelSize);
	texCoord2 += sliceOffset[int(unit2)];
	  	
	// Determine our depth texture coords
	float u1 = clamp(unit / levels, 0.0, 0.99);
	float u2 = clamp(unit2 / levels, 0.0, 0.99);

	// Texture coordinates are now ready.  Next, sample our textures
	vec4 tex = texture3DBilinear(texture, vec3(texCoord.x, texCoord.y, u1));
	vec4 tex2 = texture3DBilinear(texture, vec3(texCoord2.x, texCoord2.y, u2));

	// Now, determine our crossfade between sampled textures using our original [-.5, 5] uv
	float fadeVal = max(abs(fadeCoord.x), abs(fadeCoord.y))*2.05;
	
	// Fade between textures in the last 20% of our texture.
	fadeVal = max(0.0, fadeVal-0.8)*5.0;
	fadeVal = min(1.0, fadeVal);
	
	// Mix the textures using our fade value.  
	// Add an optional white color if debug is enabled.
    vec4 texCol = mix(tex, tex2, fadeVal) + vec4(fadeVal*showDebug);

	// Calculate any fog contribution using vertex distance in eye space.
    float dist = length(eyeSpacePosition);
	float fog = clamp((gl_Fog.end - dist) * gl_Fog.scale, 0.0, 1.0);
	
	float shade = 0.0;
	float zDist = -eyeSpacePosition.z;
	if (zDist < sampleDist.x) {
		shade = shadow2DProj(shadowMap0, gl_TexCoord[0]).x;
	} else if (zDist < sampleDist.y)  {
    	shade = shadow2DProj(shadowMap1, gl_TexCoord[1]).x;
    } else if (zDist < sampleDist.z)  {
    	shade = shadow2DProj(shadowMap2, gl_TexCoord[2]).x;
    } else if (zDist < sampleDist.w)  {
    	shade = shadow2DProj(shadowMap3, gl_TexCoord[3]).x;
    }
    
    gl_FragColor = mix(gl_Fog.color, texCol * (ambient + diffuse) * vec4(1.0-shade*shadowColor.a), fog);  
}
