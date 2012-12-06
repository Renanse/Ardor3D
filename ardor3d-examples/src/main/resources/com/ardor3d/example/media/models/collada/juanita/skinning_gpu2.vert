
attribute mat3 Weights;
attribute mat3 JointIDs;

uniform mat4 JointPalette[50];

varying vec3 lightVec;
varying vec3 halfVec;
varying vec3 eyeVec;
varying vec2 texCoord;
varying float lightDistance;

void main(void) {
    mat4 mat = mat4(0.0);

    for (int i = 0; i < 3; i++) {
        vec3 w = Weights[i];
        vec3 d = JointIDs[i];
        for (int j = 0; j < 3; j++) {
            mat += JointPalette[int(d[j])] * w[j];
        }
    }

    gl_Position = gl_ModelViewProjectionMatrix * (mat * gl_Vertex);

    vec3 n = gl_NormalMatrix * (mat3(mat[0].xyz,mat[1].xyz,mat[2].xyz) * gl_Normal);
    n = normalize(n);
    vec3 t;
	vec3 b;
	vec3 v;

	vec3 c1 = cross(n, vec3(0.0, 0.0, 1.0));
	vec3 c2 = cross(n, vec3(0.0, 1.0, 0.0));

	if(length(c1)>length(c2)) {
		t = c1;
	} else {
		t = c2;
	}

	t = normalize(t);

	b = cross(n, t);
	b = normalize(b);


	vec3 vVertex = vec3(gl_ModelViewMatrix * mat * gl_Vertex);

	vec3 tmpVec = gl_LightSource[0].position.xyz - vVertex;
	vec3 lightDir = normalize(tmpVec);

    lightDistance = length(tmpVec);


	lightVec.x = dot(tmpVec, t);
	lightVec.y = dot(tmpVec, b);
	lightVec.z = dot(tmpVec, n);

	tmpVec = -vVertex;
	eyeVec.x = dot(tmpVec, t);
	eyeVec.y = dot(tmpVec, b);
	eyeVec.z = dot(tmpVec, n);

    vVertex = normalize(vVertex);

    /* Normalize the halfVector to pass it to the fragment shader */
    vec3 halfVector = normalize((vVertex + lightDir) / 2.0);
    v.x = dot (halfVector, t);
    v.y = dot (halfVector, b);
    v.z = dot (halfVector, n);
    halfVec = normalize (v);

    gl_TexCoord[0] = gl_MultiTexCoord0;
}


