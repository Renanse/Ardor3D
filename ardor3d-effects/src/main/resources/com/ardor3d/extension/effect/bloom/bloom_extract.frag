/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform float exposurePow;
uniform float exposureCutoff;
uniform sampler2D RT;

varying vec2 vTexCoord;

void main(void)
{
   vec4 sum = texture2D(RT, vTexCoord);
   if ( (sum.r+sum.g+sum.b)/3.0 < exposureCutoff ) {
      sum = vec4(0.0);
   }
   sum = pow(sum,vec4(exposurePow));
   gl_FragColor = sum;
}