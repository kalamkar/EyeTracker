package care.dovetail.blinker.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

import care.dovetail.blinker.Config;
import care.dovetail.blinker.R;

/**
 * Created by abhi on 9/28/16.
 */

public class GridView extends View {

    public static final int IMAGES[] = {R.mipmap.ic_butterfly1, R.mipmap.ic_butterfly2,
            R.mipmap.ic_butterfly3};

    public static final String BACKGROUNDS[] = {"garden1.jpg", "garden2.jpg", "garden3.jpg"};

    private final Paint paint = new Paint();
    private Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Canvas canvas;

    private int cellWidth = 0;
    private int cellHeight = 0;

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        background(0);
    }

    public void background(int index) {
        try {
            InputStream inStream = getResources().getAssets().open(BACKGROUNDS[index]);
            setBackground(Drawable.createFromStream(inStream, null));
        } catch (Throwable t) {
        }
    }

    public void highlight(int row, int column, int index) {
        clearAll(bitmap.getWidth(), bitmap.getHeight());
        float left = cellWidth * (Config.NUM_STEPS - row);
        float top = cellHeight * (Config.NUM_STEPS - column);
        Bitmap image = getImage(index);
        canvas.drawBitmap(image, left - image.getWidth() / 2, top - image.getHeight() / 2, paint);
        invalidate();
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

    private Bitmap getImage(int index) {
        return BitmapFactory.decodeResource(getResources(), IMAGES[index]);
    }
}
