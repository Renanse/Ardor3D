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
 * This fragment shader is purely for demonstration.  It colors the faces based on their normal vectors.
 */

varying vec3 N;

void main(void) {
    gl_FragColor =  vec4(abs(normalize(N)),1.0);
}
