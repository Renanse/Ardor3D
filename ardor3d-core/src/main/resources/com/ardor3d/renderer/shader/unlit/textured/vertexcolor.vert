#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec4 color;
layout (location = 2) in vec2 uv0;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec4 DiffuseColor;
out vec2 TexCoord0;

void main()
{
    gl_Position = projection * view * model * vec4(vertex, 1.0);
    DiffuseColor = color;
    TexCoord0 = uv0;
}
