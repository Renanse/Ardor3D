#ifndef PHONG_LIGHTING_INC
#define PHONG_LIGHTING_INC

@import include/light.glsl

// default us to use blinn-phong over phong.
#ifndef USE_BLINN_PHONG
#define USE_BLINN_PHONG true
#endif

vec3 calcDiffuse(Light light, const vec3 lightDir, const vec3 worldNormal)
{
    float NdotL = max(dot(worldNormal, lightDir), 0.0);
    return light.color * light.intensity * NdotL; 
}

vec3 calcSpecular(Light light, const ColorSurface surface, const vec3 viewDir, const vec3 lightDir, const vec3 worldNormal)
{
#if USE_BLINN_PHONG
	    // blinn phong
        vec3 halfwayDir = normalize(lightDir + viewDir);
        float NdotH = max(0.0, dot(worldNormal, halfwayDir));
        
        return light.color * light.intensity * pow(NdotH, surface.shininess);
#else
	    // phong
        vec3 reflectDir = normalize(reflect(-lightDir, worldNormal));
        float RdotV = max(0.0, dot(reflectDir, viewDir));
        
        return light.color * light.intensity * pow(RdotV, surface.shininess);
#endif
}

float calcAttenuation(Light light, float distance)
{
    return 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
}

LightingResult calcDirectionalLight(Light light, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    vec3 lightDir = normalize(-light.direction);

    LightingResult result;    
    result.diffuse = calcDiffuse(light, lightDir, worldNormal);
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal);
    return result;
}

LightingResult calcPointLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult result;
    result.diffuse = vec3(0.0);
    result.specular = vec3(0.0);
    
    vec3 lightDir = (light.position - worldPos);
    float distance = length(lightDir);
    if (distance > light.range) return result;
    
    lightDir /= distance;
    
    float attenuation = calcAttenuation(light, distance);

    LightingResult result;
    result.diffuse = calcDiffuse(light, lightDir, worldNormal) * attenuation;
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal) * attenuation;
    return result;
}

LightingResult calcSpotLight(Light light, const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult result;
    result.diffuse = vec3(0.0);
    result.specular = vec3(0.0);
    vec3 lightDir = (light.position - worldPos);
    float distance = length(lightDir);
    if (distance > light.range) return result;
    
    lightDir /= distance;
    
    float attenuation = calcAttenuation(light, distance);

    // spotlight intensity
    float theta = dot(lightDir, normalize(-light.direction)); 
    float epsilon = cos(light.innerAngle) - cos(light.angle);
    float intensity = clamp((theta - cos(light.angle)) / epsilon, 0.0, 1.0);
    
    result.diffuse = calcDiffuse(light, lightDir, worldNormal) * attenuation * intensity;
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal) * attenuation * intensity;
    return result;
}

LightingResult calcLighting(LightProperties lightProps,  const vec3 worldPos, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult totalResult;
    for (int i = 0; i < MAX_LIGHTS; i++)
    {
        Light light = lightProps.lights[i];
        if (!light.enabled) continue;
        
        LightingResult result;
        switch (light.type)
        {
            case LIGHT_DIRECTIONAL:
                result = calcDirectionalLight(light, worldNormal, viewDir, surface);
                break;
            case LIGHT_POINT:
                result = calcPointLight(light, worldPos, worldNormal, viewDir, surface);
                break;
            case LIGHT_SPOT:
                result = calcSpotLight(light, worldPos, worldNormal, viewDir, surface);
                break;
        }
        totalResult.diffuse += result.diffuse;
        totalResult.specular += result.specular;
    }
    
    // XXX: HDR?
    totalResult.diffuse = clamp(totalResult.diffuse, 0.0, 1.0);
    totalResult.specular = clamp(totalResult.specular, 0.0, 1.0);
    
    return totalResult;
}

#endif