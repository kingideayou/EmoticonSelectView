package me.next.emoticonkeyboard;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

/**
 * Created by dss886 on 15/9/26.
 */
public class EmoticonInputDetector {

    private static final String SHARE_PREFERENCE_NAME = "me.next.emoticoninputview";
    private static final String SHARE_PREFERENCE_TAG = "soft_input_height";

    private Activity mActivity;
    private InputMethodManager mInputManager;
    private SharedPreferences sp;
    private View mEmotionLayout;
    private EditText mEditText;
    private View mContentView;

    private EmoticonInputDetector() {}

    public static EmoticonInputDetector with(Activity activity) {
        EmoticonInputDetector emotionInputDetector = new EmoticonInputDetector();
        emotionInputDetector.mActivity = activity;
        emotionInputDetector.mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        emotionInputDetector.sp = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return emotionInputDetector;
    }

    public EmoticonInputDetector bindToContent(View contentView) {
        mContentView = contentView;
        return this;
    }

    public EmoticonInputDetector bindToTableLayout(View tableLayout, boolean showTabLayout) {
        if (!showTabLayout) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tableLayout.getLayoutParams();
            params.height = 0;
            tableLayout.setLayoutParams(params);
        }
        return this;
    }

    public EmoticonInputDetector bindToEditText(EditText editText) {
        mEditText = editText;
        mEditText.requestFocus();
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && mEmotionLayout.isShown()) {
                    lockContentHeight();
                    hideEmotionLayout(true);

                    mEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            unlockContentHeightDelayed();
                        }
                    }, 200L);
                }
                return false;
            }
        });
        return this;
    }

    public EmoticonInputDetector bindToEmotionButton(View emotionButton) {
        emotionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmotionLayout.isShown()) {
                    lockContentHeight();
                    hideEmotionLayout(true);
                    unlockContentHeightDelayed();
                } else {
                    if (isSoftInputShown()) {
                        lockContentHeight();
                        showEmotionLayout();
                        unlockContentHeightDelayed();
                    } else {
                        showEmotionLayout();
                    }
                }
            }
        });
        return this;
    }

    public EmoticonInputDetector setEmotionView(View emotionView) {
        mEmotionLayout = emotionView;
        return this;
    }

    public EmoticonInputDetector build(){
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        hideSoftInput();
        return this;
    }

    public boolean interceptBackPress() {
        // TODO: 15/11/2 change this method's name
        if (mEmotionLayout.isShown()) {
            hideEmotionLayout(false);
            return true;
        }
        return false;
    }

    private void showEmotionLayout() {
        int softInputHeight = getSupportSoftInputHeight();
        if (softInputHeight == 0) {
            softInputHeight = sp.getInt(SHARE_PREFERENCE_TAG, 400);
        }
        hideSoftInput();
        mEmotionLayout.getLayoutParams().height = softInputHeight;
        mEmotionLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmotionLayout(boolean showSoftInput) {
        if (mEmotionLayout.isShown()) {
            mEmotionLayout.setVisibility(View.GONE);
            if (showSoftInput) {
                showSoftInput();
            }
        }
    }

    private void lockContentHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
        params.height = mContentView.getHeight();
        params.weight = 0.0F;
    }

    private void unlockContentHeightDelayed() {
        mEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F;
            }
        }, 200L);
    }

    private void showSoftInput() {
        mEditText.requestFocus();
        mEditText.post(new Runnable() {
            @Override
            public void run() {
                mInputManager.showSoftInput(mEditText, 0);
            }
        });
    }

    private void hideSoftInput() {
        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    private boolean isSoftInputShown() {
        return getSupportSoftInputHeight() != 0;
    }

    private int getSupportSoftInputHeight() {
        Rect r = new Rect();
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
        int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
        int softInputHeight = screenHeight - r.bottom;
        if (Build.VERSION.SDK_INT >= 20) {
            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
        }
        if (softInputHeight < 0) {
            Log.w("EmotionInputDetector", "Warning: value of softInputHeight is below zero!");
        }
        if (softInputHeight > 0) {
            sp.edit().putInt(SHARE_PREFERENCE_TAG, softInputHeight).apply();
        }
        return softInputHeight;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private int getSoftButtonsBarHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }

}
