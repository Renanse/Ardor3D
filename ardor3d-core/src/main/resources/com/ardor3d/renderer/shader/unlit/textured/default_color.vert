#version 330 core

in vec3 vertex;
in vec2 uv0;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform vec4 defaultColor;

out vec3 WorldPos;
out vec4 DiffuseColor;
out vec2 TexCoords;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    DiffuseColor = defaultColor;   
    TexCoords = uv0;

    gl_Position = projection * view * vec4(WorldPos, 1.0);
}
