#version 330 core

@import include/alpha_test.glsl

#ifdef USE_FOG
@import include/fog.glsl
#endif

out vec4 FragColor;

in vec3 WorldPos;
in vec4 ViewPos;

#ifdef FLAT_COLORS
flat in vec4 DiffuseColor;
#else
in vec4 DiffuseColor;
#endif

#ifdef UV_COUNT 
	#if UV_COUNT > 0
	in vec2 TexCoords0;
	uniform sampler2D tex0;
	#endif
	#if UV_COUNT > 1
	in vec2 TexCoords1;
	uniform sampler2D tex1;
	#endif
	#if UV_COUNT > 2
	in vec2 TexCoords2;
	uniform sampler2D tex2;
	#endif
	#if UV_COUNT > 3
	in vec2 TexCoords3;
	uniform sampler2D tex3;
	#endif
#endif

void main()
{
	vec4 color = DiffuseColor;

#ifdef UV_COUNT
	#if UV_COUNT > 0
	    color = color * texture(tex0, TexCoords0);
	#endif
	#if UV_COUNT > 1
	    color = color * texture(tex1, TexCoords1);
	#endif
	#if UV_COUNT > 2
	    color = color * texture(tex2, TexCoords2);
	#endif
	#if UV_COUNT > 3
	    color = color * texture(tex3, TexCoords3);
	#endif
#endif

    if (!applyAlphaTest(color)) discard;

#ifdef USE_FOG
    float fogAmount = calcFogAmount(abs(ViewPos.z/ViewPos.w));
    FragColor = mix(color, fogParams.color, fogAmount);
#else
    FragColor = color;
#endif
}
