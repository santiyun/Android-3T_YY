package com.tttrtclive.live.yybeautfysdk.utils;

import android.util.Log;

import com.orangefilter.OrangeFilter;

public class BeautyUtil {
    private static final String TAG = "BeautyUtil";

    private static final int FILTER_INDEX_WHITE = 0;
    private static final int FILTER_INDEX_BEAUTY = 1;
    private static final int FILTER_INDEX_LEVELS = 2;
    private static final int FILTER_INDEX_FACELIFTING = 3;
    private static final int FILTER_COUNT = 4;

    public static final int BEAUTY_OPTION_SKIN = 0;
    public static final int BEAUTY_OPTION_WHITE = 1;
    public static final int BEAUTY_OPTION_THIN_FACE = 2;
    public static final int BEAUTY_OPTION_SMALL_FACE = 3;
    public static final int BEAUTY_OPTION_SQUASH_FACE = 4;
    public static final int BEAUTY_OPTION_FOREHEAD_LIFTING = 5;
    public static final int BEAUTY_OPTION_WIDE_FOREHEAD = 6;
    public static final int BEAUTY_OPTION_BIG_EYE = 7;
    public static final int BEAUTY_OPTION_EYE_DISTANCE = 8;
    public static final int BEAUTY_OPTION_EYE_ROTATE = 9;
    public static final int BEAUTY_OPTION_THIN_NOSE = 10;
    public static final int BEAUTY_OPTION_LONG_NOSE = 11;
    public static final int BEAUTY_OPTION_THIN_NOSE_BRIDGE = 12;
    public static final int BEAUTY_OPTION_THIN_MOUTH = 13;
    public static final int BEAUTY_OPTION_MOVE_MOUTH = 14;
    public static final int BEAUTY_OPTION_CHIN_LIFTING = 15;

    private static final String[] BEAUTY_OPTION_NAMES = new String[] {
        "Opacity",
        "Intensity",
        "ThinfaceIntensity",
        "SmallfaceIntensity",
        "SquashedFaceIntensity",
        "ForeheadLiftingIntensity",
        "WideForeheadIntensity",
        "BigSmallEyeIntensity",
        "EyesOffset",
        "EyesRotationIntensity",
        "ThinNoseIntensity",
        "LongNoseIntensity",
        "ThinNoseBridgeIntensity",
        "ThinmouthIntensity",
        "MovemouthIntensity",
        "ChinLiftingIntensity",
    };

    private int mContext = 0;
    private int mEffect = 0;
    private OrangeFilter.OF_EffectInfo mInfo;
    private boolean mReady = false;

    public BeautyUtil() {

    }

    public void setEffect(int context, int effect) {
        mContext = context;
        mEffect = effect;

        mInfo = new OrangeFilter.OF_EffectInfo();
        OrangeFilter.getEffectInfo(mContext, mEffect, mInfo);

        if (mInfo.filterCount != FILTER_COUNT) {
            Log.e(TAG, "effect info error with filter count: " + mInfo.filterCount);
        }

        mReady = true;
    }

    public void clearEffect() {
        mContext = 0;
        mEffect = 0;
        mInfo = null;
        mReady = false;
    }

    public boolean isReady() {
        return mReady;
    }

    private int getFilterIndex(int option) {
        if (option == BEAUTY_OPTION_WHITE) {
            return FILTER_INDEX_WHITE;
        } else if (option == BEAUTY_OPTION_SKIN) {
            return FILTER_INDEX_BEAUTY;
        } else {
            return FILTER_INDEX_FACELIFTING;
        }
    }

    private OrangeFilter.OF_Param getFilterParam(int option) {
        int filter = mInfo.filterList[getFilterIndex(option)];
        String name = BEAUTY_OPTION_NAMES[option];
        return OrangeFilter.getFilterParamData(mContext, filter, name);
    }

    public int getBeautyOptionMinValue(int option) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam(option);
        return (int) (param.minVal / (param.maxVal - param.minVal) * 100);
    }

    public int getBeautyOptionMaxValue(int option) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam(option);
        return (int) (param.maxVal / (param.maxVal - param.minVal) * 100);
    }

    public int getBeautyOptionDefaultValue(int option) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam(option);
        return (int) (param.defVal / (param.maxVal - param.minVal) * 100);
    }

    public int getBeautyOptionValue(int option) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam(option);
        return (int) (param.val / (param.maxVal - param.minVal) * 100);
    }

    public void setBeautyOptionValue(int option, int value) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam(option);
        param.val = value / 100.0f * (param.maxVal - param.minVal);

        int filter = mInfo.filterList[getFilterIndex(option)];
        OrangeFilter.setFilterParamData(mContext, filter, param.name, param);
    }
}
