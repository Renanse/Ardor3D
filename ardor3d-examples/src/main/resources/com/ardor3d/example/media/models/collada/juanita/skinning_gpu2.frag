
varying vec3 lightVec;
varying vec3 eyeVec;
varying vec3 halfVec;

varying float lightDistance;

uniform sampler2D colorMap;
uniform sampler2D normalMap;
uniform sampler2D specularMap;
uniform float quantizationFactor;
uniform bvec4 flags;

void main (void)
{
    vec3 normal = normalize( texture2D(normalMap, gl_TexCoord[0].st).xyz * 2.0 - 1.0);

    float lambertFactor;
    if (flags[0]) {
        lambertFactor = max( dot(lightVec, normal), 0.0 );
    } else {
        lambertFactor = max( dot(lightVec, vec3(0,0,1) ), 0.0 ); //lambert factor
    }

    vec4 ambientLight = gl_LightSource[0].ambient;

	vec4 base = texture2D(colorMap, gl_TexCoord[0].st);

    vec4 diffuseMaterial;
    vec4 diffuseLight;

    // compute specular lighting
    vec4 specularMaterial ;
    vec4 specularLight ;
    float shininess ;

    if (flags[1]) {
        diffuseMaterial = texture2D (colorMap, gl_TexCoord[0].st); //should be squared but looks bad.
//        diffuseMaterial *= diffuseMaterial;
        //diffuseMaterial = pow(texture2D (colorMap, gl_TexCoord[0].st),2.0);
    } else {
        diffuseMaterial = vec4(0.2);
    }
    diffuseLight  = gl_LightSource[0].diffuse;

    if (flags[2]) {
//        specularMaterial =  pow(texture2D (specularMap, gl_TexCoord[0].st),2.0);
        specularMaterial =  texture2D (specularMap, gl_TexCoord[0].st);
        specularMaterial *= specularMaterial;
    } else {
        specularMaterial = vec4(0.2);
    }

    specularLight = gl_LightSource[0].specular;
    shininess = pow (max (dot (halfVec, normal), 0.0), 0.01)  ;


    gl_FragColor =	diffuseMaterial * diffuseLight * lambertFactor;
    gl_FragColor +=	specularMaterial * specularLight * shininess ;
//    gl_FragColor *= 		shadow;

    gl_FragColor /= (gl_LightSource[0].constantAttenuation +
                    gl_LightSource[0].linearAttenuation * lightDistance +
                    gl_LightSource[0].quadraticAttenuation * lightDistance * lightDistance) ;

    gl_FragColor +=	ambientLight;

    //posterization

    gl_FragColor.xyz =
            max(
                floor(gl_FragColor.xyz / quantizationFactor) * quantizationFactor,
                0.0);
}