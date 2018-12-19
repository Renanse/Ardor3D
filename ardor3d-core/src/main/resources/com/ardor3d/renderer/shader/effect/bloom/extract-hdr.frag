#version 330 core

out vec4 FragColor;

#ifdef FLAT_COLORS
flat in vec4 DiffuseColor;
#else
in vec4 DiffuseColor;
#endif

uniform sampler2D inputTex;

in vec2 TexCoords0;

void main() {
	vec4 color = texture(inputTex, texCoord);
	
	// Extracting luminance using itu-r bt.709 HDTV standards values
	float luminance = dot(vec3(0.2127, 0.7152, 0.0722), color.rgb);

	if (luminance <= 1.0 ) {
		FragColor = vec4(0.0, 0.0, 0.0, color.a);
	} else {
		FragColor = color;
	}
}