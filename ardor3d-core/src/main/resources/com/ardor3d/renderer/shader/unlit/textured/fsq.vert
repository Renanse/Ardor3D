#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec2 uv0;

out vec2 TexCoords;
out vec4 DiffuseColor;

void main()
{
    TexCoords = uv0;
    DiffuseColor = vec4(1);
    
    gl_Position = vec4(vertex, 1.0);
}
