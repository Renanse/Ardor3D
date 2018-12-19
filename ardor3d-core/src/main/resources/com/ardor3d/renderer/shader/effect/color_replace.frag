#version 330 core

out vec4 FragColor;

#ifdef FLAT_COLORS
flat in vec4 DiffuseColor;
#else
in vec4 DiffuseColor;
#endif

uniform sampler2D inputTex;
uniform sampler2D colorRampTex;

uniform float redWeight;
uniform float greenWeight;
uniform float blueWeight;

in vec2 TexCoords0;

void main() {
	vec4 color = DiffuseColor * texture(inputTex, TexCoords0);
	vec3 convert = vec3(redWeight, greenWeight, blueWeight);
	
	float luminance = dot(convert, color.rgb);

    vec4 finalColor = texture( colorRampTex, vec2(luminance, .5) );
    finalColor.a = color.a; 
	
	FragColor = finalColor;
}