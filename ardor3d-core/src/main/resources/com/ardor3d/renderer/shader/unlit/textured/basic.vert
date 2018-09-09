#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec2 uv0;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec2 TexCoord0;

void main()
{
    gl_Position = projection * view * model * vec4(vertex, 1.0);
    TexCoord0 = uv0;
}
