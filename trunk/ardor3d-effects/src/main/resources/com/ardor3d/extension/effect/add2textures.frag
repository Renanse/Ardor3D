/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

uniform sampler2D tex1;
uniform sampler2D tex2;

varying vec2 texCoord;

void main(void) {
	vec4 color1 = texture2D(tex1, texCoord);
	vec4 color2 = texture2D(tex2, texCoord);
    
	gl_FragColor = vec4(color1.rgb + color2.rgb, min(color1.a + color2.a, 1.0));
}