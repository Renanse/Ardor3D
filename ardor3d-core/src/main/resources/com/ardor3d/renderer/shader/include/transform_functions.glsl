#ifndef TRANSFORM_FUNCTIONS_INC
#define TRANSFORM_FUNCTIONS_INC

vec3 clipToNDC(vec4 clipPoint)
{
	return clipPoint.xyz / clipPoint.w;
}

vec2 ndcToScreen2D(vec3 ndcPoint, vec2 viewSize, vec2 viewOffset)
{
	return (ndcPoint.xy + 1.0) * 0.5 * viewSize + viewOffset;
}

vec2 clipToScreen2D(vec4 clipPoint, vec2 viewSize, vec2 viewOffset)
{
	return ndcToScreen2D(clipToNDC(clipPoint), viewSize, viewOffset);
}

vec3 screen2DToNDC(vec2 screenPoint, vec2 viewSize, vec2 viewOffset, float ndcZ) {
    return vec3((2.0 * (screenPoint - viewOffset) / viewSize) - 1.0, ndcZ);
}

vec4 ndcToClip(vec3 ndcPoint, float clipW)
{
    return vec4(ndcPoint * clipW, clipW);
}

vec4 screen2DToClip(vec2 screenPoint, vec2 viewSize, vec2 viewOffset, float ndcZ, float clipW)
{
	return ndcToClip(screen2DToNDC(screenPoint, viewSize, viewOffset, ndcZ), clipW);
}

#endif