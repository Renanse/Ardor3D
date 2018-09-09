#version 330 core

in vec3 vertex;
in vec2 uv0;
#ifdef VERTEX_COLORS
in vec4 color;
#endif

out vec3 WorldPos;
out vec2 TexCoords;
#ifdef VERTEX_COLORS
#ifdef FLAT_COLORS
flat out vec4 VertColor;
#else
out vec4 VertColor;
#endif
#endif

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    TexCoords = uv0;
#ifdef VERTEX_COLORS
    VertColor = color;
#endif

    gl_Position =  projection * view * vec4(WorldPos, 1.0);
}