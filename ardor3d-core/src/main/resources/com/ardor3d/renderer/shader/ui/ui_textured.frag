#version 330 core

out vec4 FragColor;

in vec2 TexCoords;
#ifdef VERTEX_COLORS
#ifdef FLAT_COLORS
flat in vec4 VertColor;
#else
in vec4 VertColor;
#endif
#else
uniform vec4 defaultColor;
#endif

uniform sampler2D subTex;

void main()
{
#ifdef VERTEX_COLORS	
	vec4 color = VertColor;
#else
	vec4 color = defaultColor;
#endif

    FragColor = color * texture(subTex, TexCoords);
}