package com.tttrtclive.live.yybeautfysdk;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tttrtclive.live.R;
import com.tttrtclive.live.yybeautfysdk.utils.FilterUtil;
import com.tttrtclive.live.yybeautfysdk.utils.UIUtil;

import java.io.File;
import java.util.Vector;

public class FilterEffectPanel {
    private static final String TAG = "FilterEffectPanel";

    class Filter {
        public String name;
        public int imageId;
        public int percent;
        public int min;
        public int max;
        public EffectManager.Effect effect;
    }

    private Activity mActivity;
    private RelativeLayout mFilterEffectView;
    private RelativeLayout mBeautyBasePanel;
    private RelativeLayout mFilterEffectPanel;
    private Vector<EffectManager.EffectTab> mFilterEffects;
    private int mSelectFilter = -1;
    private Vector<Filter> mFilters;
    private ImageView mFilterSelectBorder;
    private RelativeLayout mSeekPanel;
    private SeekBar mFilterSeek;
    private TextView mFilterSeekName;
    private TextView mFilterSeekValue;
    private EffectManager.OnSetEffectListener mOnSetFilterEffectListener;
    private FilterUtil mFilterUtil;

    public FilterEffectPanel(
            Activity activity,
            RelativeLayout filterEffectView,
            Vector<EffectManager.EffectTab> filterEffects,
            EffectManager.OnSetEffectListener onSetFilterEffectListener,
            FilterUtil filterUtil) {
        mActivity = activity;
        mFilterEffectView = filterEffectView;
        mFilterEffects = filterEffects;
        mOnSetFilterEffectListener = onSetFilterEffectListener;
        mFilterUtil = filterUtil;

        mBeautyBasePanel = new RelativeLayout(mActivity);
        mBeautyBasePanel.setId(View.generateViewId());
        mBeautyBasePanel.setBackgroundColor(0x80000000);
        mBeautyBasePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 200));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mFilterEffectView.addView(mBeautyBasePanel, params);

        // scroll
        HorizontalScrollView scroll = new HorizontalScrollView(mActivity);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setHorizontalScrollBarEnabled(false);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 100));
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mBeautyBasePanel.addView(scroll, params);

        mFilterEffectPanel = new RelativeLayout(mActivity);
        scroll.addView(mFilterEffectPanel, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // filters
        mFilters = new Vector<>();

        int effectWidth = UIUtil.dip2px(mActivity, 50);

        if (mFilterEffects.size() >= 1) {
            Vector<EffectManager.Effect> effects = mFilterEffects.get(0).effects;

            int id = 0;
            for (int i = 0; i < effects.size() + 1; ++i) {
                final int index = i;
                EffectManager.Effect effect = null;
                if (i >= 1) {
                    effect = effects.get(i - 1);
                }

                // label
                TextView text = new TextView(mActivity);
                text.setId(View.generateViewId());
                text.setTextColor(0xFFFFFFFF);
                text.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                text.setGravity(Gravity.CENTER);
                text.setTextSize(12);
                if (i == 0) {
                    text.setText("原图");
                } else {
                    text.setText(effect.name);
                }
                params = new RelativeLayout.LayoutParams(effectWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
                if (i == 0) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                } else {
                    params.addRule(RelativeLayout.RIGHT_OF, id);
                }
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.setMargins(UIUtil.dip2px(mActivity, 10), 0, UIUtil.dip2px(mActivity, 10), UIUtil.dip2px(mActivity, 10));
                mFilterEffectPanel.addView(text, params);

                id = text.getId();

                // image
                ImageView image = new ImageView(mActivity);
                image.setId(View.generateViewId());
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mSelectFilter != index) {
                            onSelectFilter(index);
                        }
                    }
                });
                params = new RelativeLayout.LayoutParams(effectWidth, effectWidth);
                params.addRule(RelativeLayout.ALIGN_LEFT, id);
                params.addRule(RelativeLayout.ABOVE, id);
                params.setMargins(0, 0, 0, UIUtil.dip2px(mActivity, 10));
                mFilterEffectPanel.addView(image, params);

                if (i == 0) {
                    image.setImageResource(R.drawable.beauty_original);
                } else {
                    Picasso.get().load(effect.thumb).into(image);
                }

                Filter filter = new Filter();
                if (i >= 1) {
                    filter.name = effect.name;
                }
                filter.imageId = image.getId();
                filter.min = 0;
                filter.max = 100;
                filter.percent = 100;
                filter.effect = effect;
                mFilters.add(filter);
            }
        }
    }

    private void onSelectFilter(int index) {
        mSelectFilter = index;

        if (mFilterSelectBorder == null) {
            mFilterSelectBorder = new ImageView(mActivity);
            mFilterSelectBorder.setImageResource(R.drawable.border);
        }
        mFilterEffectPanel.removeView(mFilterSelectBorder);

        final Filter filter = mFilters.get(index);

        int effectWidth = UIUtil.dip2px(mActivity, 50);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(effectWidth, effectWidth);
        params.addRule(RelativeLayout.ALIGN_LEFT, filter.imageId);
        params.addRule(RelativeLayout.ALIGN_TOP, filter.imageId);
        mFilterEffectPanel.addView(mFilterSelectBorder, params);

        if (mFilterSeek == null) {
            RelativeLayout panel = (RelativeLayout) mFilterEffectPanel.getParent().getParent();

            mSeekPanel = new RelativeLayout(mActivity);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 100));
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            panel.addView(mSeekPanel, params);

            int seekWidth = (int) (UIUtil.screenWidth * 0.7f);
            int labelWidth = (UIUtil.screenWidth - seekWidth) / 2;

            mFilterSeek = new SeekBar(mActivity);
            mFilterSeek.setProgress(filter.percent);
            mFilterSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int percent, boolean b) {
                    if (mSelectFilter >= 1) {
                        onSlideFilter(mSelectFilter, percent);
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
            mSeekPanel.addView(mFilterSeek, params);

            // name
            mFilterSeekName = new TextView(mActivity);
            mFilterSeekName.setTextColor(0xFFFFFFFF);
            mFilterSeekName.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            mFilterSeekName.setGravity(Gravity.CENTER);
            mFilterSeekName.setTextSize(12);
            params = new RelativeLayout.LayoutParams(labelWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            mSeekPanel.addView(mFilterSeekName, params);

            // value
            mFilterSeekValue = new TextView(mActivity);
            mFilterSeekValue.setTextColor(0xFFFFFFFF);
            mFilterSeekValue.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            mFilterSeekValue.setGravity(Gravity.CENTER);
            mFilterSeekValue.setTextSize(12);
            params = new RelativeLayout.LayoutParams(labelWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            mSeekPanel.addView(mFilterSeekValue, params);
        }

        if (index == 0) {
            mSeekPanel.setVisibility(View.INVISIBLE);

            onSetFilterEffect("");

            params = (RelativeLayout.LayoutParams) mBeautyBasePanel.getLayoutParams();
            params.height = UIUtil.dip2px(mActivity, 100);
            mBeautyBasePanel.setLayoutParams(params);
        } else {
            mSeekPanel.setVisibility(View.VISIBLE);
            mFilterSeekName.setText(filter.name);
            mFilterSeek.setProgress(filter.percent);

            onSlideFilter(index, filter.percent);

            if (new File(filter.effect.path).exists()) {
                onSetFilterEffect(filter.effect.path);
            } else {
                Log.e(TAG, "onSelectFilter effect file not exist: " + filter.effect.path);
            }

            params = (RelativeLayout.LayoutParams) mBeautyBasePanel.getLayoutParams();
            params.height = UIUtil.dip2px(mActivity, 200);
            mBeautyBasePanel.setLayoutParams(params);
        }
    }

    private void onSlideFilter(int index, int percent) {
        final Filter filter = mFilters.get(index);
        filter.percent = percent;

        int value = filter.percent;
        mFilterSeekValue.setText(String.valueOf(value));

        if (mFilterUtil.isReady()) {
            mFilterUtil.setFilterIntensity(value);
        }
    }

    private void onSetFilterEffect(String path) {
        Log.i(TAG, "onSetFilterEffect: " + path);

        if (mOnSetFilterEffectListener != null) {
            mOnSetFilterEffectListener.onSetEffect(path);
        }
    }

    public void setOnSetFilterEffectListener(EffectManager.OnSetEffectListener listener) {
        mOnSetFilterEffectListener = listener;
    }

    public void show() {
        if (mSelectFilter == -1) {
            onSelectFilter(0);

            mFilterUtil.setOnFilterChangeListener(new FilterUtil.OnFilterChangeListener() {
                @Override
                public void onFilterChange() {
                    final Filter filter = mFilters.get(mSelectFilter);
                    if (mFilterUtil.isReady()) {
                        mFilterUtil.setFilterIntensity(filter.percent);
                    }
                }
            });
        }
    }
}
