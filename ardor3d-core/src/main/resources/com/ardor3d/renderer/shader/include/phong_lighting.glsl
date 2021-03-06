#ifndef PHONG_LIGHTING_INC
#define PHONG_LIGHTING_INC

@import include/light.glsl
@import include/surface.glsl

// default us to use blinn-phong over phong.
#ifndef USE_BLINN_PHONG
#define USE_BLINN_PHONG true
#endif

#define POINT_SAMPLE_DIR_LENGTH 20
const vec3 pointSampleOffsetDirections[POINT_SAMPLE_DIR_LENGTH] = vec3[]
(
   vec3( 1,  1,  1), vec3( 1, -1,  1), vec3(-1, -1,  1), vec3(-1,  1,  1),
   vec3( 1,  1, -1), vec3( 1, -1, -1), vec3(-1, -1, -1), vec3(-1,  1, -1),
   vec3( 1,  1,  0), vec3( 1, -1,  0), vec3(-1, -1,  0), vec3(-1,  1,  0),
   vec3( 1,  0,  1), vec3(-1,  0,  1), vec3( 1,  0, -1), vec3(-1,  0, -1),
   vec3( 0,  1,  1), vec3( 0, -1,  1), vec3( 0, -1, -1), vec3( 0,  1, -1)
);

#define SHADOW_SAMPLE_DIR_LENGTH 8
const vec2 sampleOffsetDirections[SHADOW_SAMPLE_DIR_LENGTH] = vec2[]
(
   vec2( 1,  1), vec2( -1,  1),
   vec2( 1,  -1), vec2( -1,  -1),
   vec2( 1,  0), vec2( -1,  0),
   vec2( 0,  1), vec2( 0,  -1)
);

vec3 calcDiffuse(const Light light, const vec3 lightDir, const vec3 worldNormal)
{
    float NdotL = max(dot(worldNormal, lightDir), 0.0);
    return light.color * light.intensity * NdotL; 
}

vec3 calcSpecular(const Light light, const ColorSurface surface, const vec3 viewDir, const vec3 lightDir, const vec3 worldNormal)
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

float calcAttenuation(const Light light, const float distance)
{
    return 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
}

int calcSplit(const float absViewZ)
{
    for (int i=1; i<MAX_SPLITS - 1; i++) {
        if (absViewZ < lightProps.splitDistances[i]) {
            return i-1;
        }
    }
    return MAX_SPLITS - 1;
}

float calcDirShadowFactor(const sampler2DArrayShadow shadowTex, const int layer, 
const int mode, const vec3 projCoords, const float compare)
{
    // 3x3 sampling
    if (mode == 1) {
        float shadowFactor = 0.0;
        float inc = 1.0 / textureSize(shadowTex, 0).x;
        const float samples = SHADOW_SAMPLE_DIR_LENGTH;
        for (int i = 0; i < samples; i++) {
            shadowFactor += texture(shadowTex, vec4(projCoords.xy + sampleOffsetDirections[i] * inc, layer, compare)); 
        }
        return shadowFactor / samples;
    }
    
    // all other modes, including 0 - technically 2x2 sampling since it is a Shadow texture sample.
    else return texture(shadowTex, vec4(projCoords.xy, layer, compare));
}

float calcSpotShadowFactor(const sampler2DShadow shadowTex, const int mode, const vec3 projCoords, const float compare)
{
    // 3x3 sampling
    if (mode == 1) {
        float shadowFactor = 0.0;
        float inc = 1.0 / textureSize(shadowTex, 0).x;
        const float samples = SHADOW_SAMPLE_DIR_LENGTH;
        for (int i = 0; i < samples; i++) {
            shadowFactor += texture(shadowTex, vec3(projCoords.xy + sampleOffsetDirections[i] * inc, compare)); 
        }
        return shadowFactor / samples;
    }
    
    // all other modes, including 0 - technically 2x2 sampling since it is a Shadow texture sample.
    else return texture(shadowTex, vec3(projCoords.xy, compare));
}

float calcPointShadowFactor(const samplerCubeShadow shadowTex, const int mode, const vec3 fragToLight, const float compare)
{
    // 3x3x3 sampling
    if (mode == 1) {
        float shadowFactor = 0.0;
        const float inc = 0.05;
        const float samples = POINT_SAMPLE_DIR_LENGTH;
        for (int i = 0; i < samples; i++) {
            shadowFactor += texture(shadowTex, vec4(fragToLight.xyz + pointSampleOffsetDirections[i] * inc, compare)); 
        }
        return shadowFactor / samples;
    }
    
    // all other modes, including 0 - technically 2x2 sampling since it is a Shadow texture sample.
    else return texture(shadowTex, vec4(fragToLight, compare));
}

LightingResult calcDirectionalLight(const Light light, const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    vec3 lightDir = normalize(-light.direction);

    LightingResult result;
    result.diffuse = calcDiffuse(light, lightDir, worldNormal);
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal);
    return result;
}

