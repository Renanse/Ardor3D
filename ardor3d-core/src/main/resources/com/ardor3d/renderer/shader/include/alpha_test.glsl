#ifndef ALPHA_TEST_INC
#define ALPHA_TEST_INC

// Alpha test values:
// 0 - ALWAYS
// 1 - NEVER
// 2 - EQUAL_TO
// 3 - NOT_EQUAL_TO
// 4 - LESS_THAN
// 5 - LESS_THAN_OR_EQUAL_TO
// 6 - GREATER_THAN
// 7 - GREATER_THAN_OR_EQUAL_TO
uniform int alphaTestType;

uniform float alphaReference;

bool applyAlphaTest(const vec4 color)
{
    switch(alphaTestType) {
        default:
        case 0: // ALWAYS
            return true;
        case 1: // NEVER
            return false;
        case 2: // EQUAL_TO
            return color.a == alphaReference;
        case 3: // NOT_EQUAL_TO
            return color.a != alphaReference;
        case 4: // LESS_THAN
            return color.a < alphaReference;
        case 5: // LESS_THAN_OR_EQUAL_TO
            return color.a <= alphaReference;
        case 6: // GREATER_THAN
            return color.a > alphaReference;
        case 7: // GREATER_THAN_OR_EQUAL_TO
            return color.a >= alphaReference;
    }
}

#endif