/*
* cgeContrastAdjust.cpp
*
*  Created on: 2013-12-26
*      Author: Wang Yang
*/

#include "cgePosterizeAdjust.h"

const static char* const s_fshContrast = CGE_SHADER_STRING_PRECISION_H
(
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform highp float colorLevels;

void main()
{
    highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
    gl_FragColor = floor((textureColor * colorLevels) + vec4(0.5)) / colorLevels;
}

);

namespace CGE
{
	CGEConstString CGEPosterizeFilter::paramName = "colorLevels";

	bool CGEPosterizeFilter::init()
	{
		if(initShadersFromString(g_vshDefaultWithoutTexCoord, s_fshContrast))
		{
			return true;
		}
		return false;
	}

	void CGEPosterizeFilter::setIntensity(float value)
	{
		m_program.bind();
		m_program.sendUniformf(paramName, value);
	}
}