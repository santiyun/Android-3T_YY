package com.tttrtclive.live.yybeautfysdk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Target;
import com.tttrtclive.live.yybeautfysdk.utils.BeautyUtil;
import com.tttrtclive.live.yybeautfysdk.utils.FilterUtil;
import com.tttrtclive.live.yybeautfysdk.utils.HttpUtil;
import com.tttrtclive.live.yybeautfysdk.utils.UIUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;
import java.util.Vector;

public class EffectManager {
    private static final String TAG = "EffectManager";

    private static final int EFFECT_TYPE_BEAUTY = 0;
    private static final int EFFECT_TYPE_FILTER = 1;
    private static final int EFFECT_TYPE_STICKER = 2;
    private static final int EFFECT_TYPE_AVATAR = 3;

    class Effect {
        public String name;
        public String md5;
        public String thumb;
        public String url;
        public String path;
        public int viewId;
        public ImageView downView;
        public ImageView loadBackView;
        public ImageView loadView;
        public Animation loadAnimation;
    }

    class EffectTab {
        public String name;
        public String thumb;
        public String selectedThumb;
        public int type;
        public ImageView tabItem;
        public Target thumbTarget;
        public Target selectedThumbTarget;
        public Bitmap thumbBitmap;
        public Bitmap selectedThumbBitmap;
        public Vector<Effect> effects;
    }

    private Activity mActivity;
    private RelativeLayout mLayout;
    private RelativeLayout mBottomTab;
    private BeautyUtil mBeautyUtil;
    private FilterUtil mFilterUtil;
    private HttpUtil mHttpUtil;
    private boolean mInitUI = false;
    private Vector<EffectTab> mBeautyEffects;
    private Vector<EffectTab> mFilterEffects;
    private Vector<EffectTab> mStickerEffects;
    private Vector<EffectTab> mAvatarEffects;
    private RelativeLayout mBeautyEffectView;
    private BeautyEffectPanel mBeautyEffectPanel;
    private RelativeLayout mFilterEffectView;
    private FilterEffectPanel mFilterEffectPanel;
    private RelativeLayout mStickerEffectView;
    private StickerEffectPanel mStickerEffectPanel;
    private OnSetEffectListener mOnSetBeautyEffectListener;
    private OnSetEffectListener mOnSetFilterEffectListener;
    private OnSetEffectListener mOnSetStickerEffectListener;
    private OnEnableEffectListener mOnEnableEffectListener;

    public EffectManager(Activity activity, RelativeLayout layout, BeautyUtil beautyUtil, FilterUtil filterUtil) {
        mActivity = activity;
        mLayout = layout;
        mBeautyUtil = beautyUtil;
        mFilterUtil = filterUtil;
        mHttpUtil = new HttpUtil();
        UIUtil.screenWidth = layout.getWidth();
        UIUtil.screenHeight = layout.getHeight();
    }