LightingResult calcDirectionalShadowLight(const Light light, const vec3 worldPos, const vec3 worldNormal, 
const vec3 viewPos, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult result = calcDirectionalLight(light, worldNormal, viewDir, surface);
    if (!light.castsShadows) return result;
    
    float shadowFactor = 1.0;
    int split = calcSplit(abs(viewPos.z));
	vec4 lightSpacePos = light.shadowMatrix[split] * vec4(worldPos, 1.0);
	vec3 projCoords = (lightSpacePos.xyz / lightSpacePos.w) * 0.5 + 0.5;
	shadowFactor = calcDirShadowFactor(lightProps.dirShadowMap, split, light.filterMode, projCoords, projCoords.z - light.bias);
	shadowFactor = clamp(shadowFactor, 0.0, 1.0);

    result.diffuse *= shadowFactor;
    result.specular *= shadowFactor;
    return result;
}

float vectToDepth(const vec3 vec, const mat4 proj)
{
    vec3 absVec = abs(vec);
    float maxAxis = max(max(absVec.x, absVec.y), absVec.z);
    float rVal = -proj[2][2] + proj[3][2] / maxAxis;
    return (rVal + 1.0) * 0.5;
}

LightingResult calcPointLight(const Light light, const samplerCubeShadow shadowTexture, const vec3 worldPos, 
const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult result;
    result.diffuse = vec3(0.0);
    result.specular = vec3(0.0);
    
    vec3 lightDir = (light.position - worldPos);
    float distance = length(lightDir);
    if (distance > light.range) return result;
    lightDir /= distance;
    
    float shadowFactor = 1.0;
    if (light.castsShadows) {
        vec3 fragToLight = worldPos - light.position;
        float eval = vectToDepth(fragToLight, light.shadowMatrix[0]) - light.bias;
        shadowFactor = calcPointShadowFactor(shadowTexture, light.filterMode, fragToLight, eval);
        if (shadowFactor <= 0.0) return result;
    }
    
    float attenuation = calcAttenuation(light, distance);

    result.diffuse = calcDiffuse(light, lightDir, worldNormal) * attenuation * shadowFactor;
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal) * attenuation * shadowFactor;
    return result;
}

LightingResult calcSpotLight(const Light light, const sampler2DShadow shadowTexture, const vec3 worldPos, 
const vec3 worldNormal, const vec3 viewDir, const ColorSurface surface)
{
    LightingResult result;
    result.diffuse = vec3(0.0);
    result.specular = vec3(0.0);
    
    float shadowFactor = 1.0;
    if (light.castsShadows) {
    	vec4 lightSpacePos = light.shadowMatrix[0] * vec4(worldPos, 1.0);
    	vec3 projCoords = (lightSpacePos.xyz / lightSpacePos.w) * 0.5 + 0.5;
    	shadowFactor = calcSpotShadowFactor(shadowTexture, light.filterMode, projCoords, projCoords.z - light.bias);
        if (shadowFactor <= 0.0) return result;
    }
    vec3 lightDir = (light.position - worldPos);
    float distance = length(lightDir);
    if (distance > light.range) return result;
    
    lightDir /= distance;
    
    float attenuation = calcAttenuation(light, distance);

    // spotlight intensity
    float theta = dot(lightDir, normalize(-light.direction)); 
    float epsilon = cos(light.innerAngle) - cos(light.angle);
    float intensity = clamp((theta - cos(light.angle)) / epsilon, 0.0, 1.0);
    
    result.diffuse = calcDiffuse(light, lightDir, worldNormal) * attenuation * intensity * shadowFactor;
    result.specular = calcSpecular(light, surface, viewDir, lightDir, worldNormal) * attenuation * intensity * shadowFactor;
    return result;
}

LightingResult calcLighting(const vec3 worldPos, const vec3 worldNormal, 
const vec3 viewPos, const vec3 viewDir, const ColorSurface surface)
{
    Light light;
    LightingResult totalResult, result;
    totalResult.diffuse = vec3(0.0);
    totalResult.specular = vec3(0.0);
    for (int i = 0; i < MAX_LIGHTS; i++)
    {
        light = lightProps.lights[i];
        if (!light.enabled) continue;
        
        switch (light.type)
        {
            case LIGHT_DIRECTIONAL:
                result = calcDirectionalLight(light, worldNormal, viewDir, surface);
                break;
            case LIGHT_POINT:
                result = calcPointLight(light, lightProps.pointShadowMaps[i], worldPos, worldNormal, viewDir, surface);
                break;
            case LIGHT_SPOT:
                result = calcSpotLight(light, lightProps.spotShadowMaps[i], worldPos, worldNormal, viewDir, surface);
                break;
        }
        totalResult.diffuse += result.diffuse;
        totalResult.specular += result.specular;
    }
    
    // include our directional light with shadows
    if (lightProps.dirShadowLight.enabled) {
        light = lightProps.dirShadowLight;
        result = calcDirectionalShadowLight(light, worldPos, worldNormal, viewPos, viewDir, surface);
        totalResult.diffuse += result.diffuse;
        totalResult.specular += result.specular;
    }
    
    // XXX: HDR?
    totalResult.diffuse = clamp(totalResult.diffuse, 0.0, 1.0);
    totalResult.specular = clamp(totalResult.specular, 0.0, 1.0);
    
    return totalResult;
}

#endif