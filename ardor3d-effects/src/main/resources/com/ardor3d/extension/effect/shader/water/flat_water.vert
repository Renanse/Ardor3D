#version 330 core

in vec3 vertex;
in vec3 normal;
in vec2 uv0;

out vec2 refrCoords;
out vec2 normCoords;
out vec4 viewCoords;
out vec3 viewTangetSpace;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat3 normalMat;

uniform mat4 reflectMat;
uniform mat4 dudvMat;

uniform vec3 tangent;
uniform vec3 binormal;
uniform float normalTranslation, refractionTranslation;

void main()
{
	vec3 normalVec = normalize(normalMat * normal);
	vec3 tangent2 = normalize(normalMat * tangent);
	vec3 binormal2 = normalize(normalMat * binormal);

	// Calculate the vector coming from the vertex to the camera
	vec4 v = view * model * vec4(vertex, 1.0);
	vec3 viewDir = normalize(-(v.xyz/v.w));

	// Compute tangent space for the view direction
	viewTangetSpace.x = dot(viewDir, tangent2);
	viewTangetSpace.y = dot(viewDir, binormal2);
	viewTangetSpace.z = dot(viewDir, normalVec);

	refrCoords = (dudvMat * vec4(uv0,0,1)).xy + vec2(0.0, refractionTranslation);
	normCoords = uv0.xy + vec2(0.0, normalTranslation);

	// This calculates our current projection coordinates
	viewCoords = projection * v;
	gl_Position = viewCoords;
}
