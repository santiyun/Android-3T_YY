package com.tttrtclive.live.yybeautfysdk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.tttrtclive.live.R;
import com.tttrtclive.live.yybeautfysdk.utils.HttpUtil;
import com.tttrtclive.live.yybeautfysdk.utils.UIUtil;

import java.io.File;
import java.util.Vector;

public class StickerEffectPanel {
    private static final String TAG = "StickerEffectPanel";

    public static final int EFFECT_STICKER_COUNT_PER_ROW = 5;

    private Activity mActivity;
    private RelativeLayout mStickerEffectView;
    private RelativeLayout mStickerEffectPanel;
    private Vector<EffectManager.EffectTab> mStickerEffects;
    private int mSelectStickerTab = -1;
    private int mSelectStickerEffect = -1;
    private int mSelectStickerEffectTab = -1;
    private ImageView mStickerSelectBorder;
    private HttpUtil mHttpUtil;
    private EffectManager.OnSetEffectListener mOnSetStickerEffectListener;

    public StickerEffectPanel(
            Activity activity,
            RelativeLayout stickerEffectView,
            Vector<EffectManager.EffectTab> stickerEffects,
            HttpUtil httpUtil,
            EffectManager.OnSetEffectListener onSetStickerEffectListener) {
        mActivity = activity;
        mStickerEffectView = stickerEffectView;
        mStickerEffects = stickerEffects;
        mHttpUtil = httpUtil;
        mOnSetStickerEffectListener = onSetStickerEffectListener;

        RelativeLayout panel = new RelativeLayout(mActivity);
        panel.setId(View.generateViewId());
        panel.setBackgroundColor(0x80000000);
        panel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 200));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mStickerEffectView.addView(panel, params);

        // scroll
        ScrollView scroll = new ScrollView(mActivity);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        panel.addView(scroll, params);

        // panel
        mStickerEffectPanel = new RelativeLayout(mActivity);
        scroll.addView(mStickerEffectPanel, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        initStickerEffectTab(panel.getId());
    }

    private void initStickerEffectTab(int panelId) {
        // line
        View line = new View(mActivity);
        line.setId(View.generateViewId());
        line.setBackgroundColor(0x80FFFFFF);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 1);
        params.addRule(RelativeLayout.ABOVE, panelId);
        mStickerEffectView.addView(line, params);

        // tab
        RelativeLayout tab = new RelativeLayout(mActivity);
        tab.setBackgroundColor(0x80000000);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { }
        });
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, UIUtil.dip2px(mActivity, 46));
        params.addRule(RelativeLayout.ABOVE, line.getId());
        mStickerEffectView.addView(tab, params);

        // unselect button
        ImageView unselect = new ImageView(mActivity);
        unselect.setId(View.generateViewId());
        unselect.setImageResource(R.drawable.close);
        unselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelSelectStickerEffect();
            }
        });
        params = new RelativeLayout.LayoutParams(UIUtil.dip2px(mActivity, 36), UIUtil.dip2px(mActivity, 36));
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.setMargins(UIUtil.dip2px(mActivity, 5), 0, UIUtil.dip2px(mActivity, 5), 0);
        tab.addView(unselect, params);

        // separate
        View separate = new View(mActivity);
        separate.setId(View.generateViewId());
        separate.setBackgroundColor(0x80FFFFFF);
        params = new RelativeLayout.LayoutParams(1, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.RIGHT_OF, unselect.getId());
        params.setMargins(0, UIUtil.dip2px(mActivity, 5), 0, UIUtil.dip2px(mActivity, 5));
        tab.addView(separate, params);

        // tab scroll
        HorizontalScrollView scroll = new HorizontalScrollView(mActivity);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setHorizontalScrollBarEnabled(false);
        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.RIGHT_OF, separate.getId());
        tab.addView(scroll, params);

        RelativeLayout scrollLayout = new RelativeLayout(mActivity);
        scroll.addView(scrollLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int id = 0;
        for (int i = 0; i < mStickerEffects.size(); ++i) {
            final EffectManager.EffectTab effectTab = mStickerEffects.get(i);

            final int index = i;
            final ImageView item = new ImageView(mActivity);
            item.setId(View.generateViewId());
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mSelectStickerTab != index) {
                        onSelectStickerTab(index);
                    }
                }
            });
            params = new RelativeLayout.LayoutParams(UIUtil.dip2px(mActivity, 36), UIUtil.dip2px(mActivity, 36));
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            if (i == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id);
            }
            params.setMargins(UIUtil.dip2px(mActivity, 5), 0, UIUtil.dip2px(mActivity, 5), 0);
            scrollLayout.addView(item, params);

            effectTab.tabItem = item;

            effectTab.thumbTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    effectTab.thumbBitmap = bitmap;
                    item.setImageBitmap(bitmap);
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Log.e(TAG, "get icon image failed" + effectTab.thumb);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            if (effectTab.thumb.length() > 0) {
                Picasso.get().load(effectTab.thumb).into(effectTab.thumbTarget);
            } else {
                item.setImageResource(R.drawable.thumb);
            }

            effectTab.selectedThumbTarget = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    effectTab.selectedThumbBitmap = bitmap;
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Log.e(TAG, "get icon image failed" + effectTab.selectedThumb);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            if (effectTab.selectedThumb.length() > 0) {
                Picasso.get().load(effectTab.selectedThumb).into(effectTab.selectedThumbTarget);
            }

            id = item.getId();
        }
    }

    private void onSelectStickerTab(int index) {
        if (mSelectStickerTab >= 0) {
            final EffectManager.EffectTab tabOld = mStickerEffects.get(mSelectStickerTab);

            for (int i = 0; i < tabOld.effects.size(); ++i) {
                EffectManager.Effect effect = tabOld.effects.get(i);

                if (effect.loadView != null) {
                    effect.loadAnimation.cancel();
                }
            }
        }
        mStickerEffectPanel.removeAllViews();

        mSelectStickerTab = index;
        final EffectManager.EffectTab tab = mStickerEffects.get(mSelectStickerTab);

        for (int i = 0; i < mStickerEffects.size(); ++i) {
            if (mStickerEffects.get(i).thumbBitmap != null) {
                mStickerEffects.get(i).tabItem.setImageBitmap(mStickerEffects.get(i).thumbBitmap);
            }
        }
        if (tab.selectedThumbBitmap != null) {
            tab.tabItem.setImageBitmap(tab.selectedThumbBitmap);
        }

        int effectMargin = UIUtil.dip2px(mActivity, 10);
        int effectWidth = UIUtil.screenWidth / EFFECT_STICKER_COUNT_PER_ROW - effectMargin * 2;

        // add effects
        int id = 0;

        for (int i = 0; i < tab.effects.size(); ++i) {
            final int effectIndex = i;

            final EffectManager.Effect effect = tab.effects.get(i);

            ImageView item = new ImageView(mActivity);
            item.setId(View.generateViewId());
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onSelectStickerEffect(effectIndex);
                }
            });

            int size = effectWidth;
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size, size);
            if (i % EFFECT_STICKER_COUNT_PER_ROW == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id);
            }
            if (i / EFFECT_STICKER_COUNT_PER_ROW == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                params.addRule(RelativeLayout.BELOW, tab.effects.get(i - EFFECT_STICKER_COUNT_PER_ROW).viewId);
            }
            params.setMargins(effectMargin, effectMargin, effectMargin, effectMargin);
            mStickerEffectPanel.addView(item, params);

            if (effect.thumb.length() > 0 && effect.thumb.endsWith(".png")) {
                Picasso.get().load(effect.thumb).into(item);
            } else {
                item.setImageResource(R.drawable.thumb);
            }

            id = item.getId();
            effect.viewId = id;

            // download icon
            if (effect.loadView == null) {
                if (!new File(effect.path).exists()) {
                    ImageView down = new ImageView(mActivity);
                    down.setImageResource(R.drawable.download);
                    size = effectWidth / 3;
                    params = new RelativeLayout.LayoutParams(size, size);
                    params.addRule(RelativeLayout.ALIGN_RIGHT, effect.viewId);
                    params.addRule(RelativeLayout.ALIGN_BOTTOM, effect.viewId);
                    params.setMargins(effectMargin, effectMargin, effectMargin, effectMargin);
                    mStickerEffectPanel.addView(down, params);
                    effect.downView = down;
                }
            } else {
                params = new RelativeLayout.LayoutParams(size, size);
                params.addRule(RelativeLayout.ALIGN_RIGHT, effect.viewId);
                params.addRule(RelativeLayout.ALIGN_BOTTOM, effect.viewId);
                mStickerEffectPanel.addView(effect.loadBackView, params);

                params = new RelativeLayout.LayoutParams(size, size);
                params.addRule(RelativeLayout.ALIGN_RIGHT, effect.viewId);
                params.addRule(RelativeLayout.ALIGN_BOTTOM, effect.viewId);
                mStickerEffectPanel.addView(effect.loadView, params);

                effect.loadView.startAnimation(effect.loadAnimation);
            }
        }

        if (mSelectStickerEffect >= 0 && mSelectStickerEffectTab == mSelectStickerTab) {
            onSelectStickerEffect(mSelectStickerEffect);
        }
    }

    private void onSelectStickerEffect(int index) {
        Log.i(TAG, "onSelectEffect: " + index + " in tab: " + mSelectStickerTab);

        mSelectStickerEffectTab = mSelectStickerTab;
        mSelectStickerEffect = index;
        final EffectManager.EffectTab tab = mStickerEffects.get(mSelectStickerEffectTab);
        final EffectManager.Effect effect = tab.effects.get(mSelectStickerEffect);

        if (mStickerSelectBorder == null) {
            mStickerSelectBorder = new ImageView(mActivity);
            mStickerSelectBorder.setImageResource(R.drawable.border);
        }
        mStickerEffectPanel.removeView(mStickerSelectBorder);

        int effectMargin = UIUtil.dip2px(mActivity, 10);
        int effectWidth = UIUtil.screenWidth / EFFECT_STICKER_COUNT_PER_ROW - effectMargin * 2;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(effectWidth, effectWidth);
        params.addRule(RelativeLayout.ALIGN_LEFT, effect.viewId);
        params.addRule(RelativeLayout.ALIGN_TOP, effect.viewId);
        mStickerEffectPanel.addView(mStickerSelectBorder, params);

        if (effect.downView != null) {
            if (effect.loadView == null) {
                int size = effectWidth;

                ImageView bg = new ImageView(mActivity);
                bg.setBackgroundColor(0x80000000);
                params = new RelativeLayout.LayoutParams(size, size);
                params.addRule(RelativeLayout.ALIGN_RIGHT, effect.viewId);
                params.addRule(RelativeLayout.ALIGN_BOTTOM, effect.viewId);
                mStickerEffectPanel.addView(bg, params);
                effect.loadBackView = bg;

                ImageView load = new ImageView(mActivity);
                load.setImageResource(R.drawable.loading);
                params = new RelativeLayout.LayoutParams(size, size);
                params.addRule(RelativeLayout.ALIGN_RIGHT, effect.viewId);
                params.addRule(RelativeLayout.ALIGN_BOTTOM, effect.viewId);
                mStickerEffectPanel.addView(load, params);
                effect.loadView = load;

                mStickerEffectPanel.removeView(effect.downView);
                effect.downView = null;

                // rotation
                Animation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setDuration(1000);
                anim.setRepeatCount(Animation.INFINITE);
                anim.setInterpolator(new LinearInterpolator());
                effect.loadView.startAnimation(anim);
                effect.loadAnimation = anim;

                // begin download
                mHttpUtil.request(effect.url, effect.path, new HttpUtil.CallbackFile() {
                    @Override
                    public void onFailure() {
                        Log.e(TAG, "file download failed: " + effect.path);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "file download complete: " + effect.path);

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mStickerEffectPanel.removeView(effect.loadBackView);
                                effect.loadBackView = null;
                                mStickerEffectPanel.removeView(effect.loadView);
                                effect.loadView = null;
                                effect.loadAnimation.cancel();
                                effect.loadAnimation = null;

                                if (mSelectStickerEffect >= 0) {
                                    if (mStickerEffects.get(mSelectStickerEffectTab).effects.get(mSelectStickerEffect) == effect) {
                                        onSetStickerEffect(effect.path);
                                    }
                                }
                            }
                        });
                    }
                });
            }
        } else if (effect.loadView == null) {
            onSetStickerEffect(effect.path);
        }
    }

    private void onCancelSelectStickerEffect() {
        mSelectStickerEffectTab = -1;
        mSelectStickerEffect = -1;
        if (mStickerSelectBorder != null) {
            mStickerEffectPanel.removeView(mStickerSelectBorder);
        }

        onSetStickerEffect("");
    }

    private void onSetStickerEffect(String path) {
        Log.i(TAG, "onSetStickerEffect: " + path);

        if (mOnSetStickerEffectListener != null) {
            mOnSetStickerEffectListener.onSetEffect(path);
        }
    }

    public void setOnSetStickerEffectListener(EffectManager.OnSetEffectListener listener) {
        mOnSetStickerEffectListener = listener;
    }

    public void show() {
        if (mStickerEffects.size() > 0) {
            if (mSelectStickerTab == -1) {
                onSelectStickerTab(0);
            } else {
                onSelectStickerTab(mSelectStickerTab);
            }
        }
    }
}
