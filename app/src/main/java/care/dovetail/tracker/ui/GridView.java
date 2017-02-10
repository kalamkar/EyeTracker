package care.dovetail.tracker.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by abhi on 9/28/16.
 */

public class GridView extends View {

    private final Paint paint = new Paint();
    private Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Canvas canvas;

    private int numSteps = 5;

    private int cellWidth = 0;
    private int cellHeight = 0;

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAlpha((int) Math.round(255.0 * 0.7));
    }

    public void setNumSteps(int numSteps) {
        this.numSteps = numSteps;
        cellWidth = getWidth() / numSteps;
        cellHeight = getHeight() / numSteps;
    }

    public void highlight(int horizontalSector, int verticalSector) {
        clearAll(bitmap.getWidth(), bitmap.getHeight());
        float left = cellWidth * horizontalSector;
        float top = cellHeight * verticalSector;
        canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
//        canvas.drawCircle(left + Math.round(cellWidth / 2), top + Math.round(cellHeight / 2),
//                Math.round(Math.min(cellHeight, cellWidth) / 2), paint);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        clearAll(w, h);
        cellWidth = w / numSteps;
        cellHeight = h / numSteps;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }

    private void clearAll(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas();
        canvas.setBitmap(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
    }
}
