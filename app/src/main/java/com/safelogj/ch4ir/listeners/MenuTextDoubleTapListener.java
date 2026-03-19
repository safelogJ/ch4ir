package com.safelogj.ch4ir.listeners;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.safelogj.ch4ir.MainActivity;

import java.lang.ref.WeakReference;
import java.util.Objects;

@OptIn(markerClass = UnstableApi.class)
public class MenuTextDoubleTapListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;

    public MenuTextDoubleTapListener(WeakReference<MainActivity> activityRef) {
        gestureDetector = new GestureDetector(Objects.requireNonNull(activityRef.get()).getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.setClipboardText();
                }
                return true;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}
