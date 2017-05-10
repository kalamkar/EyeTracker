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

public class GestureView extends View {

    private static final int STROKE_WIDTH = 10;
    private static final float STROKE_ALPHA = 0.9f;
    private static final float MARGIN = 30;

    private final Paint paint = new Paint();
    private Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Canvas canvas;

    private float width;
    private float height;

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAlpha((int) (255 * STROKE_ALPHA));
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void showArrow(EyeEvent.Direction direction, boolean clear) {
        if (clear) {
            clearAll(bitmap.getWidth(), bitmap.getHeight());
        }
        show(direction);
        invalidate();
    }

    public void showCircle(boolean clear) {
        if (clear) {
            clearAll(bitmap.getWidth(), bitmap.getHeight());
        }
        canvas.drawCircle(width / 2, height / 2, Math.min(width, height) / 4, paint);
        invalidate();
    }

    public void showSquare(boolean clear) {
        if (clear) {
            clearAll(bitmap.getWidth(), bitmap.getHeight());
        }
        int radius = (int) (Math.min(width, height) / 4);
        canvas.drawRect(width / 2 - radius, height / 2 - radius,
                width / 2 + radius, height / 2 + radius, paint);
        invalidate();
    }

    private void show(EyeEvent.Direction direction) {
        switch (direction) {
            case UP_LEFT:
                canvas.drawLine(width / 2, height / 2, MARGIN, MARGIN, paint);
                break;
            case DOWN_RIGHT:
                canvas.drawLine(width / 2, height / 2, width - MARGIN, height - MARGIN, paint);
                break;
            case UP_RIGHT:
                canvas.drawLine(width / 2, height / 2, width - MARGIN, MARGIN, paint);
                break;
            case DOWN_LEFT:
                canvas.drawLine(width / 2, height / 2, MARGIN, height - MARGIN, paint);
                break;
            case LEFT:
                canvas.drawLine(width / 2, height / 2, MARGIN, height / 2, paint);
                break;
            case RIGHT:
                canvas.drawLine(width / 2, height / 2, width - MARGIN, height / 2, paint);
                break;
            case UP:
                canvas.drawLine(width / 2, height / 2, width / 2, MARGIN, paint);
                break;
            case DOWN:
                canvas.drawLine(width / 2, height / 2, width / 2, height - MARGIN, paint);
                break;
        }
    }

    public void clear() {
        clearAll(bitmap.getWidth(), bitmap.getHeight());
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
}
