package care.dovetail.blinker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import com.google.vr.sdk.widgets.pano.VrPanoramaView;

/**
 * Created by abhi on 9/28/16.
 */

public class VrGridView extends VrPanoramaView {

    private final static int FOV_WIDTH = 640;
    private final static int FOV_HEIGHT = 480;

    private final static int SCENE_WIDTH = FOV_WIDTH * 6;
    private final static int SCENE_HEIGHT = FOV_HEIGHT * 2;

    private final Paint paint = new Paint();

    private Bitmap fovBitmap;
    private Canvas fovCanvas;

    private Bitmap sceneBitmap;
    private Canvas sceneCanvas;

    private int cellWidth = 0;
    private int cellHeight = 0;

    public VrGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);

        sceneBitmap = Bitmap.createBitmap(SCENE_WIDTH, SCENE_HEIGHT, Bitmap.Config.ARGB_8888);
        sceneCanvas = new Canvas();
        sceneCanvas.setBitmap(sceneBitmap);
        sceneCanvas.drawColor(Color.TRANSPARENT);

        fovBitmap = Bitmap.createBitmap(FOV_WIDTH, FOV_HEIGHT, Bitmap.Config.ARGB_8888);
        fovCanvas = new Canvas();
        fovCanvas.setBitmap(fovBitmap);
        fovCanvas.drawColor(Color.WHITE);
    }

    public void highlight(int row, int column) {
        float left = cellWidth * (Config.NUM_STEPS - row);
        float top = cellHeight * (Config.NUM_STEPS - column);
        fovCanvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);

        // TODO(abhi): Update left and top as per gaze (head position) given by VR APIs
        sceneCanvas.drawBitmap(fovBitmap, SCENE_WIDTH / 2 /* left */, FOV_HEIGHT / 2 /* top */, paint);
        loadImageFromBitmap(sceneBitmap, null);
    }
}
