#version 330 core

in vec2 refrCoords;
in vec2 normCoords;
in vec4 viewCoords;
in vec3 viewTangetSpace;

out vec4 FragColor;

uniform sampler2D normalMap;
uniform sampler2D reflection;
uniform sampler2D dudvMap;

uniform vec4 waterColor;
uniform vec4 waterColorEnd;
uniform int abovewater;
uniform int useFadeToFogColor;

void main()
{
	float fogDist = 0.0;//clamp((viewCoords.z-gl_Fog.start)*gl_Fog.scale,0.0,1.0);

	vec2 distOffset = texture(dudvMap, refrCoords).xy * 0.01;
	vec3 dudvColor = texture(dudvMap, normCoords + distOffset).xyz;
	dudvColor = normalize(dudvColor * 2.0 - 1.0) * 0.015;

	vec3 normalVector = texture(normalMap, normCoords + distOffset * 0.6).xyz;
	normalVector = normalVector * 2.0 - 1.0;
	normalVector = normalize(normalVector);
	normalVector.xy *= 0.5;

	vec3 localView = normalize(viewTangetSpace);
	float fresnel = dot(normalVector, localView);
	if ( abovewater == 0 ) {
		fresnel = -fresnel;
	}
	fresnel *= 1.0 - fogDist;
	float fresnelTerm = 1.0 - fresnel;
	fresnelTerm *= fresnelTerm;
	fresnelTerm = fresnelTerm * 0.9 + 0.1;
	fresnel = 1.0 - fresnelTerm;

	vec2 projCoord = viewCoords.xy / viewCoords.q;
	projCoord = (projCoord + 1.0) * 0.5;
	if ( abovewater == 1 ) {
		projCoord.x = 1.0 - projCoord.x;
	}

    projCoord += (dudvColor.xy * 0.5 + normalVector.xy * 0.2);
	projCoord = clamp(projCoord, 0.001, 0.999);

	vec4 reflectionColor = texture(reflection, projCoord);
	if ( abovewater == 0 ) {
		reflectionColor *= vec4(0.8,0.9,1.0,1.0);
		vec4 endColor = mix(reflectionColor,waterColor,fresnelTerm);
		FragColor = mix(endColor,waterColor,fogDist);
	}
	else {
		vec4 waterColorNew = mix(waterColor,waterColorEnd,fresnelTerm);
		vec4 endColor = mix(waterColorNew,reflectionColor,fresnelTerm);
		
		if( useFadeToFogColor == 0) {
			FragColor = mix(endColor,reflectionColor,fogDist);
		} else {
			FragColor = mix(endColor,reflectionColor,fogDist);// * (1.0-fogDist) + gl_Fog.color * fogDist;
		}
	}
}