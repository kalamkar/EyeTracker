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

    private final Filter hDrift2;
    private final Filter vDrift2;

    private final Filter hFeatures;
    private final Filter vFeatures;

    private final Filter hCurveFit;
    private final Filter vCurveFit;

    private final StepSlopeGestureFilter hGesture;
    private final StepSlopeGestureFilter vGesture;

    private int skipWindow = 0;

    private String gestureValue = "";

    public GestureRecognizer(GestureObserver gestureObserver) {
        this.gestureObserver = gestureObserver;
        hDrift1 = new FixedWindowSlopeRemover(1024);
        vDrift1 = new FixedWindowSlopeRemover(1024);

        hDrift2 = new FixedWindowSlopeRemover(512);
        vDrift2 = new FixedWindowSlopeRemover(512);

        hFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);
        vFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);

        hCurveFit = new ValueChangeCurveFitDriftRemoval(512);
        vCurveFit = new ValueChangeCurveFitDriftRemoval(512);

//        hGesture = new SlopeGestureFilter(5, 512, 3.0f, 1000);
//        vGesture = new SlopeGestureFilter(5, 512, 3.0f, 1000);

        hGesture = new StepSlopeGestureFilter(5, 512, 3.0f, 4000, 50);
        vGesture = new StepSlopeGestureFilter(5, 512, 3.0f, 4000, 50);
    }

    @Override
    public void update(int hRaw, int vRaw) {
        int hValue = hRaw;
        int vValue = vRaw;

        hValue = hDrift1.filter(hValue);
        vValue = vDrift1.filter(vValue);

        hValue = hDrift2.filter(hValue);
        vValue = vDrift2.filter(vValue);

        hValue = hFeatures.filter(hValue);
        vValue = vFeatures.filter(vValue);

        hValue = hCurveFit.filter(hValue);
        vValue = vCurveFit.filter(vValue);

        int hSlope = hGesture.filter(hValue);
        int vSlope = vGesture.filter(vValue);

        if (skipWindow <= 0 && isGoodSignal() && checkSlopes(hSlope, 0)) {
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
        return String.format("%s\n%d,%d", gestureValue, hGesture.threshold(),
                hGesture.getGazeSize());
    }

    @Override
    public boolean isGoodSignal() {
        return isStableHorizontal() && isStableVertical() ;
    }

    @Override
    public int getSignalQuality() {
        return 100 - Math.min(100000, Math.max(hStats.stdDev, vStats.stdDev)) / 1000;
    }

    @Override
    public boolean isStableHorizontal() {
        return hStats.stdDev < 20000 && hStats.stdDev > 0;
    }

    @Override
    public boolean isStableVertical() {
        return true; // vStats.stdDev < 20000 && vStats.stdDev > 0;
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
        return Pair.create(-5000, 5000);
//        return Pair.create(hStats.min, hStats.max);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(-5000, 5000);
//        return Pair.create(vStats.min, vStats.max);
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
