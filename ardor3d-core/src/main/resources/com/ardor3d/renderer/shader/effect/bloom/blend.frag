#version 330 core

out vec4 FragColor;

in vec2 TexCoords0;

uniform sampler2D scene;
uniform sampler2D bloomBlur;

uniform float exposure;

void main()
{             
	vec4 sceneColor = texture(scene, TexCoords0);
    vec3 hdrColor = sceneColor.rgb;
    vec3 bloomColor = texture(bloomBlur, TexCoords0).rgb;
    hdrColor += bloomColor;
    
    // tone mapping
    vec3 result = vec3(1.0) - exp(-hdrColor * exposure);
    
    // gamma correct
    const float gamma = 2.2;
    result = pow(result, vec3(1.0 / gamma));

    FragColor = vec4(result, 1);
}