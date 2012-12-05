/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */
 
uniform float scale; 
uniform vec3 eyePosition;

varying vec2 vVertex;

void main(void){	
	gl_TexCoord[0] = gl_MultiTexCoord0;

	vVertex = (gl_Vertex.xz - eyePosition.xz) * vec2(scale);

    gl_Position = ftransform();
}
