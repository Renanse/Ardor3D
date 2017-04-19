/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
varying float zDist; 

void main(void){
	// force w coord to 1 so we don't have to do anything special to get clipmap shadows to work. 
	vec4 vertex = vec4(gl_Vertex.xyz, 1.0);
	
	vec4 ePos = gl_ModelViewMatrix * vertex;
	
    gl_TexCoord[0] = gl_TextureMatrix[0] * ePos;
    gl_TexCoord[1] = gl_TextureMatrix[1] * ePos;
    gl_TexCoord[2] = gl_TextureMatrix[2] * ePos;
    gl_TexCoord[3] = gl_TextureMatrix[3] * ePos;
    
    zDist = -ePos.z;
 
    gl_Position = gl_ModelViewProjectionMatrix * vertex; 
}
