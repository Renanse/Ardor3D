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

void main()
{   
	float shade = 0.0;
	vec3 col = vec3(0.0);
	if (zDist <= sampleDist.x) {
		shade = shadow2DProj(shadowMap0, gl_TexCoord[0]).x;
		col.r = 0.5;
	} 
	else if (zDist <= sampleDist.y)  {
    	shade = shadow2DProj(shadowMap1, gl_TexCoord[1]).x;
    	col.g = 0.5;
    } 
    else if (zDist <= sampleDist.z)  {
    	shade = shadow2DProj(shadowMap2, gl_TexCoord[2]).x;
    	col.b = 0.5;
    } 
    else if (zDist <= sampleDist.w)  {
    	shade = shadow2DProj(shadowMap3, gl_TexCoord[3]).x;
    	col.r = 0.5;
    	col.b = 0.5;
    }
    
    gl_FragColor = vec4(col.rgb, 0.5 * shade);
}
