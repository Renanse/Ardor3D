#version 330 core

out vec4 FragColor;

in VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif
	
	vec2 uv0;
} VertexIn;

#ifdef TEXTURED
	uniform sampler2D tex0;
#endif

void main()
{
	vec4 color = VertexIn.color;

#ifdef TEXTURED
    color = color * texture(tex0, VertexIn.uv0);
#endif

    FragColor = color;
}
