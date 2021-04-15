#version 330 core

@import include/alpha_test.glsl

@import include/surface.glsl
@import include/phong_lighting.glsl

#ifdef USE_FOG
@import include/fog.glsl
#endif

#ifndef NR_LIGHTS
#define NR_LIGHTS 4
#endif

out vec4 FragColor;

in vec3 WorldPos;
in vec4 ViewPos;
in vec3 Normal;

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

uniform LightProperties lightProps;
uniform vec3 cameraLoc;
uniform ColorSurface surface;

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

    vec3 V = normalize(cameraLoc - WorldPos);
    LightingResult lit = calcLighting(lightProps, WorldPos, Normal, V, surface);
    
    vec3 emissive = surface.emissive;
    vec3 ambient = surface.ambient * lightProps.globalAmbient;
    vec3 diffuse = surface.diffuse * lit.diffuse;
    vec3 specular = surface.specular * lit.specular;
    
    color = clamp(color * vec4(emissive + ambient + diffuse + specular, surface.opacity), 0.0, 1.0);

    if (!applyAlphaTest(color)) discard;

#ifdef USE_FOG
    float fogAmount = calcFogAmount(abs(ViewPos.z/ViewPos.w));
    FragColor = mix(color, fogParams.color, fogAmount);
#else
    FragColor = color;
#endif
}
