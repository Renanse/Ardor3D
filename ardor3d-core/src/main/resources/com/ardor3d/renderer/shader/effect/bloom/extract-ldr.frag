#version 330 core

out vec4 FragColor;

#ifdef FLAT_COLORS
flat in vec4 DiffuseColor;
#else
in vec4 DiffuseColor;
#endif

uniform sampler2D inputTex;

uniform float exposureIntensity;
uniform float exposureCutoff;

in vec2 TexCoords0;

void main() {
	vec4 color = texture(inputTex, TexCoords0);
	
	// Extracting luminance using Paul Haeberli's standard values
	float luminance = dot(vec3(0.3086, 0.6094, 0.0820), color.rgb);

	if (luminance < exposureCutoff ) {
		FragColor = vec4(0.0, 0.0, 0.0, color.a);
	} else {
		FragColor = vec4(color.rgb * exposureIntensity, color.a);
	}
}