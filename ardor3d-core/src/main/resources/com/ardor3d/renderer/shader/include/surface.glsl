#ifndef SURFACE_INC
#define SURFACE_INC

// com.ardor3d.surface.ColorSurface
struct ColorSurface {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

// com.ardor3d.surface.TextureSurface
struct TextureSurface {
    sampler2D diffuse;
    sampler2D specular;
    float shininess;
};

// com.ardor3d.surface.PbrSurface
struct PbrSurface {
    vec3 albedo;
    float metallic;
    float roughness;
    float ao;
};

// com.ardor3d.surface.PbrTexturedSurface
struct PbrTexturedSurface {
    sampler2D albedoMap;
    sampler2D normalMap;
    sampler2D metallicMap;
    sampler2D roughnessMap;
    sampler2D aoMap;
};

#endif