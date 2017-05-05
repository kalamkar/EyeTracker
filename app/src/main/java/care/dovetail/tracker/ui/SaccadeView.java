package care.dovetail.tracker.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 9/28/16.
 */

public class SaccadeView extends View {

    private static final int STROKE_WIDTH = 10;
    private static final float STROKE_ALPHA = 0.9f;
    private static final float MARGIN = 30;

    private final Paint paint = new Paint();
    private Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Canvas canvas;

    private float width;
    private float height;

    public SaccadeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAlpha((int) (255 * STROKE_ALPHA));
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void show(EyeEvent.Direction direction, int amplitude) {
        if (canvas == null) {
            return;
        }
        clearAll(bitmap.getWidth(), bitmap.getHeight());
        switch (direction) {
            case UP_LEFT:
            case DOWN_RIGHT:
                drawDiagonal();
                break;
            case UP_RIGHT:
            case DOWN_LEFT:
                drawDiagonalReverse();
                break;
            case LEFT:
            case RIGHT:
                drawHorizontal();
                break;
            case UP:
            case DOWN:
                drawVertical();
                break;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        clearAll(w, h);
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }

    private void clearAll(int width, int height) {
        this.width = width;
        this.height = height;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas();
        canvas.setBitmap(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
    }

    private void drawDiagonal() {
        canvas.drawLine(MARGIN, MARGIN, width - MARGIN, height - MARGIN, paint);
    }

    private void drawDiagonalReverse() {
        canvas.drawLine(width - MARGIN, MARGIN, MARGIN, height - MARGIN, paint);
    }

    private void drawHorizontal() {
        canvas.drawLine(MARGIN, height / 2, width - MARGIN, height / 2, paint);
    }

    private void drawVertical() {
        canvas.drawLine(width / 2, MARGIN, width / 2, height - MARGIN, paint);
    }

    private void drawCircle() {
        canvas.drawCircle(width / 2, height / 2, Math.min(width, height) / 2 - MARGIN, paint);
    }
}
