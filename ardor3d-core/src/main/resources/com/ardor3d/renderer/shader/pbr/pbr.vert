#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv0;

out vec3 WorldPos;
out vec3 Normal;
out vec2 TexCoords;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    Normal = mat3(model) * normal;   
    TexCoords = uv0;

    gl_Position =  projection * view * vec4(WorldPos, 1.0);
}