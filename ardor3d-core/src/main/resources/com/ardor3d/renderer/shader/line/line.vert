#version 330 core

in vec3 vertex;

#ifdef TEXTURED
in float texV;
#endif

uniform mat4 modelViewProj;

#ifndef VERT_COLORS
uniform vec4 defaultColor;
#else
in vec4 color;
#endif

out VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif
	
	float texV;
} VertexOut;

void main()
{    
#ifndef VERT_COLORS
    VertexOut.color = defaultColor;
#else
    VertexOut.color = color;
#endif

#ifdef TEXTURED
	VertexOut.texV = texV;
#else
	VertexOut.texV = 0.0;
#endif

    gl_Position = modelViewProj * vec4(vertex, 1.0);
}
