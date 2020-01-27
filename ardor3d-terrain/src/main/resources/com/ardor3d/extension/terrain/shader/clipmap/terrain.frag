#version 330 core

#ifdef USE_FOG
@import include/fog.glsl
#endif

in vec2 vVertex;
in vec3 eyeSpacePosition;

out vec4 FragColor;

uniform sampler3D diffuseMap;
uniform vec4 tint;

uniform int levels;
uniform int minLevel;
uniform int validLevels;
uniform int showDebug; 
uniform vec2 sliceOffset[16];

#ifdef USE_FOG
uniform FogParams fogParams;
#endif

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

#ifdef USE_FOG
	// Calculate any fog contribution using vertex distance in eye space.
    float dist = length(eyeSpacePosition);
    float fogAmount = calcFogAmount(fogParams, abs(dist));
    FragColor = mix(tint * texCol, fogParams.color, fogAmount);
#else
    FragColor = tint * texCol;
#endif
}
