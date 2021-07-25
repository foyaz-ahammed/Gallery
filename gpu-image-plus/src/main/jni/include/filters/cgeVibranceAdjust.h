/*
 * cgeContrastAdjust.h
 *
 *  Created on: 2013-12-26
 *      Author: Wang Yang
 */

#ifndef _CGEVIBRANCE_ADJUST_H_
#define _CGEVIBRANCE_ADJUST_H_

#include "cgeGLFunctions.h"
#include "cgeImageFilter.h"
#include "cgeImageHandler.h"

namespace CGE
{
	class CGEVibranceFilter : public CGEImageFilterInterface
	{
	public:
		CGEVibranceFilter(){}
		~CGEVibranceFilter(){}

		void setIntensity(float value); //range > 0, and 1 for origin

		bool init();

	protected:
		static CGEConstString paramName;
	};
}

#endif
