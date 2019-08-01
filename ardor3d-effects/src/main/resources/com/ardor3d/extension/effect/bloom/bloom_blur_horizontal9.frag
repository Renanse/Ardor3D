/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform float sampleDist;
uniform sampler2D RT;
varying vec2 texCoord;

void main(void)
{
   vec4 sum = vec4(0.0);

   sum += texture2D(RT, vec2(texCoord.x - 1.0*sampleDist, texCoord.y)) * 1.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x - 0.75*sampleDist, texCoord.y)) * 2.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x - 0.5*sampleDist, texCoord.y)) * 3.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x - 0.25*sampleDist, texCoord.y)) * 4.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x, texCoord.y)) * 5.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x + 0.25*sampleDist, texCoord.y)) * 4.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x + 0.5*sampleDist, texCoord.y)) * 3.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x + 0.75*sampleDist, texCoord.y)) * 2.0/25.0;
   sum += texture2D(RT, vec2(texCoord.x + 1.0*sampleDist, texCoord.y)) * 1.0/25.0;

   gl_FragColor = sum;
}