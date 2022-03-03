#version 330 core

@import include/transform_functions.glsl

// Handles converting lines or line segments to triangle strips.
// portions inspired by https://github.com/paulhoux/Cinder-Samples/tree/master/GeometryShader

uniform float	lineWidth;
uniform vec2	viewSize;
uniform vec2	viewOffset;
uniform float	featherWidth;

uniform mat4 textureMatrix0;

layout(lines) in;
layout(triangle_strip, max_vertices=8) out;

in VertexData{
	#ifdef FLAT_COLORS
	flat vec4 color;
	#else
	vec4 color;
	#endif

	float texV;
} VertexIn[2];

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

	// ignore lines that are completely behind us.
	if (cs0.w <= 0 && cs1.w <= 0) return;
	
	// now clamp points so they end at roughly our view plane if they are behind us.  
	// This eliminates texturing issues and oddly flipped vertices.
	clampCSW(cs0, cs1, 0.1);
		
	// convert the vertices passed to the shader to screen space:
	vec3 ndc0 = clipToNDC(cs0);
	vec3 ndc1 = clipToNDC(cs1);
	vec2 p0 = ndcToScreen2D(ndc0, viewSize, viewOffset);	// start of current segment
	vec2 p1 = ndcToScreen2D(ndc1, viewSize, viewOffset);	// end of current segment

	// determine the direction of the segment
	vec2 dir = normalize(p1 - p0);

	// determine the normal of the segment
	vec2 normal = vec2(-dir.y, dir.x);
	
	// multiply normal by lineWidth to get offset
	// we will add/subtract this to our screen space points and then convert
	// the points back to a modified clip space to get our new triangle strip line.
	vec2 offset = lineWidth * 0.5 * normal;

	// come up with our start and end uv coords
	vec2 texVALeft = vec2(textureMatrix0 * vec4(0.0, VertexIn[0].texV, 1.0, 1.0));
	vec2 texVARight = vec2(textureMatrix0 * vec4(1.0, VertexIn[0].texV, 1.0, 1.0));
	vec2 texVBLeft = vec2(textureMatrix0 * vec4(0.0, VertexIn[1].texV, 1.0, 1.0));
	vec2 texVBRight = vec2(textureMatrix0 * vec4(1.0, VertexIn[1].texV, 1.0, 1.0));
	
	float distanceA = 0;
	float distanceB = distanceA + length(p1 - p0);

#ifndef ANTIALIAS
	// generate the triangle strip using two triangles
	emitVert(p0 + offset, ndc0.z, VertexIn[0].color, distanceA, texVARight);
	emitVert(p0 - offset, ndc0.z, VertexIn[0].color, distanceA, texVALeft);
	emitVert(p1 + offset, ndc1.z, VertexIn[1].color, distanceB, texVBRight);
	emitVert(p1 - offset, ndc1.z, VertexIn[1].color, distanceB, texVBLeft);
#else
	vec2 offsetFeather = (featherWidth + (lineWidth * 0.5)) * normal;
	
	// generate the triangle strip using six triangles
	emitVert(p1 + offsetFeather, ndc1.z, vec4(VertexIn[1].color.xyz, 0.0), distanceB, texVBRight);
	emitVert(p0 + offsetFeather, ndc0.z, vec4(VertexIn[0].color.xyz, 0.0), distanceA, texVARight);
	emitVert(p1 + offset, ndc1.z, VertexIn[1].color, distanceB, texVBRight);
	emitVert(p0 + offset, ndc0.z, VertexIn[0].color, distanceA, texVARight);
	emitVert(p1 - offset, ndc1.z, VertexIn[1].color, distanceB, texVBLeft);
	emitVert(p0 - offset, ndc0.z, VertexIn[0].color, distanceA, texVALeft);
	emitVert(p1 - offsetFeather, ndc1.z, vec4(VertexIn[1].color.xyz, 0.0), distanceB, texVBLeft);
	emitVert(p0 - offsetFeather, ndc0.z, vec4(VertexIn[0].color.xyz, 0.0), distanceA, texVALeft);
#endif
	EndPrimitive();
}