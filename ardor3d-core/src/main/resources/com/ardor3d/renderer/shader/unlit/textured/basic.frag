#version 330 core

out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D tex0;
uniform vec4 defaultColor;

void main()
{
    FragColor = defaultColor * texture(tex0, TexCoords);
}
