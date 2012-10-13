/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

varying vec2 refrCoords;
varying vec2 normCoords;
varying vec2 foamCoords;
varying vec4 viewCoords;
varying vec3 viewTangetSpace;
varying vec2 vnormal;
varying vec4 vVertex;

uniform vec3 cameraPos;
uniform vec3 tangent;
uniform vec3 binormal;
uniform float normalTranslation, refractionTranslation;
uniform float waterHeight;
uniform float heightFalloffStart;
uniform float heightFalloffSpeed;

void main()
{
	viewCoords = gl_ModelViewProjectionMatrix * gl_Vertex;
	vVertex = gl_Vertex;
	float heightAdjust = 1.0 - clamp((viewCoords.z-heightFalloffStart)/heightFalloffSpeed,0.0,1.0);
	vVertex.y = mix(waterHeight,vVertex.y,heightAdjust);
	viewCoords = gl_ModelViewProjectionMatrix * vVertex;
	gl_Position = viewCoords;
	vVertex.w = waterHeight;

	// Because we have a flat plane for water we already know the vectors for tangent space
	vec3 normal = vec3(gl_Normal.x*heightAdjust,gl_Normal.y,gl_Normal.z*heightAdjust);
	vnormal = normal.xz * 0.15;

	// Calculate the vector coming from the vertex to the camera
	vec3 viewDir = cameraPos - gl_Vertex.xyz;

	// Compute tangent space for the view direction
	viewTangetSpace.x = dot(viewDir, tangent);
	viewTangetSpace.y = dot(viewDir, binormal);
	viewTangetSpace.z = dot(viewDir, normal);

	//todo test 0.8
	refrCoords = (gl_TextureMatrix[2] * gl_MultiTexCoord0).xy + vec2(0.0,refractionTranslation);
	normCoords = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy + vec2(0.0,normalTranslation);
	foamCoords = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy + vec2(0.0,normalTranslation*0.4);
}
