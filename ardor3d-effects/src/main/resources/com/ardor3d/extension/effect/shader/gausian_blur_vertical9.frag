/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

uniform sampler2D inputTex;
uniform float sampleDist;

varying vec2 texCoord;

void main(void)
{
   vec4 sum = vec4(0.0);

   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y - 1.0  * sampleDist)) * 0.04;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y - 0.75 * sampleDist)) * 0.08;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y - 0.5  * sampleDist)) * 0.12;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y - 0.25 * sampleDist)) * 0.16;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y                    )) * 0.20;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y + 0.25 * sampleDist)) * 0.16;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y + 0.5  * sampleDist)) * 0.12;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y + 0.75 * sampleDist)) * 0.08;
   sum += texture2D(inputTex, vec2(texCoord.x, texCoord.y + 1.0  * sampleDist)) * 0.04;

   gl_FragColor = sum;
}