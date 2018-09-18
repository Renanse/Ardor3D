#version 330 core

in vec2 refrCoords;
in vec2 normCoords;
in vec2 foamCoords;
in vec4 viewCoords;
in vec3 viewTangetSpace;
in vec2 vNormal;
in vec4 vVertex;

out vec4 FragColor;

uniform sampler2D normalMap;
uniform sampler2D reflection;
uniform sampler2D dudvMap;
uniform sampler2D refraction;
uniform sampler2D depthMap;
uniform sampler2D foamMap;

uniform vec4 waterColor;
uniform vec4 waterColorEnd;
uniform int abovewater;
uniform int useFadeToFogColor;
uniform float amplitude;

void main()
{
	float fogDist = 0.0; //clamp((viewCoords.z-gl_Fog.start)*gl_Fog.scale,0.0,1.0);

	vec2 distOffset = texture(dudvMap, refrCoords).xy * 0.01;
	vec3 dudvColor = texture(dudvMap, normCoords + distOffset).xyz;
	dudvColor = normalize(dudvColor * 2.0 - 1.0) * 0.015;

	vec3 normalVector = texture(normalMap, normCoords + distOffset * 0.6).xyz;
	normalVector = normalVector * 2.0 - 1.0;
	normalVector = normalize(normalVector);
	normalVector.xy *= 0.5;

	vec3 localView = normalize(viewTangetSpace);
	float fresnel = dot(normalVector, localView);
	fresnel *= 1.0 - fogDist;
	float fresnelTerm = 1.0 - fresnel;
	fresnelTerm *= fresnelTerm;
	fresnelTerm *= fresnelTerm;
	fresnelTerm = fresnelTerm * 0.9 + 0.1;
	fresnel = 1.0 - fresnelTerm;

	vec2 projCoord = viewCoords.xy / viewCoords.q;
	projCoord = (projCoord + 1.0) * 0.5;
	vec2 projCoordDepth = projCoord;
	if ( abovewater == 1 ) {
		projCoord.x = 1.0 - projCoord.x;
	}

    projCoord += (vNormal + dudvColor.xy * 0.5 + normalVector.xy * 0.2);
	projCoord = clamp(projCoord, 0.001, 0.999);

    projCoordDepth += (vNormal + dudvColor.xy * 0.5 + normalVector.xy * 0.2);
	projCoordDepth = clamp(projCoordDepth, 0.001, 0.999);

	vec4 reflectionColor = texture(reflection, projCoord);
	if ( abovewater == 0 ) {
		reflectionColor *= vec4(0.8,0.9,1.0,1.0);
		vec4 endColor = mix(reflectionColor,waterColor,fresnelTerm);
		FragColor = mix(endColor,waterColor,fogDist);
	}
	else {
		vec4 waterColorNew = mix(waterColor,waterColorEnd,fresnelTerm);
		vec4 endColor = mix(waterColorNew,reflectionColor,fresnelTerm);
	
		float foamVal = (vVertex.y-vVertex.w) / (amplitude * 2.0);
		foamVal = clamp(foamVal,0.0,1.0);
		vec4 foamTex = texture(foamMap, foamCoords + vNormal * 0.6 + normalVector.xy * 0.05);
		float normLength = length(vNormal*5.0);
		foamVal *= 1.0-normLength;
		foamVal *= foamTex.a;
		endColor = mix(endColor,foamTex,clamp(foamVal,0.0,0.95));
	
		vec4 refractionColor = texture(refraction, projCoordDepth);
		float depth = texture(depthMap, projCoordDepth).r;
		depth = pow(depth,15.0);
		float invDepth = 1.0-depth;
	
		endColor = refractionColor*vec4(invDepth*fresnel) + endColor*vec4(depth*fresnel);
		if( useFadeToFogColor == 0) {
			FragColor = endColor + reflectionColor * vec4(fresnelTerm);
		} else {
			FragColor = (endColor + reflectionColor * vec4(fresnelTerm)); // * (1.0-fogDist) + gl_Fog.color * fogDist;
		}
	}
}