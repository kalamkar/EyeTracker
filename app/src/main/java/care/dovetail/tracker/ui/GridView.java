package care.dovetail.tracker.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by abhi on 9/28/16.
 */

public class GridView extends View {

    private final Paint paint = new Paint();
    private Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    private Canvas canvas;

    private final static int ALPHA = 175;
    private final static int MIN_ALPHA = 75;
    private final static int MAX_ALPHA = 175;

    private final static int MAX_MARKED = 10;
    private final static int HEATMAP_LENGTH = 100;

    private int numSteps = 5;
    private CursorStyle style = CursorStyle.RECTANGLE;

    private int cellWidth = 0;
    private int cellHeight = 0;

    private final List<Sector> marked = new ArrayList<>();

    private boolean heatmap;

    public enum CursorStyle {
        CIRCLE,
        RECTANGLE
    }

    private static class Sector {
        private int horizontal;
        private int vertical;

        private Sector(int horizontal, int vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }

        @Override
        public int hashCode() {
            return horizontal * 100 + vertical;
        }
    }

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
        paint.setAlpha(ALPHA);
    }

    public void setNumSteps(int numSteps) {
        this.numSteps = numSteps;
        cellWidth = getWidth() / numSteps;
        cellHeight = getHeight() / numSteps;
    }

    public void setCursorStyle(CursorStyle style) {
        this.style = style;
        switch (style) {
            case CIRCLE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5.0f);
                break;
            case RECTANGLE:
            default:
        }

    }

    public void setHeatmap(boolean enabled) {
        heatmap = enabled;
    }

    public void mark(int horizontalSector, int verticalSector) {
        marked.add(new Sector(horizontalSector, verticalSector));
        if (marked.size() > MAX_MARKED) {
            marked.remove(0);
        }
        drawSector(horizontalSector, verticalSector);
    }

    public void highlight(int horizontalSector, int verticalSector) {
        clearAll(bitmap.getWidth(), bitmap.getHeight());

        if (heatmap) {
            marked.add(new Sector(horizontalSector, verticalSector));
            if (marked.size() > HEATMAP_LENGTH) {
                marked.remove(0);
            }
            HashMap<Sector, Integer> heatmap = new HashMap<>();
            int max = 0;
            for (Sector sector : marked) {
                int frequency = heatmap.containsKey(sector) ? heatmap.get(sector) + 1 : 1;
                max = Math.max(max, frequency);
                heatmap.put(sector, frequency);
            }
            drawHeatmap(heatmap , max);
        } else {
            for (Sector sector : marked) {
                drawSector(sector.horizontal, sector.vertical);
            }
            drawSector(horizontalSector, verticalSector);
        }
    }

    private void drawHeatmap(HashMap<Sector, Integer> heatmap, int maxFrequency) {
        for (Sector sector : heatmap.keySet()) {
            int frequency = heatmap.get(sector);
            paint.setAlpha(MIN_ALPHA + (frequency * (MAX_ALPHA - MIN_ALPHA) / maxFrequency));
            drawSector(sector.horizontal, sector.vertical);
        }
    }

    private void drawSector(int horizontalSector, int verticalSector) {
        if (horizontalSector >= 0 && verticalSector >= 0) {
            float left = cellWidth * horizontalSector;
            float top = cellHeight * verticalSector;
            switch (style) {
                case CIRCLE:
                    drawCircle(left, top);
                    break;
                case RECTANGLE:
                default:
                    drawRect(left, top);
            }
        }
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

    private void drawRect(float left, float top) {
        canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
    }

    private void drawCircle(float left, float top) {
        float centerX = left + Math.round(cellWidth / 2);
        float centerY = top + Math.round(cellHeight / 2);
        float radius = Math.round(Math.min(canvas.getHeight(), canvas.getWidth()) / 10);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }
}
