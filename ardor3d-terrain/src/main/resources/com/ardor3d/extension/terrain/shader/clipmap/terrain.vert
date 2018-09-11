#version 330 core

in vec4 vertex;
 
out vec2 vVertex;
out vec3 EyeSpacePosition;
 
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
 
uniform vec3 eyePosition;
uniform float scale;
uniform float vertexDistance;
uniform float clipSideSize;

vec4 applyTerrainBlending(vec4 position)
{
    float scaledClipSideSize = clipSideSize * vertexDistance * 0.5;
    vec2 viewDistance = abs(position.xz - eyePosition.xz);
    float maxDistance = max(viewDistance.x, viewDistance.y) / scaledClipSideSize;
    float blend = clamp((maxDistance - 0.51) * 2.2, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
    
    return position;
}

void main() 
{
	vVertex = (vertex.xz - eyePosition.xz) * vec2(scale/32.0);
    
    vec4 position = applyTerrainBlending(vertex);
	vec4 eyeViewSpacePos = view * model * position;
    EyeSpacePosition = eyeViewSpacePos.xyz;
    gl_Position =  projection * eyeViewSpacePos;
}
