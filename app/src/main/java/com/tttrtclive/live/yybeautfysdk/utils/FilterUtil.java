package com.tttrtclive.live.yybeautfysdk.utils;

import android.util.Log;

import com.orangefilter.OrangeFilter;

public class FilterUtil {
    private static final String TAG = "FilterUtil";

    private int mContext = 0;
    private int mEffect = 0;
    private OrangeFilter.OF_EffectInfo mInfo;
    private boolean mReady = false;
    private OnFilterChangeListener mOnFilterChangeListener;

    public FilterUtil() {

    }

    public void setEffect(int context, int effect) {
        mContext = context;
        mEffect = effect;

        if (mEffect != 0) {
            mInfo = new OrangeFilter.OF_EffectInfo();
            OrangeFilter.getEffectInfo(mContext, mEffect, mInfo);

            if (mInfo.filterCount != 1) {
                Log.e(TAG, "effect info error with filter count: " + mInfo.filterCount);
            }

            mReady = true;
        } else {
            mInfo = null;
            mReady = false;
        }

        if (mOnFilterChangeListener != null) {
            mOnFilterChangeListener.onFilterChange();
        }
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

    private OrangeFilter.OF_Param getFilterParam() {
        int filter = mInfo.filterList[0];
        return OrangeFilter.getFilterParamData(mContext, filter, "Intensity");
    }

    public int getFilterIntensity() {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam();
        return (int) (param.val * 100);
    }

    public void setFilterIntensity(int value) {
        OrangeFilter.OF_Paramf param = (OrangeFilter.OF_Paramf) getFilterParam();
        param.val = value / 100.0f;

        int filter = mInfo.filterList[0];
        OrangeFilter.setFilterParamData(mContext, filter, param.name, param);
    }

    public void setOnFilterChangeListener(OnFilterChangeListener listener) {
        mOnFilterChangeListener = listener;
    }

    public interface OnFilterChangeListener {
        void onFilterChange();
    }
}
