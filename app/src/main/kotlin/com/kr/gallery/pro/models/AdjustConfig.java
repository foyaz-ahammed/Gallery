package com.kr.gallery.pro.models;

public class AdjustConfig {
    public float intensity, slierIntensity = 0.5f;
    public float minValue, originvalue, maxValue;

    public int mNameResID;
    public int mIconResID;
    public String mConfigStr;


    public String mTypeName;
    public String mFuncName;                // eg: shadowhighlight
    public String mSubFuncName;             // eg: shadow or highlight

    public AdjustConfig(
            int nameResID,
            int iconResId,
            String typeName,
            String subFuncName,
            float _minValue, float _maxValue, float _originValue) {

        mNameResID = nameResID;
        mIconResID = iconResId;

        mTypeName = typeName;
        mSubFuncName = subFuncName;
        mFuncName = getConfigName(mSubFuncName);
        mConfigStr = "@" + mTypeName + " " + mFuncName + " " + _originValue;

        minValue = _minValue;
        originvalue = _originValue;
        maxValue = _maxValue;
        intensity = _originValue;
    }

    protected float calcIntensity(float _intensity) {
        float result;
        if (_intensity <= 0.0f) {
            result = minValue;
        } else if (_intensity >= 1.0f) {
            result = maxValue;
        } else if (_intensity <= 0.5f) {
            result = minValue + (originvalue - minValue) * _intensity * 2.0f;
        } else {
            result = maxValue + (originvalue - maxValue) * (1.0f - _intensity) * 2.0f;
        }
        return result;
    }

    //_intensity range: [0.0, 1.0], 0.5 for the origin.
    public void updateIntensity(float _intensity) {
        slierIntensity = _intensity;
        intensity = calcIntensity(_intensity);
        updateConfigStr();
    }

    public String getConfigName(String typeName) {
        switch (typeName) {
            case "shadow":
            case "highlight":
                return "shadowhighlight";
        }
        return typeName;
    }

    public void updateConfigStr() {
        final String intensityStr;

        switch (mSubFuncName) {
            case "shadow":
                intensityStr = (int)intensity + " 0";
                break;
            case "highlight":
                intensityStr = "0 " + (int)intensity;
                break;
            case "edge":
                intensityStr = "1 " + (int)intensity;
                break;
            case "whitebalance":
                intensityStr = intensity + " 1";
                break;
            default:
                intensityStr = "" + intensity;
                break;
        }

        mConfigStr = "@" + mTypeName + " " + mFuncName + " " + intensityStr;
    }
}
