package com.safelogj.ch4ir.listeners;

import android.graphics.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;

import com.safelogj.ch4ir.ExoRecorder;
import com.safelogj.ch4ir.MainActivity;

import java.lang.ref.WeakReference;


@UnstableApi
public class ExoTouchZoomPanListener implements View.OnTouchListener {
    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private final WeakReference<MainActivity> activityRef;
    private final ExoRecorder player;

    private float scaleFactor = 1.0f;
    private static final float MAX_SCALE = 5.0f;
    private static final float MIN_SCALE = 1.0f;

    private float translateX = 0f;
    private float translateY = 0f;
    private float lastTouchX;
    private  float lastTouchY;

    private boolean isScaling = false;
    private TextureView textureView;
    private View touchedVlcView;



    public ExoTouchZoomPanListener(TextureView textureView, WeakReference<MainActivity> activityRef, ExoRecorder player) {
        this.textureView = textureView;
        this.activityRef = activityRef;
        this.player = player;

        scaleGestureDetector = new ScaleGestureDetector(textureView.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {

                float prevScale = scaleFactor;
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));
                float scaleChange = scaleFactor / prevScale;

                translateX *= scaleChange;
                translateY *= scaleChange;

                if (scaleFactor < MIN_SCALE) scaleFactor = MIN_SCALE;
                if (scaleFactor > MAX_SCALE) scaleFactor = MAX_SCALE;

                applyTransform();
                isScaling = true;
                return true;
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
                if (scaleFactor <= 1.01f) {
                    resetZoomPan();
                }
                isScaling = false;
            }
        });

        gestureDetector = new GestureDetector(textureView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.fullScreenVLC(touchedVlcView);
                }

                if (scaleFactor > 1.0f) {
                    resetZoomPan();
                }
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                MainActivity activity = activityRef.get();
                if (activity != null) {
                    activity.openMenu(touchedVlcView);
                }
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        touchedVlcView = v;
        gestureDetector.onTouchEvent(event);

        scaleGestureDetector.onTouchEvent(event);

        if (!isScaling) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (scaleFactor > 1.0f) {
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();

                        float viewWidth = textureView.getWidth();
                        float viewHeight = textureView.getHeight();

                        float scaledWidth = viewWidth * scaleFactor;
                        float scaledHeight = viewHeight * scaleFactor;

                        float maxTranslateX = (scaledWidth - viewWidth) / 2f;
                        float maxTranslateY = (scaledHeight - viewHeight) / 2f;

                        translateX += dx;
                        translateY += dy;

                        translateX = Math.max(-maxTranslateX, Math.min(translateX, maxTranslateX));
                        translateY = Math.max(-maxTranslateY, Math.min(translateY, maxTranslateY));

                        applyTransform();
                    }
                    break;
            }
        }
        return true;
    }

    public void resetZoomPan() {
        scaleFactor = 1.0f;
        translateX = 0f;
        translateY = 0f;
        applyTransform();
    }

    public void onScrollFromMouse(float scrollDelta, float focusX, float focusY) {
        float prevScale = scaleFactor;
        scaleFactor *= (1 + scrollDelta); // scrollDelta: положительный — увеличение, отрицательный — уменьшение
        scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));
        float scaleChange = scaleFactor / prevScale;

        // Центр относительно фокуса прокрутки
        float dx = focusX - textureView.getWidth() / 2f;
        float dy = focusY - textureView.getHeight() / 2f;

        // Масштабируем смещение от фокуса, чтобы изображение «приближалось» к курсору
        translateX = (translateX - dx) * scaleChange + dx;
        translateY = (translateY - dy) * scaleChange + dy;

        // Ограничение translate, чтобы изображение не "залипало"
        float viewWidth = textureView.getWidth();
        float viewHeight = textureView.getHeight();

        float scaledWidth = viewWidth * scaleFactor;
        float scaledHeight = viewHeight * scaleFactor;

        float maxTranslateX = (scaledWidth - viewWidth) / 2f;
        float maxTranslateY = (scaledHeight - viewHeight) / 2f;

        translateX = Math.max(-maxTranslateX, Math.min(translateX, maxTranslateX));
        translateY = Math.max(-maxTranslateY, Math.min(translateY, maxTranslateY));

        if (scaleFactor <= 1.01f) { // Если масштаб почти сброшен — сбросить пан
            resetZoomPan();
        } else {
            applyTransform();
        }
    }

    public void cleanup() {
        textureView.setOnTouchListener(null);
        textureView = null;
        touchedVlcView = null;
    }



    private void applyTransform() {
        scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));
        player.setScaleText(scaleFactor);

        MainActivity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            activity.setScaleText(player.getScaleText());
        }

        Matrix matrix = new Matrix();
        float centerX = textureView.getWidth() / 2f;
        float centerY = textureView.getHeight() / 2f;
        matrix.postScale(scaleFactor, scaleFactor, centerX, centerY);
        matrix.postTranslate(translateX, translateY);
        textureView.setTransform(matrix);
    }


}
