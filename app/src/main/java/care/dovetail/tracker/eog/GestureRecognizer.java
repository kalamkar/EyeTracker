package care.dovetail.tracker.eog;

import android.util.Pair;

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

    private String gestureValue = "";

    public GestureRecognizer(GestureObserver gestureObserver) {
        this.gestureObserver = gestureObserver;
        hDrift1 = new FixedWindowSlopeRemover(512);
        vDrift1 = new FixedWindowSlopeRemover(512);

        hGesture = new SlopeGestureFilter(10, 512, 3.0f, 500);
        vGesture = new SlopeGestureFilter(10, 512, 3.0f, 500);
    }

    @Override
    public void update(int hRaw, int vRaw) {
        int hValue = hRaw;
        int vValue = vRaw;

        hValue = hDrift1.filter(hValue);
        vValue = vDrift1.filter(vValue);

        hValue = hGesture.filter(hValue);
        vValue = vGesture.filter(vValue);

        // TODO(abhi): Enable vertical axis once its stable.
        if (skipWindow <= 0 && checkSlopes(hValue, 0)) {
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
        return String.format("%s\n%d,%d", gestureValue, hGesture.threshold(), vGesture.threshold());
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
        GestureObserver.Direction hDirection = hSlope > 0 ? GestureObserver.Direction.LEFT
                : hSlope < 0 ? GestureObserver.Direction.RIGHT : null;
        GestureObserver.Direction vDirection = vSlope > 0 ? GestureObserver.Direction.UP
                : vSlope < 0 ? GestureObserver.Direction.DOWN : null;

        GestureObserver.Direction direction;
        int amplitude = 0;
        if (hDirection != null && vDirection != null) {
            if (vDirection == GestureObserver.Direction.UP) {
                if (hDirection == GestureObserver.Direction.LEFT) {
                    direction = GestureObserver.Direction.UP_LEFT;
                } else {
                    direction = GestureObserver.Direction.UP_RIGHT;
                }
            } else {
                if (hDirection == GestureObserver.Direction.LEFT) {
                    direction = GestureObserver.Direction.DOWN_LEFT;
                } else {
                    direction = GestureObserver.Direction.DOWN_RIGHT;
                }
            }
            amplitude = Math.max(Math.abs(hSlope), Math.abs(vSlope));
        } else if (hDirection != null) {
            direction = hDirection;
            amplitude = hSlope;
        } else if (vDirection != null) {
            direction = vDirection;
            amplitude = vSlope;
        } else {
            gestureValue = "";
            return false;
        }
        gestureValue = String.format("%s %d", direction.toString(), amplitude);
        gestureObserver.onGesture(direction, Math.abs(amplitude));
        return true;
    }
}
