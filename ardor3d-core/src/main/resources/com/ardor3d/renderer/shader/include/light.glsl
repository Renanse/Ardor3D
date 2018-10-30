#ifndef LIGHT_INC
#define LIGHT_INC

struct Light {
	int type;
	
	// Point and Spot
    vec3 position;
    
    // Directional and Spot
    vec3 direction;
    
    // attenuation - Point and Spot
    float constant;
    float linear;
    float quadratic;  
    
    // All types
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

	// Spot only
    float angle;
    float innerAngle;
}; 

#endif