#version 330 core

out vec4 FragColor;

// stipple - pattern is 16 bit
uniform float	stippleFactor;
uniform uint	stipplePattern;

in VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif
	
	vec2 uv0;
	noperspective float distance;
} VertexIn;

#ifdef TEXTURED
	uniform sampler2D tex0;
#endif

void main()
{
	uint bit = uint(round(VertexIn.distance / stippleFactor)) % 16U;
	if ((stipplePattern & (1U << bit)) == 0U) discard;

	vec4 color = VertexIn.color;

#ifdef TEXTURED
    color = color * texture(tex0, VertexIn.uv0);
#endif

    FragColor = color;
}
