#version 330 core

@import include/surface.glsl
@import include/phong_lighting.glsl

#ifndef NR_LIGHTS
#define NR_LIGHTS 4
#endif

out vec4 FragColor;

in vec4 DiffuseColor;
in vec3 WorldPos;
in vec3 Normal;

uniform Light light[NR_LIGHTS];
uniform vec3 cameraLoc;
uniform ColorSurface surface;

void main()
{
    vec3 V = normalize(cameraLoc - WorldPos);
    vec3 lighting = vec3(0);
    for(int i = 0; i < NR_LIGHTS; i++)
    	lighting += calcLighting(light[i], WorldPos, Normal, V, surface);
    	
    FragColor = DiffuseColor * vec4(lighting, 1.0);
}
