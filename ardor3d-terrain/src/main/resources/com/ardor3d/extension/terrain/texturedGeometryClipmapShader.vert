/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform float textureDensity;
uniform vec3 eyePosition;
uniform float vertexDistance;      // Clip-Gridspacing
uniform float clipSideSize;     // Clip-Size

varying vec2 vVertex; 			// our vertex position relative to our eye.
varying vec3 eyeSpacePosition;

varying vec4 diffuse, ambient;

void main(void){	
	gl_TexCoord[0] = gl_MultiTexCoord0;

	// Sent to fragment program for texture clipmap generation.
	// This is the signed distance from our eye to the vertex/pixel, modified by our 
	// pixel density.  A higher density means the texture is packed more tightly 
	// around the viewer. We accomplish this by increasing the distance to the pixel.
	vVertex = (gl_Vertex.xz - eyePosition.xz) * textureDensity;

    
    vec4 position = gl_Vertex;

//////////// terrain height blending
    float scaledClipSideSize = clipSideSize * vertexDistance * 0.5;
    vec2 viewDistance = abs(position.xz - eyePosition.xz);
    float maxDistance = max(viewDistance.x, viewDistance.y)/scaledClipSideSize;
    float blend = clamp((maxDistance - 0.51) * 2.2, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
//////////////
    
    gl_Position = gl_ModelViewProjectionMatrix * position;

	// Used for fog calculations
	vec4 ePos = gl_ModelViewMatrix * position;	
    eyeSpacePosition = ePos.xyz;
    
    gl_TexCoord[0] = gl_TextureMatrix[1] * ePos;
    gl_TexCoord[1] = gl_TextureMatrix[2] * ePos;
    gl_TexCoord[2] = gl_TextureMatrix[3] * ePos;
    gl_TexCoord[3] = gl_TextureMatrix[4] * ePos;
    
    // LIGHTING and MATERIALS
	diffuse = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse;
	ambient = gl_FrontMaterial.ambient * gl_LightSource[0].ambient;
	ambient += gl_LightModel.ambient * gl_FrontMaterial.ambient;
}
