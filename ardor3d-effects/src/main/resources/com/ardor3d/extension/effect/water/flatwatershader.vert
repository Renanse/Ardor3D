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
varying vec4 viewCoords;
varying vec3 viewTangetSpace;

//uniform vec3 cameraPos;
uniform vec3 tangent;
uniform vec3 binormal;
uniform float normalTranslation, refractionTranslation;

void main()
{
	// Because we have a flat plane for water we already know the vectors for tangent space
//	vec3 normal = gl_Normal;
	vec3 normal = gl_NormalMatrix * gl_Normal;
	normal = normalize(normal);
	vec3 tangent2 = gl_NormalMatrix * tangent;
	tangent2 = normalize(tangent2);
	vec3 binormal2 = gl_NormalMatrix * binormal;
	binormal2 = normalize(binormal2);

	// Calculate the vector coming from the vertex to the camera
//	vec3 viewDir = cameraPos - gl_Vertex.xyz;
	vec4 v = gl_ModelViewMatrix * gl_Vertex;
	vec3 viewDir = -(v.xyz/v.w);
	viewDir = normalize(viewDir);

	// Compute tangent space for the view direction
	viewTangetSpace.x = dot(viewDir, tangent2);
	viewTangetSpace.y = dot(viewDir, binormal2);
	viewTangetSpace.z = dot(viewDir, normal);

	refrCoords = (gl_TextureMatrix[2] * gl_MultiTexCoord0).xy + vec2(0.0,refractionTranslation);
	normCoords = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy + vec2(0.0,normalTranslation);

	// This calculates our current projection coordinates
	viewCoords = gl_ModelViewProjectionMatrix * gl_Vertex;
	gl_Position = viewCoords;
}
