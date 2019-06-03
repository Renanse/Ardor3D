#version 330 core

// Handles converting lines or line segments to triangle strips and connecting them with miter joints.
// portions inspired by https://github.com/paulhoux/Cinder-Samples/tree/master/GeometryShader

uniform float	miterLimit;
uniform float	lineWidth;
uniform vec2	viewSize;
uniform vec2	viewOffset;

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 7) out;

in VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif
} VertexIn[4];

out VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif
	
	vec2 uv0;
} VertexOut;


vec3 toScreenSpace(vec4 clipSpacePos)
{
	vec3 ndcSpacePos = (clipSpacePos.xyz / clipSpacePos.w);
	vec2 windowSpacePos = ((ndcSpacePos.xy + 1.0) / (2.0 * sign(clipSpacePos.w))) * viewSize + viewOffset;
	return vec3(windowSpacePos, ndcSpacePos.z);
}

vec4 toNDCSpace(vec3 point, vec2 offset) {
    return vec4((2.0 * (point.xy + offset - viewOffset) / viewSize) - 1.0, point.z, 1.0);
}

void main(void)
{
	// ignore lines that are behind us.
	if (gl_in[1].gl_Position.w <= 0 &&
		gl_in[2].gl_Position.w <= 0) return;
		
	// convert the vertices passed to the shader to screen space:
	vec3 p0 = toScreenSpace(gl_in[0].gl_Position);	// start of previous segment
	vec3 p1 = toScreenSpace(gl_in[1].gl_Position);	// end of previous segment, start of current segment
	vec3 p2 = toScreenSpace(gl_in[2].gl_Position);	// end of current segment, start of next segment
	vec3 p3 = toScreenSpace(gl_in[3].gl_Position);	// end of next segment

	// determine the direction of each of the 3 segments (previous, current, next)
	vec2 dirPrev = normalize(p1.xy - p0.xy);
	vec2 dirCurr = normalize(p2.xy - p1.xy);
	vec2 dirNext = normalize(p3.xy - p2.xy);

	// determine the normal of each of the 3 segments (previous, current, next)
	vec2 normPrev = vec2(-dirPrev.y, dirPrev.x);
	vec2 normCurr = vec2(-dirCurr.y, dirCurr.x);
	vec2 normNext = vec2(-dirNext.y, dirNext.x);

	// determine miter lines by averaging the normals of the 2 segments
	vec2 miter_a = normalize(normPrev + normCurr);	// miter at start of current segment
	vec2 miter_b = normalize(normCurr + normNext);	// miter at end of current segment

	// determine the length of the miter by projecting it onto normal and then inverse it
	float length_a = lineWidth * 0.5 / dot(miter_a, normCurr);
	float length_b = lineWidth * 0.5 / dot(miter_b, normCurr);

	// prevent excessively long miters at sharp corners
	if(dot(dirPrev, dirCurr) < -miterLimit) {
		miter_a = normCurr;
		length_a = lineWidth * 0.5;

		// close the gap
		if(dot(dirPrev, normCurr) > 0) {
			VertexOut.uv0 = vec2(0, 0);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, lineWidth * 0.5 * normPrev);
			EmitVertex();

			VertexOut.uv0 = vec2(0, 0);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, lineWidth * 0.5 * normCurr);
			EmitVertex();

			VertexOut.uv0 = vec2(0, 0.5);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, vec2(0.0));
			EmitVertex();

			EndPrimitive();
		}
		else {
			VertexOut.uv0 = vec2(0, 1);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, -lineWidth * 0.5 * normCurr);
			EmitVertex();

			VertexOut.uv0 = vec2(0, 1);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, -lineWidth * 0.5 * normPrev);
			EmitVertex();

			VertexOut.uv0 = vec2(0, 0.5);
			VertexOut.color = VertexIn[1].color;
			gl_Position = toNDCSpace(p1, vec2(0.0));
			EmitVertex();

			EndPrimitive();
		}
	}

	if(dot(dirCurr, dirNext) < -miterLimit) {
		miter_b = normCurr;
		length_b = lineWidth * 0.5;
	}

	// generate the triangle strip
	VertexOut.uv0 = vec2(0, 0);
	VertexOut.color = VertexIn[1].color;
	gl_Position = toNDCSpace(p1, length_a * miter_a);
	EmitVertex();

	VertexOut.uv0 = vec2(1, 0);
	VertexOut.color = VertexIn[1].color;
	gl_Position = toNDCSpace(p1, -length_a * miter_a);
	EmitVertex();

	VertexOut.uv0 = vec2(0, 0);
	VertexOut.color = VertexIn[2].color;
	gl_Position = toNDCSpace(p2, length_b * miter_b);
	EmitVertex();

	VertexOut.uv0 = vec2(1, 0);
	VertexOut.color = VertexIn[2].color;
	gl_Position = toNDCSpace(p2, -length_b * miter_b);
	EmitVertex();

	EndPrimitive();
}