    private void showToast(final String text) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity,text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void requestEffectList() {
        final String channel = "9c4fc48365";
        final String version = "1.0.0";
        final int os = 2;
        String url = String.format(Locale.getDefault(), "http://ovotest.yy.com/asset/common?channel=%s&version=%s&os=%d", channel, version, os);
        //String url = "http://ovotest.yy.com/expression/common?channel=032f64b9d2&version=1.0.0&os=2";

        mHttpUtil.request(url, new HttpUtil.CallbackJSON() {
            @Override
            public void onFailure() {
                showToast("request effect info failed");
            }

            @Override
            public void onComplete(JSONObject json) {
                try {
                    int code = json.optInt("code", -1);
                    if (code == 0) {
                        JSONArray data = json.getJSONArray("data");
                        if (data != null) {
                            mBeautyEffects = new Vector<>();
                            mFilterEffects = new Vector<>();
                            mStickerEffects = new Vector<>();
                            mAvatarEffects = new Vector<>();

                            for (int i = 0; i < data.length(); ++i) {
                                JSONObject datai = data.getJSONObject(i);

                                final EffectTab tab = new EffectTab();
                                tab.name = datai.getString("name");
                                tab.thumb = datai.getString("thumb");
                                tab.selectedThumb = datai.getString("selectedThumb");
                                tab.effects = new Vector<>();
                                tab.type = -1;

                                String ext = datai.getString("groupExpandJson");
                                if (ext.length() > 0) {
                                    String type = new JSONObject(ext).getString("type");
                                    if (type.equals("Beauty")) {
                                        tab.type = EFFECT_TYPE_BEAUTY;
                                    } else if (type.equals("Filter")) {
                                        tab.type = EFFECT_TYPE_FILTER;
                                    } else if (type.equals("Sticker")) {
                                        tab.type = EFFECT_TYPE_STICKER;
                                    } else if (type.equals("Avatar")) {
                                        tab.type = EFFECT_TYPE_AVATAR;
                                    }
                                }

                                JSONArray icons = datai.getJSONArray("icons");
                                //JSONArray icons = datai.getJSONArray("emoticons");

                                for (int j = 0; j < icons.length(); ++j) {
                                    JSONObject effect = icons.getJSONObject(j);

                                    String effectName = effect.getString("name");
                                    String md5 = effect.getString("md5");
                                    String effectThumb = effect.getString("thumb");
                                    String url = effect.getString("url");

                                    Log.i(TAG, "effect name: " + effectName + " url: " + url);

                                    final String path = mActivity.getFilesDir().getPath()
                                            + "/orangefilter/effects/"
                                            + md5 + ".zip";

                                    final Effect e = new Effect();
                                    e.name = effectName;
                                    e.md5 = md5;
                                    e.thumb = effectThumb;
                                    e.url = url;
                                    e.path = path;

                                    tab.effects.add(e);
                                }

                                for (int j = 0; j < tab.effects.size(); ++j) {
                                    final int index = j;
                                    final Effect e = tab.effects.get(j);

                                    // auto download beauty and filters
                                    if (tab.type == EFFECT_TYPE_BEAUTY || tab.type == EFFECT_TYPE_FILTER) {
                                        if (!new File(e.path).exists()) {
                                            mHttpUtil.request(e.url, e.path, new HttpUtil.CallbackFile() {
                                                @Override
                                                public void onFailure() {
                                                    showToast("file download failed: " + e.path);
                                                }

                                                @Override
                                                public void onComplete() {
                                                    Log.i(TAG, "file download complete: " + e.path);

                                                    if (index == 0) {
                                                        if (tab.type == EFFECT_TYPE_BEAUTY) {
                                                            onSetBeautyEffect(e.path);
                                                        } else if (tab.type == EFFECT_TYPE_FILTER) {
                                                            //onSetFilterEffect(e.path);
                                                        }
                                                    }
                                                }
                                            });
                                        } else {
                                            if (index == 0) {
                                                if (tab.type == EFFECT_TYPE_BEAUTY) {
                                                    onSetBeautyEffect(e.path);
                                                } else if (tab.type == EFFECT_TYPE_FILTER) {
                                                    //onSetFilterEffect(e.path);
                                                }
                                            }
                                        }
                                    }
                                }

                                if (tab.type == EFFECT_TYPE_BEAUTY) {
                                    mBeautyEffects.add(tab);
                                } else if (tab.type == EFFECT_TYPE_FILTER) {
                                    mFilterEffects.add(tab);
                                } else if (tab.type == EFFECT_TYPE_STICKER) {
                                    mStickerEffects.add(tab);
                                } else if (tab.type == EFFECT_TYPE_AVATAR) {
                                    mAvatarEffects.add(tab);
                                }
                            }
                        }
                    } else {
                        showToast("effects response code error: " + code);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void initUI() {
        if (mInitUI) {
            return;
        }
        mInitUI = true;

        Log.i(TAG, "initUI, layout width: " + mLayout.getWidth());

        // effect view
        mBeautyEffectView = new RelativeLayout(mActivity);
        mBeautyEffectView.setBackgroundColor(0);
        mBeautyEffectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.removeView(mBeautyEffectView);
                mBottomTab.setVisibility(View.VISIBLE);
            }
        });

        mFilterEffectView = new RelativeLayout(mActivity);
        mFilterEffectView.setBackgroundColor(0);
        mFilterEffectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.removeView(mFilterEffectView);
                mBottomTab.setVisibility(View.VISIBLE);
            }
        });

        mStickerEffectView = new RelativeLayout(mActivity);
        mStickerEffectView.setBackgroundColor(0);
        mStickerEffectView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.removeView(mStickerEffectView);
                mBottomTab.setVisibility(View.VISIBLE);
            }
        });

        mBeautyEffectPanel = new BeautyEffectPanel(mActivity, mBeautyEffectView, mBeautyUtil, mOnEnableEffectListener);
        mFilterEffectPanel = new FilterEffectPanel(mActivity, mFilterEffectView, mFilterEffects, mOnSetFilterEffectListener, mFilterUtil);
        mStickerEffectPanel = new StickerEffectPanel(mActivity, mStickerEffectView, mStickerEffects, mHttpUtil, mOnSetStickerEffectListener);

        // tab buttons
        final int panelWidth = mLayout.getWidth();

        mBottomTab = new RelativeLayout(mActivity);
        mBottomTab.setBackgroundColor(0x80000000);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(panelWidth, UIUtil.dip2px(mActivity, 45));
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mLayout.addView(mBottomTab, params);

        //"美颜", "滤镜", "贴纸", "虚拟角色"
        final String[] titles = new String[] {
            "美颜", "滤镜", "贴纸", ""
        };
        final View.OnClickListener[] clicks = new View.OnClickListener[4];
        clicks[0] = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.addView(mBeautyEffectView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                mBeautyEffectPanel.show();
                mBottomTab.setVisibility(View.INVISIBLE);
            }
        };
        clicks[1] = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.addView(mFilterEffectView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                mFilterEffectPanel.show();
                mBottomTab.setVisibility(View.INVISIBLE);
            }
        };
        clicks[2] = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayout.addView(mStickerEffectView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

                mStickerEffectPanel.show();
                mBottomTab.setVisibility(View.INVISIBLE);
            }
        };

        int id = 0;
        for (int i = 0; i < 4; ++i) {
            TextView button = new TextView(mActivity);
            button.setId(View.generateViewId());
            button.setTextColor(0xFFFFFFFF);
            button.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
            button.setGravity(Gravity.CENTER);
            button.setTextSize(16);
            button.setText(titles[i]);
            button.setOnClickListener(clicks[i]);
            params = new RelativeLayout.LayoutParams(panelWidth / 4, RelativeLayout.LayoutParams.MATCH_PARENT);
            if (i == 0) {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id);
            }
            mBottomTab.addView(button, params);

            id = button.getId();
        }
    }

    private void onSetBeautyEffect(String path) {
        Log.i(TAG, "onSetBeautyEffect: " + path);

        if (mOnSetBeautyEffectListener != null) {
            mOnSetBeautyEffectListener.onSetEffect(path);
        }
    }

    private void onSetFilterEffect(String path) {
        Log.i(TAG, "onSetFilterEffect: " + path);

        if (mOnSetFilterEffectListener != null) {
            mOnSetFilterEffectListener.onSetEffect(path);
        }
    }

    public void setOnSetBeautyEffectListener(OnSetEffectListener listener) {
        mOnSetBeautyEffectListener = listener;
    }

    public void setOnEnableBeautyEffectListener(OnEnableEffectListener listener) {
        mOnEnableEffectListener = listener;

        if (mBeautyEffectPanel != null) {
            mBeautyEffectPanel.setOnEnableEffectListener(listener);
        }
    }

    public void setOnSetFilterEffectListener(OnSetEffectListener listener) {
        mOnSetFilterEffectListener = listener;

        if (mFilterEffectPanel != null) {
            mFilterEffectPanel.setOnSetFilterEffectListener(mOnSetFilterEffectListener);
        }
    }

    public void setOnSetStickerEffectListener(OnSetEffectListener listener) {
        mOnSetStickerEffectListener = listener;

        if (mStickerEffectPanel != null) {
            mStickerEffectPanel.setOnSetStickerEffectListener(mOnSetStickerEffectListener);
        }
    }

    public interface OnSetEffectListener {
        void onSetEffect(String path);
    }

    public interface OnEnableEffectListener {
        void onEnableEffect(boolean enable);
    }
}
