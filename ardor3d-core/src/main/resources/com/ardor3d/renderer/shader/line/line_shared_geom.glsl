void emitVert(const vec2 screenPoint, const float ndcZ, const vec4 color, const float distance, const vec2 uv)
{
	VertexOut.uv0 = uv;
	VertexOut.color = color;
	VertexOut.distance = distance;
	gl_Position = screen2DToClip(screenPoint, viewSize, viewOffset, ndcZ, 1.0);
	EmitVertex();
}

void clampCSW(inout vec4 csA, inout vec4 csB, const float minW)
{
	vec4 csNorm;
	if (csA.w < minW) {
		csNorm = normalize(csB - csA);
		csA += csNorm * ((0.1 - csA.w) / csNorm.w);
	}
	if (csB.w < minW) {
		csNorm = normalize(csA - csB);
		csB += csNorm * ((0.1 - csB.w) / csNorm.w);
	}
}
