package care.dovetail.blinker;

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
    private Bitmap bitmap;
    private Canvas canvas;

    private int cellWidth = 0;
    private int cellHeight = 0;

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);
    }

    public void highlight(int row, int column) {
        float left = cellWidth * row;
        float top = cellHeight * column;
        canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
    }

    public void clear() {
        clearAll(bitmap.getWidth(), bitmap.getHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        clearAll(w, h);
        cellWidth = w / Config.NUM_STEPS;
        cellHeight = h / Config.NUM_STEPS;
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
