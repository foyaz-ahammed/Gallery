/*
* cgeContrastAdjust.cpp
*
*  Created on: 2013-12-26
*      Author: Wang Yang
*/

#include "cgeVibranceAdjust.h"

const static char* const s_fshContrast = CGE_SHADER_STRING_PRECISION_H
(
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform lowp float vibrance;

void main()
{
	lowp vec4 color = texture2D(inputImageTexture, textureCoordinate);
	lowp float average = (color.r + color.g + color.b) / 3.0;
	lowp float mx = max(color.r, max(color.g, color.b));
	lowp float amt = (mx - average) * (-vibrance * 3.0);
	color.rgb = mix(color.rgb, vec3(mx), amt);
	gl_FragColor = color;
}

);

namespace CGE
{
	CGEConstString CGEVibranceFilter::paramName = "vibrance";

	bool CGEVibranceFilter::init()
	{
		if(initShadersFromString(g_vshDefaultWithoutTexCoord, s_fshContrast))
		{
			return true;
		}
		return false;
	}

	void CGEVibranceFilter::setIntensity(float value)
	{
		m_program.bind();
		m_program.sendUniformf(paramName, value);
	}
}