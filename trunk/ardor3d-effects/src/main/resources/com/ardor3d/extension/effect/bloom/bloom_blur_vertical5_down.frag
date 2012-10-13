/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

uniform float sampleDist;
uniform sampler2D RT;
varying vec2 texCoord;

void main(void)
{
   vec4 sum = vec4(0.0);

   sum += texture2D(RT, vec2(texCoord.x, texCoord.y)) * 6.0/16.0;
   sum += texture2D(RT, vec2(texCoord.x, texCoord.y - 1.0*sampleDist)) * 4.0/16.0;
   sum += texture2D(RT, vec2(texCoord.x, texCoord.y - 2.0*sampleDist)) * 3.0/16.0;
   sum += texture2D(RT, vec2(texCoord.x, texCoord.y - 3.0*sampleDist)) * 2.0/16.0;
   sum += texture2D(RT, vec2(texCoord.x, texCoord.y - 4.0*sampleDist)) * 1.0/16.0;

   gl_FragColor = sum;
}