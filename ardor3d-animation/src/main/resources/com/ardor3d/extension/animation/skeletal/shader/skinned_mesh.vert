#version 330 core

in vec3 vertex;
in vec3 normal;
in vec2 uv0;

#ifdef MATRIX4_WEIGHTS
in mat4 weights;
in mat4 jointIds;
#else
in vec4 weights;
in vec4 jointIds;
#endif

out vec3 WorldPos;
out vec3 Normal;
out vec2 TexCoords;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform mat4 jointPalette[50];

// ----------------------------------------------------------------------------
mat4 calculateWeightMatrix()
{
    mat4 mat = mat4(0.0);
    
#ifdef MATRIX4_WEIGHTS
    for (int i = 0; i < 4; i++) {
        vec4 w = weights[i];
        vec4 d = jointIds[i];
        for (int j = 0; j < 4; j++) {
            mat += jointPalette[int(d[j])] * w[j];
        }
    }
#else
    mat += jointPalette[int(jointIds[0])] * weights[0];
    mat += jointPalette[int(jointIds[1])] * weights[1];
    mat += jointPalette[int(jointIds[2])] * weights[2];
    mat += jointPalette[int(jointIds[3])] * weights[3];
#endif
	
	return mat;
}
// ----------------------------------------------------------------------------
void main()
{
    mat4 weightMat = calculateWeightMatrix();

    WorldPos = vec3(model * weightMat * vec4(vertex, 1.0));
    Normal = mat3(model) * (mat3(weightMat[0].xyz,weightMat[1].xyz,weightMat[2].xyz) * normal);
     
    TexCoords = uv0;

    gl_Position =  projection * view * vec4(WorldPos, 1.0);
}
