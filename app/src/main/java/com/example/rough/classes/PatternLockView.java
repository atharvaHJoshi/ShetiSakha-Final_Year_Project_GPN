package com.example.rough.classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PatternLockView extends View {

    private Paint dotPaint = new Paint();
    private Paint linePaint = new Paint();
    public List<Point> selectedDots = new ArrayList<>();
    private static final int NUM_ROWS = 8;
    private static final int NUM_COLS = 4;
    private Point[][] dotCenters = new Point[NUM_ROWS][NUM_COLS];

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);

        dotPaint.setAntiAlias(true);
        dotPaint.setColor(getResources().getColor(android.R.color.transparent));
        dotPaint.setStyle(Paint.Style.FILL);

        linePaint.setAntiAlias(true);
        linePaint.setColor(getResources().getColor(android.R.color.black));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate dot positions based on the view size
        for (int i = 0; i < NUM_ROWS; i++) {
            for (int j = 0; j < NUM_COLS; j++) {
                int x = w * (2 * j + 1) / (2 * NUM_COLS);
                int y = h * (2 * i + 1) / (2 * NUM_ROWS);
                dotCenters[i][j] = new Point(x, y);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw dots
        for (int i = 0; i < NUM_ROWS; i++) {
            for (int j = 0; j < NUM_COLS; j++) {
                int x = dotCenters[i][j].x;
                int y = dotCenters[i][j].y;
                canvas.drawCircle(x, y, 20, dotPaint);
            }
        }

        // Draw lines between selected dots
        if (!selectedDots.isEmpty()) {
            Path path = new Path();
            Point startPoint = selectedDots.get(0);
            path.moveTo(startPoint.x, startPoint.y);
            for (int i = 1; i < selectedDots.size(); i++) {
                Point endPoint = selectedDots.get(i);
                path.lineTo(endPoint.x, endPoint.y);
            }
            canvas.drawPath(path, linePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selectedDots.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < NUM_ROWS; i++) {
                    for (int j = 0; j < NUM_COLS; j++) {
                        int centerX = dotCenters[i][j].x;
                        int centerY = dotCenters[i][j].y;
                        if (Math.abs(centerX - x) < 50 && Math.abs(centerY - y) < 50) {
                            Point selectedDot = new Point(centerX, centerY);
                            if (!selectedDots.contains(selectedDot)) {
                                selectedDots.add(selectedDot);
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        invalidate();
        return true;
    }

}
