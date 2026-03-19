package com.safelogj.ch4ir;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class ScaleTextView extends AppCompatTextView {
    private Paint strokePaint;

    public ScaleTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public ScaleTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2); // толщина обводки
        strokePaint.setColor(Color.BLACK); // цвет обводки
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Рисуем обводку
        setTextColor(strokePaint.getColor());
        getPaint().setStyle(Paint.Style.STROKE);
        getPaint().setStrokeWidth(2);
        super.onDraw(canvas);

        getPaint().setStyle(Paint.Style.FILL); // Рисуем основной текст
        setTextColor(getResources().getColor(R.color.blue_100, getContext().getTheme())); // основной цвет текста
        super.onDraw(canvas);
    }
}
