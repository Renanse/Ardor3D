#version 330 core

in vec3 vertex;
in vec3 normal;
in vec4 color;
in vec2 uv0;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMat;

out vec3 WorldPos;
out vec4 ViewPos;
out vec3 Normal;
out vec4 DiffuseColor;
out vec2 TexCoords;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    ViewPos = view * vec4(WorldPos, 1.0);
    Normal = normalMat * normal;
    DiffuseColor = color;
    TexCoords = uv0;

    gl_Position = projection * ViewPos;
}
