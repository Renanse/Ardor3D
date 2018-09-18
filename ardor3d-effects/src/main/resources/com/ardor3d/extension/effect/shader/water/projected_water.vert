#version 330 core

in vec3 vertex;
in vec3 normal;
in vec2 uv0;

out vec2 refrCoords;
out vec2 normCoords;
out vec2 foamCoords;
out vec4 viewCoords;
out vec3 viewTangetSpace;
out vec2 vNormal;
out vec4 vVertex;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform mat4 reflectMat;
uniform mat4 dudvMat;

uniform vec3 tangent;
uniform vec3 binormal;
uniform float normalTranslation, refractionTranslation;

uniform vec3 cameraLoc;
uniform float waterHeight;
uniform float heightFalloffStart;
uniform float heightFalloffSpeed;

void main()
{
	vVertex = vec4(vertex, 1.0);
	viewCoords = projection * view * model * vVertex;
	float heightAdjust = 1.0 - clamp((viewCoords.z-heightFalloffStart)/heightFalloffSpeed,0.0,1.0);
	vVertex.y = mix(waterHeight,vVertex.y,heightAdjust);
	viewCoords = projection * view * model * vVertex;
	gl_Position = viewCoords;
	vVertex.w = waterHeight;

	vec3 normalVec = vec3(normal.x*heightAdjust,normal.y,normal.z*heightAdjust);
	vNormal = normalVec.xz * 0.15;

	// Calculate the vector coming from the vertex to the camera
	vec3 viewDir = cameraLoc - vertex;

	// Compute tangent space for the view direction
	viewTangetSpace.x = dot(viewDir, tangent);
	viewTangetSpace.y = dot(viewDir, binormal);
	viewTangetSpace.z = dot(viewDir, normal);

	//todo test 0.8
	refrCoords = (dudvMat * vec4(uv0,0,1)).xy + vec2(0.0,refractionTranslation);
	normCoords = uv0.xy + vec2(0.0,normalTranslation);
	foamCoords = uv0.xy + vec2(0.0,normalTranslation*0.4);
}
