#ifndef TERRAIN_FRAG_INC
#define TERRAIN_FRAG_INC

void computeUnit1(inout float unit1, const in vec2 vVertex, 
				  const in int validLevels, const in int minLevel)
{
	unit1 = (max(abs(vVertex.x), abs(vVertex.y)));
	unit1 = floor(unit1);
	unit1 = log2(unit1);
	unit1 = floor(unit1);
	unit1 = min(unit1, validLevels);
    unit1 = max(unit1, minLevel);
}

/**
 * approximation of bilinear texture filtering of a 3d slice
 */
vec4 texture3DBilinear( const in sampler3D textureSampler, const in vec3 uv, 
						const in float textureSize, const in float texelSize)
{
    vec4 tl = texture(textureSampler, uv);
    vec4 tr = texture(textureSampler, uv + vec3(texelSize, 0, 0));
    vec4 bl = texture(textureSampler, uv + vec3(0, texelSize, 0));
    vec4 br = texture(textureSampler, uv + vec3(texelSize , texelSize, 0));

    vec2 f = fract( uv.xy * textureSize );
    vec4 tA = mix( tl, tr, f.x );
    vec4 tB = mix( bl, br, f.x );
    return mix( tA, tB, f.y );
}

/**
 * set up values needed to lookup texture value in clipmap
 */
void clipTexSetup(inout float unit1,  out float unit2,
                  out vec2 texCoord1, out vec2 texCoord2, 
                  out vec2 offset1,   out vec2 offset2,
                  out vec2 fadeCoord, const in float texelSize)
{
  offset1 = sliceOffset[int(unit1)];
  float frac = unit1;
  frac = exp2(frac);
  frac *= 4.0; //Magic number
  texCoord1 = vVertex/vec2(frac);
  fadeCoord = texCoord1;
  texCoord1 += vec2(0.5);
  texCoord1 *= vec2(1.0 - texelSize);
  texCoord1 += offset1;

  unit2 = unit1 + 1.0;
  unit2 = min(unit2, validLevels);
  offset2 = sliceOffset[int(unit2)];
  float frac2 = unit2;
  frac2 = exp2(frac2);
  frac2 *= 4.0; //Magic number
  texCoord2 = vVertex/vec2(frac2);
  texCoord2 += vec2(0.5);
  texCoord2 *= vec2(1.0 - texelSize);
  texCoord2 += offset2;

  unit1 /= levels;
  unit1 = clamp(unit1, 0.0, 0.99);

  unit2 /= levels;
  unit2 = clamp(unit2, 0.0, 0.99);
  
  return;
}

/**
 * lookup color in texture clipmap
 */
vec4 clipTexColor(in sampler3D texture,
                  in float unit1, in float unit2,
                  in vec2 texCoord1, in vec2 texCoord2, 
                  in vec2 offset1, in vec2 offset2,
                  in vec2 fadeCoord, const in float textureSize, 
                  const in float texelSize, const in int showDebug)
{
  vec4 tex = texture3DBilinear(texture, vec3(texCoord1.x, texCoord1.y, unit1), textureSize, texelSize);
  vec4 tex2 = texture3DBilinear(texture, vec3(texCoord2.x, texCoord2.y, unit2), textureSize, texelSize);

  float fadeVal1 = abs(fadeCoord.x)*2.05;
  float fadeVal2 = abs(fadeCoord.y)*2.05;
  float fadeVal = max(fadeVal1, fadeVal2);
  fadeVal = max(0.0, fadeVal-0.8)*5.0;
  fadeVal = min(1.0, fadeVal);
  return mix(tex, tex2, fadeVal) + vec4(fadeVal*showDebug);
}

#endif