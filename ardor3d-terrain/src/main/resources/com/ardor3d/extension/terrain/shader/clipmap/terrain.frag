#version 400 core

@import include/phong_lighting.glsl
@import include/alpha_test.glsl

#ifdef USE_FOG
@import include/fog.glsl
#endif

in vec2 vVertex;
in vec3 WorldPos;
in vec4 ViewPos;

out vec4 FragColor;

uniform sampler3D diffuseMap;
uniform vec4 tint;

uniform int levels;
uniform int minLevel;
uniform int validLevels;
uniform int showDebug; 
uniform vec2 sliceOffset[16];

uniform vec3 cameraLoc;
uniform ColorSurface surface;
uniform int grayscaleDiffuse;

#ifdef USE_NORMAL_MAP
uniform sampler3D normalMap;
#endif
uniform mat3 normalMat;

@import clipmap/terrain_frag_inc.glsl

void main()
{  
	float unit;
	vec3 texCoord1, texCoord2;
	vec2 fadeCoord;

	float textureSize = textureSize(diffuseMap, 0).x;
	float texelSize = 1 / textureSize;

	// determine which unit we are looking at
	computeUnit(unit, vVertex, minLevel, validLevels, textureSize);
	
	// determine our clip coordinate values
	calculateClipUVs(unit, textureSize, texelSize, texCoord1, texCoord2, fadeCoord);
  
	// lookup clip colors
    vec4 texCol = clipTexColor(diffuseMap, texCoord1, texCoord2, fadeCoord, textureSize, texelSize, showDebug);
    if (grayscaleDiffuse != 0) texCol = texCol.rrra;
	vec4 color = tint * texCol;

#ifdef USE_NORMAL_MAP
    vec4 normCol = clipTexColor(normalMap, texCoord1, texCoord2, fadeCoord, textureSize, texelSize, 0) * vec4(2.0) - vec4(1.0);
    vec3 Normal = normalize(normalMat * normCol.xyz);
#else
    vec3 Normal = normalize(normalMat * vec3(0,1,0));
#endif

	vec3 viewDir = normalize(cameraLoc - WorldPos);
    LightingResult lit = calcLighting(WorldPos, Normal, ViewPos.xyz/ViewPos.w, viewDir, surface);
    
    vec3 emissive = surface.emissive;
    vec3 ambient = surface.ambient * lightProps.globalAmbient;
    vec3 diffuse = surface.diffuse * lit.diffuse;
    vec3 specular = surface.specular * lit.specular;
    
    color = clamp(color * vec4(emissive + ambient + diffuse + specular, surface.opacity), 0.0, 1.0);

    if (!applyAlphaTest(color)) discard;

#ifdef USE_FOG
    // Calculate any fog contribution using vertex distance in eye space.
    float dist = length(ViewPos.xyz/ViewPos.w);
    float fogAmount = calcFogAmount(abs(dist));
    color = mix(color, fogParams.color, fogAmount);
#endif

    FragColor = color;
}
