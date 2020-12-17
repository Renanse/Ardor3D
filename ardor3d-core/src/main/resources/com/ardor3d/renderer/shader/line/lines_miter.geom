#version 330 core

@import include/transform_functions.glsl

// Handles converting lines or line segments to triangle strips and connecting them with miter joints.
// portions inspired by https://github.com/paulhoux/Cinder-Samples/tree/master/GeometryShader

uniform float	miterLimit;
uniform float	lineWidth;
uniform vec2	viewSize;
uniform vec2	viewOffset;
uniform float	featherWidth;

layout(lines_adjacency) in;
layout(triangle_strip, max_vertices = 11) out;

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
	noperspective float distance;
} VertexOut;

@import line/line_shared_geom.glsl

void main()
{
	vec4 cs0 = gl_in[0].gl_Position;
	vec4 cs1 = gl_in[1].gl_Position;
	vec4 cs2 = gl_in[2].gl_Position;
	vec4 cs3 = gl_in[3].gl_Position;

	// ignore lines that are completely behind us.
	if (cs1.w <= 0 && cs2.w <= 0) return;
	
	// now clamp points so they end at roughly our view plane if they are behind us.  
	// This eliminates texturing issues and oddly flipped vertices.
	clampCSW(cs1, cs2, 0.1);
		
	// convert the vertices passed to the shader to screen space:
	vec3 ndc0 = clipToNDC(cs0);
	vec3 ndc1 = clipToNDC(cs1);
	vec3 ndc2 = clipToNDC(cs2);
	vec3 ndc3 = clipToNDC(cs3);
	vec2 p0 = ndcToScreen2D(ndc0, viewSize, viewOffset);	// start of previous segment
	vec2 p1 = ndcToScreen2D(ndc1, viewSize, viewOffset);	// end of previous segment, start of current segment
	vec2 p2 = ndcToScreen2D(ndc2, viewSize, viewOffset);	// end of current segment, start of next segment
	vec2 p3 = ndcToScreen2D(ndc3, viewSize, viewOffset);	// end of next segment

	// determine the direction of each of the 3 segments (previous, current, next)
	vec2 dirPrev = normalize(p1 - p0);
	vec2 dirCurr = normalize(p2 - p1);
	vec2 dirNext = normalize(p3 - p2);

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
		// XXX: we don't add anti-aliasing to the miter joint, but we could?
		if(dot(dirPrev, normCurr) > 0) {
			emitVert(p1 + lineWidth * 0.5 * normPrev, ndc1.z, VertexIn[1].color, 0, vec2(0, 0));
			emitVert(p1 + lineWidth * 0.5 * normCurr, ndc1.z, VertexIn[1].color, 0, vec2(0, 0));
			emitVert(p1, ndc1.z, VertexIn[1].color, 0, vec2(0.5, 0));
			EndPrimitive();
		}
		else {
			emitVert(p1 - lineWidth * 0.5 * normCurr, ndc1.z, VertexIn[1].color, 0, vec2(1, 0));
			emitVert(p1 - lineWidth * 0.5 * normPrev, ndc1.z, VertexIn[1].color, 0, vec2(1, 0));
			emitVert(p1, ndc1.z, VertexIn[1].color, 0, vec2(0.5, 0));
			EndPrimitive();
		}
	}

	if(dot(dirCurr, dirNext) < -miterLimit) {
		miter_b = normCurr;
		length_b = lineWidth * 0.5;
	}

	vec2 offsetA = length_a * miter_a;
	vec2 offsetB = length_b * miter_b;

	float distanceA = length(p1-p0);
	float distanceB = distanceA + length(p2-p1);

#ifndef ANTIALIAS
	// generate the triangle strip using two triangles
	emitVert(p1 + offsetA, ndc1.z, VertexIn[1].color, distanceA, vec2(1, 0));
	emitVert(p1 - offsetA, ndc1.z, VertexIn[1].color, distanceA, vec2(0, 0));
	emitVert(p2 + offsetB, ndc2.z, VertexIn[2].color, distanceB, vec2(1, 0));
	emitVert(p2 - offsetB, ndc2.z, VertexIn[2].color, distanceB, vec2(0, 0));
#else
	vec2 offsetFeatherA = (featherWidth + (length_a)) * miter_a;
	vec2 offsetFeatherB = (featherWidth + (length_b)) * miter_b;

	// generate the triangle strip using six triangles
	emitVert(p2 + offsetFeatherB, ndc2.z, vec4(VertexIn[2].color.xyz, 0.0), distanceB, vec2(1, 0));
	emitVert(p1 + offsetFeatherA, ndc1.z, vec4(VertexIn[1].color.xyz, 0.0), distanceA, vec2(1, 0));
	emitVert(p2 + offsetB, ndc2.z, VertexIn[2].color, distanceB, vec2(1, 0));
	emitVert(p1 + offsetA, ndc1.z, VertexIn[1].color, distanceA, vec2(1, 0));
	emitVert(p2 - offsetB, ndc2.z, VertexIn[2].color, distanceB, vec2(0, 0));
	emitVert(p1 - offsetA, ndc1.z, VertexIn[1].color, distanceA, vec2(0, 0));
	emitVert(p2 - offsetFeatherB, ndc2.z, vec4(VertexIn[2].color.xyz, 0.0), distanceB, vec2(0, 0));
	emitVert(p1 - offsetFeatherA, ndc1.z, vec4(VertexIn[1].color.xyz, 0.0), distanceA, vec2(0, 0));
#endif
	EndPrimitive();
}