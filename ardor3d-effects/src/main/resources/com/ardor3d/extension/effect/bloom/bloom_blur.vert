/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

varying vec2 texCoord;

void main(void)
{
   vec2 Pos = sign(gl_Vertex.xy);
   gl_Position = vec4(Pos.xy, 0, 1);
   texCoord.x = 0.5 * (1.0 + Pos.x);
   texCoord.y = 0.5 * (1.0 + Pos.y);
}