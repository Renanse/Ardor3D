/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * Uses a vec4 for joints and indices, allowing for up to 4 bone influences
 * per vertex.
 */

attribute vec4 Weights;
attribute vec4 JointIDs;

uniform mat4 JointPalette[50];

varying vec3 N;

void main(void) {
    mat4 mat = mat4(0.0);
    
    mat += JointPalette[int(JointIDs[0])] * Weights[0];
    mat += JointPalette[int(JointIDs[1])] * Weights[1];
    mat += JointPalette[int(JointIDs[2])] * Weights[2];
    mat += JointPalette[int(JointIDs[3])] * Weights[3];
    
    gl_Position = gl_ModelViewProjectionMatrix * (mat * gl_Vertex);
    
    N = gl_NormalMatrix * (mat3(mat[0].xyz,mat[1].xyz,mat[2].xyz) * gl_Normal);
    
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
