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

#endif