package care.dovetail.tracker.eog;

import android.util.Pair;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;
import care.dovetail.tracker.processing.EOGProcessor;

/**
 * Created by abhi on 4/19/17.
 */

public class GestureRecognizer implements EOGProcessor {
    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Stats hStats = new Stats(new int[0]);
    private Stats vStats = new Stats(new int[0]);

    private final GestureObserver gestureObserver;

    private final Filter hDrift1;
    private final Filter vDrift1;

    private final SlopeGestureFilter hGesture;
    private final SlopeGestureFilter vGesture;

    private int skipWindow = 0;

    private String gesture = "";

    public GestureRecognizer(GestureObserver gestureObserver) {
        this.gestureObserver = gestureObserver;
        hDrift1 = new FixedWindowSlopeRemover(512);
        vDrift1 = new FixedWindowSlopeRemover(512);

        hGesture = new SlopeGestureFilter(5, 512, 3.0f);
        vGesture = new SlopeGestureFilter(5, 512, 3.0f);
    }

    @Override
    public void update(int hRaw, int vRaw) {
        int hValue = hRaw;
        int vValue = vRaw;

        hValue = hDrift1.filter(hValue);
        vValue = vDrift1.filter(vValue);

        hValue = hGesture.filter(hValue);
        vValue = vGesture.filter(vValue);

        if (skipWindow <= 0 && checkSlopes(hValue, vValue)) {
            skipWindow = (int) (Config.SAMPLING_FREQ * (Config.GESTURE_VISIBILITY_MILLIS / 1000));
        } else if (skipWindow > 0){
            skipWindow--;
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;
        hStats = new Stats(horizontal);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;
        vStats = new Stats(vertical);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%s\n%d,%d", gesture, hGesture.threshold(), vGesture.threshold());
    }

    @Override
    public boolean isGoodSignal() {
        return true;
    }

    @Override
    public int getSignalQuality() {
        return 100;
    }

    @Override
    public boolean isStableHorizontal() {
        return true;
    }

    @Override
    public boolean isStableVertical() {
        return true;
    }

    @Override
    public int[] horizontal() {
        return horizontal;
    }

    @Override
    public int[] vertical() {
        return vertical;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        return Pair.create(-1000, 1000);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(-1000, 1000);
    }

    private boolean checkSlopes(int hSlope, int vSlope) {
        if (hSlope == 0 && vSlope == 0) {
            return false;
        }
        GestureObserver.Direction hDirection =
                hSlope > 0 ? GestureObserver.Direction.LEFT : GestureObserver.Direction.RIGHT;

        GestureObserver.Direction vDirection =
                vSlope > 0 ? GestureObserver.Direction.UP : GestureObserver.Direction.DOWN;

        if (hSlope != 0 && vSlope != 0) {
            if (Math.abs(hSlope) > Math.abs(vSlope)) {
                onGesture(hDirection, hSlope);
            } else {
                onGesture(vDirection, vSlope);
            }
        } else if (hSlope != 0) {
            onGesture(hDirection, hSlope);
        } else {
            onGesture(vDirection, vSlope);
        }
        return true;
    }

    private void onGesture(GestureObserver.Direction direction, int amplitude) {
        gestureObserver.onGesture(direction, Math.abs(amplitude));
        gesture = String.format("%s %d", direction.toString(), amplitude);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                gesture = "";
            }
        }, Config.GESTURE_VISIBILITY_MILLIS);
    }
}
