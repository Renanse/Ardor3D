#version 330 core

in vec2 TexCoords;

out vec4 FragColor;

uniform float sampleDist;
uniform sampler2D texMap;

void main(void)
{
   vec4 sum = vec4(0.0);

   sum += texture(texMap, vec2(TexCoords.x, TexCoords.y)) * 6.0/16.0;
   sum += texture(texMap, vec2(TexCoords.x, TexCoords.y - 1.0*sampleDist)) * 4.0/16.0;
   sum += texture(texMap, vec2(TexCoords.x, TexCoords.y - 2.0*sampleDist)) * 3.0/16.0;
   sum += texture(texMap, vec2(TexCoords.x, TexCoords.y - 3.0*sampleDist)) * 2.0/16.0;
   sum += texture(texMap, vec2(TexCoords.x, TexCoords.y - 4.0*sampleDist)) * 1.0/16.0;

   FragColor = sum;
}