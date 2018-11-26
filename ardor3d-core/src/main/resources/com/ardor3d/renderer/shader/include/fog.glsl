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

float calcLinearFogAmount(const float start, const float end, const float fogCoord)
{
	return 1.0 - clamp((end - fogCoord) / (end - start), 0.0, 1.0);
}

float calcExponentialFogAmount(const float density, const float power, const float fogCoord)
{
    return 1.0 - clamp(exp(-pow(density * fogCoord, power)), 0.0, 1.0);
}


float calcFogAmount(FogParams p, const float fogCoord)
{
	switch (p.function)
	{
		case 0: // linear
			return calcLinearFogAmount(p.start, p.end, fogCoord);
	
		case 1: // exp
			return calcExponentialFogAmount(p.density, 1, fogCoord);
	
		case 2: // exp2
			return calcExponentialFogAmount(p.density, 2, fogCoord);
	}
	
	return 0;
}


#endif