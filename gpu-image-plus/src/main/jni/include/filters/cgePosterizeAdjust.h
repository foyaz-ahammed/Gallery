/*
 * cgeContrastAdjust.h
 *
 *  Created on: 2013-12-26
 *      Author: Wang Yang
 */

#ifndef _CGEPOSTERIZE_ADJUST_H_
#define _CGEPOSTERIZE_ADJUST_H_

#include "cgeGLFunctions.h"
#include "cgeImageFilter.h"
#include "cgeImageHandler.h"

namespace CGE
{
	class CGEPosterizeFilter : public CGEImageFilterInterface
	{
	public:
		CGEPosterizeFilter(){}
		~CGEPosterizeFilter(){}

		void setIntensity(float value); //range > 0, and 1 for origin

		bool init();

	protected:
		static CGEConstString paramName;
	};
}

#endif
