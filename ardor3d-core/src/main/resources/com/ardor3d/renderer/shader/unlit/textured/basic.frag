#version 330 core

out vec4 FragColor;

in vec2 TexCoord0;

uniform sampler2D tex0;

void main()
{
    FragColor = texture(tex0, TexCoord0);
}
