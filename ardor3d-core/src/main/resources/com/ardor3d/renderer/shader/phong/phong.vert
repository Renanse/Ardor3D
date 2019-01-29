#version 330 core

in vec3 vertex;
in vec3 normal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMat;

#ifndef VERT_COLORS
uniform vec4 defaultColor;
#else
in vec4 color;
#endif

out vec3 WorldPos;
out vec4 ViewPos;
out vec3 Normal;

#ifdef FLAT_COLORS
flat out vec4 DiffuseColor;
#else
out vec4 DiffuseColor;
#endif

#ifdef UV_COUNT
	#if UV_COUNT > 0
	in vec2 uv0;
	out vec2 TexCoords0;
	#endif
	#if UV_COUNT > 1
	in vec2 uv1;
	out vec2 TexCoords1;
	#endif
	#if UV_COUNT > 2
	in vec2 uv2;
	out vec2 TexCoords2;
	#endif
	#if UV_COUNT > 3
	in vec2 uv3;
	out vec2 TexCoords3;
	#endif
#endif

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    ViewPos = view * vec4(WorldPos, 1.0);
    Normal = normalize(normalMat * normal);

#ifndef VERT_COLORS
    DiffuseColor = defaultColor;
#else
    DiffuseColor = color;
#endif

#ifdef UV_COUNT 
	#if UV_COUNT > 0
	TexCoords0 = uv0;
	#endif
	#if UV_COUNT > 1
	TexCoords1 = uv1;
	#endif
	#if UV_COUNT > 2
	TexCoords2 = uv2;
	#endif
	#if UV_COUNT > 3
	TexCoords3 = uv3;
	#endif
#endif

    gl_Position = projection * ViewPos;
}
