#version 330 core

in vec3 vertex;
in vec3 normal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform vec4 defaultColor;

out vec3 WorldPos;
out vec3 Normal;
out vec4 DiffuseColor;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    Normal = mat3(model) * normal;
    DiffuseColor = defaultColor;   

    gl_Position =  projection * view * vec4(WorldPos, 1.0);
}
