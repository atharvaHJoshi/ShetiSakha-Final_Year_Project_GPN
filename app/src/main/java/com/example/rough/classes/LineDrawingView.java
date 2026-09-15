package com.example.rough.classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class LineDrawingView extends View {

//    private Path path = new Path();
//    private Paint paint = new Paint();
//    private PointF startPoint = new PointF();
//    private PointF endPoint = new PointF();

    private Path path = new Path();
    private Paint paint = new Paint();
    private boolean isDrawing = false;

    public LineDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

//        paint.setAntiAlias(true);
//        paint.setColor(getResources().getColor(android.R.color.black));
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setStrokeWidth(5f);

        paint.setAntiAlias(true);
        paint.setColor(getResources().getColor(android.R.color.black));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        float x = event.getX();
//        float y = event.getY();
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                startPoint.set(x, y);
//                path.moveTo(x, y);
//                return true;
//
//            case MotionEvent.ACTION_MOVE:
//                endPoint.set(x, y);
//                path.reset();
//                path.moveTo(startPoint.x, startPoint.y);
//                path.lineTo(endPoint.x, endPoint.y);
//                invalidate();
//                return true;
//
//            case MotionEvent.ACTION_UP:
//                endPoint.set(x, y);
//                path.reset();
//                path.moveTo(startPoint.x, startPoint.y);
//                path.lineTo(endPoint.x, endPoint.y);
//                invalidate();
//                return true;
//        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                isDrawing = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    path.lineTo(x, y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                isDrawing = false;
//                path.reset();
                invalidate();
                return true;
        }

        return false;
    }
}
