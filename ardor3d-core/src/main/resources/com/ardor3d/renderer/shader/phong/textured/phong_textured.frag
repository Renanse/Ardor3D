#version 330 core

@import include/surface.glsl
@import include/phong_lighting.glsl

#ifdef USE_FOG
@import include/fog.glsl
#endif

#ifndef NR_LIGHTS
#define NR_LIGHTS 4
#endif

#ifndef USE_BLINN_PHONG
#define USE_BLINN_PHONG true
#endif

out vec4 FragColor;

in vec3 WorldPos;
in vec4 ViewPos;
in vec3 Normal;
in vec4 DiffuseColor;
in vec2 TexCoords;

uniform Light light[NR_LIGHTS];
uniform vec3 cameraLoc;
uniform ColorSurface surface;

uniform sampler2D tex0;

#ifdef USE_FOG
uniform FogParams fogParams;
#endif

void main()
{
    vec3 V = normalize(cameraLoc - WorldPos);
    vec3 lighting = vec3(0);
    for(int i = 0; i < NR_LIGHTS; i++)
    	lighting += calcLighting(light[i], WorldPos, Normal, V, surface, USE_BLINN_PHONG);

    vec4 color = clamp(DiffuseColor * texture(tex0, TexCoords) * vec4(surface.emissive + lighting, 1.0), 0.0, 1.0);

#ifdef USE_FOG
    float fogAmount = calcFogAmount(fogParams, abs(ViewPos.z/ViewPos.w));
    FragColor = mix(color, fogParams.color, fogAmount);
#else
    FragColor = color;
#endif

}
