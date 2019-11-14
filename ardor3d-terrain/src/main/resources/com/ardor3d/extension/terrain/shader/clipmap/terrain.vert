#version 330 core

in vec4 vertex;
 
out vec2 vVertex;             // our vertex position relative to our eye.
out vec3 eyeSpacePosition;
 
uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
 
uniform vec3 eyePosition;
uniform float textureDensity;
uniform int vertexDistance;
uniform int clipSideSize;

void applyTerrainBlending(inout vec4 position)
{
    float scaledClipSideSize = clipSideSize * vertexDistance * 0.5;
    vec2 viewDistance = abs(position.xz - eyePosition.xz);
    float maxDistance = max(viewDistance.x, viewDistance.y) / scaledClipSideSize;
    float blend = clamp((maxDistance - 0.51) * 2.2, 0.0, 1.0);

    position.y = mix(position.y, position.w, blend);
    position.w = 1.0;
}

void main() 
{
    // Sent to fragment program for texture clipmap generation.
    // This is the signed distance from our eye to the vertex/pixel, modified by our 
    // pixel density.  A higher density means the texture is packed more tightly 
    // around the viewer. We accomplish this by increasing the distance to the pixel.
    vVertex = (vertex.xz - eyePosition.xz) * textureDensity;

	// Apply terrain height blending    
    // assign vertex to position so we can use in in/out param 
    vec4 position = vertex;
    applyTerrainBlending(position);
    
    // Used for fog calculations
    mat4 modelViewMatrix = view * model;
    eyeSpacePosition = (modelViewMatrix * position).xyz;
    
    gl_Position = projection * modelViewMatrix * position;
}
