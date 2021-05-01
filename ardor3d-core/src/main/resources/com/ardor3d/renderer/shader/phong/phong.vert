#version 330 core

in vec3 vertex;
in vec3 normal;
#ifdef INSTANCED
in mat4 instanceMatrix;
#endif

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
	uniform mat4 textureMatrix0;
	#endif
	#if UV_COUNT > 1
	in vec2 uv1;
	out vec2 TexCoords1;
	uniform mat4 textureMatrix1;
	#endif
	#if UV_COUNT > 2
	in vec2 uv2;
	out vec2 TexCoords2;
	uniform mat4 textureMatrix2;
	#endif
	#if UV_COUNT > 3
	in vec2 uv3;
	out vec2 TexCoords3;
	uniform mat4 textureMatrix3;
	#endif
#endif

void main()
{

    WorldPos = (model *
#ifdef INSTANCED
    	instanceMatrix *
#endif
	    vec4(vertex, 1.0)).xyz;
    ViewPos = view * vec4(WorldPos, 1.0);
#ifdef INSTANCED
    // expensive perhaps.  Could provide this as another instance attribute.
    Normal = normalize(mat3(transpose(inverse(model * instanceMatrix))) * normal);
#else
    Normal = normalize(normalMat * normal);
#endif

#ifndef VERT_COLORS
    DiffuseColor = defaultColor;
#else
    DiffuseColor = color;
#endif

#ifdef UV_COUNT 
	#if UV_COUNT > 0
    TexCoords0 = vec2(textureMatrix0 * vec4(uv0, 1.0, 1.0));
	#endif
	#if UV_COUNT > 1
    TexCoords1 = vec2(textureMatrix1 * vec4(uv1, 1.0, 1.0));
	#endif
	#if UV_COUNT > 2
    TexCoords2 = vec2(textureMatrix2 * vec4(uv2, 1.0, 1.0));
	#endif
	#if UV_COUNT > 3
    TexCoords3 = vec2(textureMatrix3 * vec4(uv3, 1.0, 1.0));
	#endif
#endif

    gl_Position = projection * ViewPos;
}
