#ifndef FOG_INC
#define FOG_INC

// com.ardor3d.renderer.material.fog.FogParams
struct FogParams {
    vec4 color;
    float start;
    float end;
    float density;
    int function;
};

uniform FogParams fogParams;

float calcLinearFogAmount(const float start, const float end, const float fogCoord)
{
	return 1.0 - clamp((end - fogCoord) / (end - start), 0.0, 1.0);
}

float calcExponentialFogAmount(const float density, const float power, const float fogCoord)
{
    return 1.0 - clamp(exp(-pow(density * fogCoord, power)), 0.0, 1.0);
}


float calcFogAmount(const float fogCoord)
{
	switch (fogParams.function)
	{
		case 0: // linear
			return calcLinearFogAmount(fogParams.start, fogParams.end, fogCoord);
	
		case 1: // exp
			return calcExponentialFogAmount(fogParams.density, 1, fogCoord);
	
		case 2: // exp2
			return calcExponentialFogAmount(fogParams.density, 2, fogCoord);
	}
	
	return 0.0;
}


#endif