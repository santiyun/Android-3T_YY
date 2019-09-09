package com.tttrtclive.live.yybeautfysdk;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tttrtclive.live.R;
import com.tttrtclive.live.yybeautfysdk.utils.BeautyUtil;
import com.tttrtclive.live.yybeautfysdk.utils.UIUtil;

import java.util.Vector;

public class BeautyEffectPanel {
    private static final String TAG = "BeautyEffectPanel";

    class BeautyOption {
        public String name;
        public int imageId;
        public int percent;
        public int min;
        public int max;
    }

    private Activity mActivity;
    private RelativeLayout mBeautyEffectView;
    private RelativeLayout mBeautyBasePanel;
    private RelativeLayout mBeautyEffectPanel;
    private Vector<BeautyOption> mBeautyOptions;
    private int mSelectBeautyOption = -1;
    private ImageView mBeautySelectBorder;
    private RelativeLayout mSeekPanel;
    private SeekBar mBeautySeek;
    private TextView mBeautySeekName;
    private TextView mBeautySeekValue;
    private BeautyUtil mBeautyUtil;
    private EffectManager.OnEnableEffectListener mOnEnableEffectListener;

    public BeautyEffectPanel(Activity activity, RelativeLayout beautyEffectView, BeautyUtil beautyUtil, EffectManager.OnEnableEffectListener onEnableEffectListener) {
        mActivity = activity;
        mBeautyEffectView = beautyEffectView;
        mBeautyUtil = beautyUtil;
        mOnEnableEffectListener = onEnableEffectListener;

        mBeautyBasePanel = new RelativeLayout(mActivity);
        mBeautyBasePanel.setId(View.generateViewId());
        mBeautyBasePanel.setBackgroundColor(0x80000000);
        mBeautyBasePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 200));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mBeautyEffectView.addView(mBeautyBasePanel, params);

        // scroll
        HorizontalScrollView scroll = new HorizontalScrollView(mActivity);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setHorizontalScrollBarEnabled(false);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 100));
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mBeautyBasePanel.addView(scroll, params);

        mBeautyEffectPanel = new RelativeLayout(mActivity);
        scroll.addView(mBeautyEffectPanel, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String[] titles = new String[] {
                "原图",
                "美肤",   // beautyfilter Opacity
                "美白",   // lookuptable Intensity
                "窄脸",   // ThinfaceIntensity
                "小脸",   // SmallfaceIntensity
                "瘦颧骨", // SquashedFaceIntensity
                "额高",   // ForeheadLiftingIntensity
                "额宽",   // WideForeheadIntensity
                "大眼",   // BigSmallEyeIntensity
                "眼距",   // EyesOffset
                "眼角",   // EyesRotationIntensity
                "瘦鼻",   // ThinNoseIntensity
                "长鼻",   // LongNoseIntensity
                "窄鼻梁", // ThinNoseBridgeIntensity
                "小嘴",   // ThinmouthIntensity
                "嘴位",   // MovemouthIntensity
                "下巴",   // ChinLiftingIntensity
        };
        int[] icons = new int[] {
            R.drawable.beauty_original,
            R.drawable.beauty_1,
            R.drawable.beauty_0,
            R.drawable.beauty_2,
            R.drawable.beauty_3,
            R.drawable.beauty_4,
            R.drawable.beauty_5,
            R.drawable.beauty_6,
            R.drawable.beauty_7,
            R.drawable.beauty_8,
            R.drawable.beauty_9,
            R.drawable.beauty_10,
            R.drawable.beauty_11,
            R.drawable.beauty_12,
            R.drawable.beauty_13,
            R.drawable.beauty_14,
            R.drawable.beauty_15,
        };

        mBeautyOptions = new Vector<>();

        int effectWidth = UIUtil.dip2px(mActivity, 50);

        // beauty options
        int id = 0;
        for (int i = 0; i < titles.length; ++i) {
            final int index = i;

            // label
            TextView text = new TextView(mActivity);
            text.setId(View.generateViewId());
            text.setTextColor(0xFFFFFFFF);
            text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            text.setGravity(Gravity.CENTER);
            text.setTextSize(12);
            text.setText(titles[i]);
            params = new RelativeLayout.LayoutParams(effectWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            if (i == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id);
            }
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.setMargins(UIUtil.dip2px(mActivity, 10), 0, UIUtil.dip2px(mActivity, 10), UIUtil.dip2px(mActivity, 10));
            mBeautyEffectPanel.addView(text, params);

            id = text.getId();

            // image
            ImageView image = new ImageView(mActivity);
            image.setId(View.generateViewId());
            image.setImageResource(icons[i]);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mSelectBeautyOption != index) {
                        onSelectBeautyOption(index);
                    }
                }
            });
            params = new RelativeLayout.LayoutParams(effectWidth, effectWidth);
            params.addRule(RelativeLayout.ALIGN_LEFT, id);
            params.addRule(RelativeLayout.ABOVE, id);
            params.setMargins(0, 0, 0, UIUtil.dip2px(mActivity, 10));
            mBeautyEffectPanel.addView(image, params);

            BeautyOption option = new BeautyOption();
            option.name = titles[i];
            option.imageId = image.getId();
            if (mBeautyUtil.isReady() && i >= 1) {
                option.min = mBeautyUtil.getBeautyOptionMinValue(i - 1);
                option.max = mBeautyUtil.getBeautyOptionMaxValue(i - 1);
                option.percent = (mBeautyUtil.getBeautyOptionValue(i - 1) - option.min) * 100 / (option.max - option.min);
            } else {
                option.min = 0;
                option.max = 100;
                option.percent = 50;
            }
            mBeautyOptions.add(option);
        }
    }

    private void onSelectBeautyOption(int index) {
        mSelectBeautyOption = index;

        if (mBeautySelectBorder == null) {
            mBeautySelectBorder = new ImageView(mActivity);
            mBeautySelectBorder.setImageResource(R.drawable.border);
        }
        mBeautyEffectPanel.removeView(mBeautySelectBorder);

        final BeautyOption option = mBeautyOptions.get(index);

        int effectWidth = UIUtil.dip2px(mActivity, 50);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(effectWidth, effectWidth);
        params.addRule(RelativeLayout.ALIGN_LEFT, option.imageId);
        params.addRule(RelativeLayout.ALIGN_TOP, option.imageId);
        mBeautyEffectPanel.addView(mBeautySelectBorder, params);

        if (mBeautySeek == null) {
            mSeekPanel = new RelativeLayout(mActivity);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 100));
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mBeautyBasePanel.addView(mSeekPanel, params);

            int seekWidth = (int) (UIUtil.screenWidth * 0.7f);
            int labelWidth = (UIUtil.screenWidth - seekWidth) / 2;

            mBeautySeek = new SeekBar(mActivity);
            mBeautySeek.setProgress(option.percent);
            mBeautySeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int percent, boolean b) {
                    if (mSelectBeautyOption >= 1) {
                        onSlideBeautyOption(mSelectBeautyOption, percent);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            params = new RelativeLayout.LayoutParams(seekWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSeekPanel.addView(mBeautySeek, params);

            // name
            mBeautySeekName = new TextView(mActivity);
            mBeautySeekName.setTextColor(0xFFFFFFFF);
            mBeautySeekName.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            mBeautySeekName.setGravity(Gravity.CENTER);
            mBeautySeekName.setTextSize(12);
            params = new RelativeLayout.LayoutParams(labelWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            mSeekPanel.addView(mBeautySeekName, params);

            // value
            mBeautySeekValue = new TextView(mActivity);
            mBeautySeekValue.setTextColor(0xFFFFFFFF);
            mBeautySeekValue.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            mBeautySeekValue.setGravity(Gravity.CENTER);
            mBeautySeekValue.setTextSize(12);
            params = new RelativeLayout.LayoutParams(labelWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            mSeekPanel.addView(mBeautySeekValue, params);
        }

        if (index == 0) {
            mSeekPanel.setVisibility(View.INVISIBLE);
            onEnableBeautyEffect(false);

            params = (RelativeLayout.LayoutParams) mBeautyBasePanel.getLayoutParams();
            params.height = UIUtil.dip2px(mActivity, 100);
            mBeautyBasePanel.setLayoutParams(params);
        } else {
            mSeekPanel.setVisibility(View.VISIBLE);
            mBeautySeekName.setText(option.name);
            mBeautySeek.setProgress(option.percent);

            onSlideBeautyOption(index, option.percent);
            onEnableBeautyEffect(true);

            params = (RelativeLayout.LayoutParams) mBeautyBasePanel.getLayoutParams();
            params.height = UIUtil.dip2px(mActivity, 200);
            mBeautyBasePanel.setLayoutParams(params);
        }
    }

    private void onSlideBeautyOption(int index, int percent) {
        final BeautyOption option = mBeautyOptions.get(index);
        option.percent = percent;

        int value = option.min + (option.max - option.min) * option.percent / 100;
        mBeautySeekValue.setText(String.valueOf(value));

        if (mBeautyUtil.isReady()) {
            mBeautyUtil.setBeautyOptionValue(index - 1, value);
        }
    }

    private void onEnableBeautyEffect(boolean enable) {
        if (mOnEnableEffectListener != null) {
            mOnEnableEffectListener.onEnableEffect(enable);
        }
    }

    public void setOnEnableEffectListener(EffectManager.OnEnableEffectListener listener) {
        mOnEnableEffectListener = listener;
    }

    public void show() {
        if (mSelectBeautyOption == -1) {
            onSelectBeautyOption(1);
        }
    }
}
