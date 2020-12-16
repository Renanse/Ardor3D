void emitVert(vec2 screenPoint, float ndcZ, vec4 color, float distance, vec2 uv)
{
	VertexOut.uv0 = uv;
	VertexOut.color = color;
	VertexOut.distance = distance;
	gl_Position = screen2DToClip(screenPoint, viewSize, viewOffset, ndcZ, 1.0);
	EmitVertex();
}