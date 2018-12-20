#version 330 core

#ifdef USE_FOG
@import include/fog.glsl
#endif

in vec2 vVertex;
in vec3 eyeSpacePosition;

out vec4 FragColor;

uniform sampler3D texture;
uniform sampler3D normalMap;

uniform int levels;
uniform int minLevel;
uniform int validLevels;
uniform int showDebug; 
uniform vec2 sliceOffset[16];

uniform vec3 lightDir;

#ifdef USE_FOG
uniform FogParams fogParams;
#endif

@import clipmap/terrain_frag_inc.glsl

void main()
{  
	float unit1, unit2;
	vec2 texCoord1, texCoord2;
	vec2 offset1, offset2;
	vec2 fadeCoord;

	computeUnit1(unit1, vVertex, validLevels, minLevel);		

	float textureSize = textureSize(texture, 0).x;
	float texelSize = 1 / textureSize;
	
	//-- setup clip lookup values
	clipTexSetup(unit1, unit2, texCoord1, texCoord2, offset1, offset2, fadeCoord, texelSize);
  
	//-- lookup clip colors
    vec4 texCol  = clipTexColor(texture,   unit1, unit2, texCoord1, texCoord2, offset1, offset2, fadeCoord, textureSize, texelSize, showDebug);
    vec4 normCol = clipTexColor(normalMap, unit1, unit2, texCoord1, texCoord2, offset1, offset2, fadeCoord, textureSize, texelSize, 0) * vec4(2.0) - vec4(1.0);

    vec3 nLightDir = normalize(-lightDir.xyz);
    vec3 n = normalize(normCol.xyz);
    float NdotL = max(dot(n,nLightDir),0.0);
    
    vec4 color = vec4(0.1) + vec4(NdotL, NdotL, NdotL, 1.0);
    color = color * texCol;

#ifdef USE_FOG
    float dist = length(eyeSpacePosition);
    float fogAmount = calcFogAmount(fogParams, abs(dist));
    FragColor = mix(color, fogParams.color, fogAmount);
#else
    FragColor = color;
#endif

}
