#version 330 core

out vec4 FragColor;

#ifdef VERTEX_COLORS
#ifdef FLAT_COLORS
flat in vec4 VertColor;
#else
in vec4 VertColor;
#endif
#else
uniform vec4 defaultColor;
#endif

void main()
{
#ifdef VERTEX_COLORS	
	FragColor = VertColor;
#else
	FragColor = defaultColor;
#endif
}