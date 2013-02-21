
#extension GL_ARB_draw_instanced: enable

uniform int nrOfInstances = -1;
uniform vec4[120] transforms;
varying vec4 color;


void main()
{
	vec3 normal = vec3(0,0,0);
	vec4 pos = vec4(0,0,0,0);
	color = vec4(0.0,0.0,1.0,0.0);
	if(nrOfInstances > 0)
	{
		mat4 transform =   mat4(transforms[gl_InstanceID * 4],
	                            transforms[gl_InstanceID * 4 + 1],
	                            transforms[gl_InstanceID * 4 + 2],
	                            transforms[gl_InstanceID * 4 + 3] );;
		mat4 modelviewmatrix = gl_ModelViewMatrix * transform;
		pos = modelviewmatrix * gl_Vertex;
		
		normal = (modelviewmatrix * vec4(gl_Normal, 0.0)).xyz;

		color = vec4(0.0,1.0,0.0,0.0);
	}
	else
	{ 
		pos = gl_ModelViewMatrix * gl_Vertex;
		normal = gl_NormalMatrix * gl_Normal;
		color = vec4(1.0,0.0,0.0,0.0);
	}
	
    gl_Position = gl_ProjectionMatrix  * pos;            
    
}

