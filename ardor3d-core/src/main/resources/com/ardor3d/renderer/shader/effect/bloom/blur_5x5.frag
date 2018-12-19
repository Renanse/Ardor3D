#version 330 core

#ifndef HORIZONTAL
#define HORIZONTAL true
#endif

out vec4 FragColor;
  
in vec2 TexCoords0;

uniform sampler2D inputTex;

uniform float weight[5] = float[] (0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main()
{             
    vec2 tex_offset = 1.0 / textureSize(inputTex, 0);
    vec3 result = texture(inputTex, TexCoords0).rgb * weight[0];

    if (HORIZONTAL) {
        for (int i = 1; i < 5; ++i) {
            result += texture(inputTex, TexCoords0 + vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
            result += texture(inputTex, TexCoords0 - vec2(tex_offset.x * i, 0.0)).rgb * weight[i];
        }
    }
    
    else {
        for (int i = 1; i < 5; ++i) {
            result += texture(inputTex, TexCoords0 + vec2(0.0, tex_offset.y * i)).rgb * weight[i];
            result += texture(inputTex, TexCoords0 - vec2(0.0, tex_offset.y * i)).rgb * weight[i];
        }
    }
    FragColor = vec4(result, 1.0);
}