#version 330 core

out vec4 FragColor;

in vec4 DiffuseColor;

void main()
{
    FragColor = DiffuseColor;
}
