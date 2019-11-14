#ifndef TERRAIN_FRAG_INC
#define TERRAIN_FRAG_INC

void computeUnit(out float unit, const in vec2 vVertex, const in int minLevel, 
                  const in int validLevels, const in float textureSize)
{
	// get our texture size as a power of 2.
	float log2texSize = log2(textureSize);
	
	// determine the closest power of two lower than the max X or Y distance to the vertex from the eye. 
	float maxDistance = floor(max(abs(vVertex.x), abs(vVertex.y)));
	unit = floor(log2(maxDistance));
	
	// Determine our clipmap unit by subtracting our texture's size as pow 2.
	//
	// We remove 2 from our size before this: 
	//  -  One for the fact that we center our clipmap, so we are dealing with half distance
	//  -  Another because we want to be in a given clipmap up UNTIL hitting the given size.
	//
	// For example, if our texture is size 128, our 0th clipmap will cover (-64, 64).  128 is
	// 2^7.  For a vertex that is 32 units away, we'd have unit = log2(32) = 5, then we'd 
	// subtract (7 - 2) from that, giving us a final unit of 0.  At distance 64, the resulting
	// unit is 1.  [log(64) - (7 - 2) = 6 - 5 = 1.]
	unit -= (log2texSize - 2);
	
	// Now make sure that our unit falls within in an appropriate range.
	unit = clamp(unit, minLevel, validLevels);
}

/**
 * set up values needed to lookup texture value in clipmap
 */
void calculateClipUVs(const in float unit, const in float textureSize, const in float texelSize,
                  out vec3 texCoord1, out vec3 texCoord2, out vec2 fadeCoord)
{
	// get our texture size as a power of 2.
	float log2texSize = log2(textureSize);

	// We now calculate 2 texture coordinates - this is to allow us to blend between levels
	
	// Determine our first texcoord - divide our distance vector by our unit texture size,
	// This will give ous a range of [-.5, .5].  We add .5 to shift us to [0, 1]
	vec2 uv1 = vVertex / vec2(exp2(unit + log2texSize));
	fadeCoord = uv1; // save our [-.5, .5] tex coord for later use
	uv1 += vec2(0.5);
	uv1 *= vec2(1.0 - texelSize);
	uv1 += sliceOffset[int(unit)];

	// figure the next farthest out unit for blending
	float unit2 = unit + 1.0;
	unit2 = min(unit2, validLevels);

	// Determine our second texcoord - divide our distance vector by our unit texture size,
	// This will give us a range of [-.5, .5].  We add .5 to shift us to [0, 1]
	vec2 uv2 = vVertex/vec2(exp2(unit2+log2texSize));
	uv2 += vec2(0.5);
	uv2 *= vec2(1.0 - texelSize);
	uv2 += sliceOffset[int(unit2)];
	  	
	// Determine our depth texture coords
	float z1 = clamp(unit / levels, 0.0, 0.99);
	float z2 = clamp(unit2 / levels, 0.0, 0.99);
	
	texCoord1 = vec3(uv1, z1);
	texCoord2 = vec3(uv2, z2);
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
 * Look up appropriate color in texture clipmap, taking blending to next clip level and 
 * optional debug into consideration
 */
vec4 clipTexColor(in sampler3D texture,
                  in vec3 texCoord1, in vec3 texCoord2,
                  in vec2 fadeCoord, const in float textureSize, 
                  const in float texelSize, const in int showDebug)
{
	// sample our textures - this texture and the next furthest for blending
	vec4 tex1 = texture3DBilinear(texture, texCoord1, textureSize, texelSize);
	vec4 tex2 = texture3DBilinear(texture, texCoord2, textureSize, texelSize);

	// Now, determine our crossfade between sampled textures using our original [-.5, 5] uv
	float fadeVal = max(abs(fadeCoord.x), abs(fadeCoord.y)) * 2.05;
	
	// Fade between textures in the last 20% of our texture.
	fadeVal = max(0.0, fadeVal - 0.8) * 5.0;
	fadeVal = min(1.0, fadeVal);

	// Mix the textures using our fade value.  
	// Add an optional white color if debug is enabled.
	return mix(tex1, tex2, fadeVal) + vec4(fadeVal * showDebug);
}

#endif