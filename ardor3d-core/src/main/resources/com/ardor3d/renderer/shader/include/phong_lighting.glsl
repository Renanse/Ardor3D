#ifndef PHONG_LIGHTING_INC
#define PHONG_LIGHTING_INC

@import include/light.glsl

vec3 calcDirectionalLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    vec3 lightDir = normalize(-light.direction);
    
    // diffuse shading
    float diff = max(dot(worldNormal, lightDir), 0.0);
    
    // specular shading
    vec3 reflectDir = reflect(-lightDir, worldNormal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), surface.shininess);

    // combine results
    vec3 ambient  = light.ambient  * surface.ambient;
    vec3 diffuse  = light.diffuse  * diff * surface.diffuse;
    vec3 specular = light.specular * spec * surface.specular;
    return (ambient + diffuse + specular);
}

vec3 calcPointLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    vec3 lightDir = normalize(light.position - worldPos);
    
    // diffuse shading
    float diff = max(dot(worldNormal, lightDir), 0.0);
    
    // specular shading
    vec3 reflectDir = reflect(-lightDir, worldNormal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), surface.shininess);

    // attenuation
    float distance    = length(light.position - worldPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + 
  			     light.quadratic * (distance * distance));    

    // combine results
    vec3 ambient  = light.ambient  * surface.ambient;
    vec3 diffuse  = light.diffuse  * diff * surface.diffuse;
    vec3 specular = light.specular * spec * surface.specular;
    ambient  *= attenuation;
    diffuse  *= attenuation;
    specular *= attenuation;
    return (ambient + diffuse + specular);
}

vec3 calcPointLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec2 texCoord, const vec3 viewDir, const TextureSurface surface)
{
    vec3 lightDir = normalize(light.position - worldPos);
    
    // diffuse shading
    float diff = max(dot(worldNormal, lightDir), 0.0);
    
    // specular shading
    vec3 reflectDir = reflect(-lightDir, worldNormal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), surface.shininess);

    // attenuation
    float distance    = length(light.position - worldPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + 
  			     light.quadratic * (distance * distance));    

    // combine results
    vec3 ambient  = light.ambient  * vec3(texture(surface.diffuse, texCoord));
    vec3 diffuse  = light.diffuse  * diff * vec3(texture(surface.diffuse, texCoord));
    vec3 specular = light.specular * spec * vec3(texture(surface.specular, texCoord));
    ambient  *= attenuation;
    diffuse  *= attenuation;
    specular *= attenuation;
    return (ambient + diffuse + specular);
}

vec3 calcSpotLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    vec3 lightDir = normalize(light.position - worldPos);
    
    // diffuse shading
    float diff = max(dot(worldNormal, lightDir), 0.0);
    
    // specular shading
    vec3 reflectDir = reflect(-lightDir, worldNormal);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), surface.shininess);

    // attenuation
    float distance    = length(light.position - worldPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + 
  			     light.quadratic * (distance * distance));    

    // spotlight intensity
    float theta = dot(lightDir, normalize(-light.direction)); 
    float epsilon = cos(light.innerAngle) - cos(light.angle);
    float intensity = clamp((theta - cos(light.angle)) / epsilon, 0.0, 1.0);

    // combine results
    vec3 ambient  = light.ambient  * surface.ambient;
    vec3 diffuse  = light.diffuse  * diff * surface.diffuse;
    vec3 specular = light.specular * spec * surface.specular;
    ambient  *= intensity;
    diffuse  *= intensity;
    specular *= intensity;
    return (ambient + diffuse + specular);
}

vec3 calcLighting(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
	switch (light.type)
	{
		case 0: // Directional
			return calcDirectionalLight(light, worldPos, worldNormal, viewDir, surface);
		case 1: // Point
			return calcPointLight(light, worldPos, worldNormal, viewDir, surface);
		case 2: // Spot
			return calcSpotLight(light, worldPos, worldNormal, viewDir, surface);
	}
	
	return vec3(1);
}

#endif