#version 330 core

layout (location = 0) in vec3 vertex;

out vec3 WorldPos;

uniform mat4 projection;
uniform mat4 view;

void main()
{
	WorldPos = vertex;
	gl_Position =  projection * view * vec4(WorldPos, 1.0);
}