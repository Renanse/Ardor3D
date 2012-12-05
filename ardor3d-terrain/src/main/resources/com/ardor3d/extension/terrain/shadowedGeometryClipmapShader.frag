/**
 * Copyright (c) 2008-2011 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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

vec4 texture3DBilinear( const in sampler3D textureSampler, const in vec3 uv, const in vec2 offset )
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
	float unit = (max(abs(vVertex.x), abs(vVertex.y)));
	
	unit = floor(unit);
	unit = log2(unit);
	unit = floor(unit);
	
	unit = min(unit, validLevels);
    unit = max(unit, minLevel);
	
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

	vec4 tex = texture3DBilinear(texture, vec3(texCoord.x, texCoord.y, unit), offset);
	vec4 tex2 = texture3DBilinear(texture, vec3(texCoord2.x, texCoord2.y, unit2), offset2);

	float fadeVal1 = abs(fadeCoord.x)*2.05;
	float fadeVal2 = abs(fadeCoord.y)*2.05;
	float fadeVal = max(fadeVal1, fadeVal2);
	fadeVal = max(0.0, fadeVal-0.8)*5.0;
	fadeVal = min(1.0, fadeVal);
    vec4 texCol = mix(tex, tex2, fadeVal) + vec4(fadeVal*showDebug);

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
