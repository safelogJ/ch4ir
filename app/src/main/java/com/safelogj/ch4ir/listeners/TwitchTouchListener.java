package com.safelogj.ch4ir.listeners;

import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;

import com.safelogj.ch4ir.ExoRecorder;
import com.safelogj.ch4ir.MainActivity;

import java.lang.ref.WeakReference;

@UnstableApi
public class TwitchTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    private final ExoRecorder exoRecorder;
    private WebView touchedWebView;

    private static final int HOLD_DELAY_MS = 1100;
    private float downX;
    private float downY;
    private boolean isHolding = false;
    private final Handler handler;
    private final Runnable holdRunnable;
    private final int touchSlop;

    public TwitchTouchListener(WeakReference<MainActivity> activityRef, ExoRecorder exoRecorder) {
        this.exoRecorder = exoRecorder;
        handler = new Handler(Looper.getMainLooper());
        MainActivity activity = activityRef.get();
            holdRunnable = () -> {
                if (isHolding) {
                    activity.openMenu(touchedWebView);
                }
            };
            touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

            gestureDetector = new GestureDetector((activity), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(@NonNull MotionEvent e) {
                    activity.fullScreenVLC(touchedWebView);
                    touchedWebView.post(() -> {
                        while (touchedWebView.zoomOut()) {  }
                        touchedWebView.postDelayed(exoRecorder::startCalculateScale, 310); // 200 на подстройку + 110 на анимацию фулскрина
                    });
                    return true;
                }
            });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v instanceof WebView webView) {
            touchedWebView = webView;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (v instanceof WebView webView) {
                    touchedWebView = webView;
                }
                downX = event.getX();
                downY = event.getY();
                isHolding = true;
                handler.postDelayed(holdRunnable, HOLD_DELAY_MS);
                break;

            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();
                float dx = Math.abs(moveX - downX);
                float dy = Math.abs(moveY - downY);
                if (dx > touchSlop || dy > touchSlop) {
                    isHolding = false;
                    handler.removeCallbacks(holdRunnable);
                }
                break;

            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL:
               handler.postDelayed(exoRecorder::switchMuteByTwitchUser, 800);
                isHolding = false;
                handler.removeCallbacks(holdRunnable);
                break;
        }
       return gestureDetector.onTouchEvent(event);
    }

    public void cleanup() {
        handler.removeCallbacks(holdRunnable);
        handler.removeCallbacksAndMessages(null);
    }
}
