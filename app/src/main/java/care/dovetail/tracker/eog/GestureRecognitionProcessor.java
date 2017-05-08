package care.dovetail.tracker.eog;

import android.util.Pair;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/19/17.
 */

public class GestureRecognitionProcessor implements EOGProcessor {
    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Stats hStats = new Stats(new int[0]);
    private Stats vStats = new Stats(new int[0]);

    private final EyeEvent.Observer eventObserver;

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

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public GestureRecognitionProcessor(EyeEvent.Observer eventObserver, int threshold) {
        this.eventObserver = eventObserver;
        hDrift1 = new FixedWindowSlopeRemover(1024);
        vDrift1 = new FixedWindowSlopeRemover(1024);

        hDrift2 = new FixedWindowSlopeRemover(512);
        vDrift2 = new FixedWindowSlopeRemover(512);

        hFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);
        vFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);

        hCurveFit = new ValueChangeCurveFitDriftRemoval(512);
        vCurveFit = new ValueChangeCurveFitDriftRemoval(512);

        hGesture = new StepSlopeGestureFilter(5, 512, 3.0f, threshold, 40);
        vGesture = new StepSlopeGestureFilter(5, 512, 3.0f, threshold, 40);

        firstUpdateTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void update(int hRaw, int vRaw) {
        updateCount++;
        long startTime = System.currentTimeMillis();
        firstUpdateTimeMillis = updateCount == 1 ? startTime : firstUpdateTimeMillis;

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

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        return String.format("%d\n%d", seconds, hGesture.threshold());
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
        return hStats.stdDev < 10000 && hStats.stdDev > 0;
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
        EyeEvent.Direction hDirection = hSlope > 0 ? EyeEvent.Direction.LEFT
                : hSlope < 0 ? EyeEvent.Direction.RIGHT : null;
        EyeEvent.Direction vDirection = vSlope > 0 ? EyeEvent.Direction.UP
                : vSlope < 0 ? EyeEvent.Direction.DOWN : null;

        EyeEvent.Direction direction;
        int amplitude = 0;
        if (hDirection != null && vDirection != null) {
            if (vDirection == EyeEvent.Direction.UP) {
                if (hDirection == EyeEvent.Direction.LEFT) {
                    direction = EyeEvent.Direction.UP_LEFT;
                } else {
                    direction = EyeEvent.Direction.UP_RIGHT;
                }
            } else {
                if (hDirection == EyeEvent.Direction.LEFT) {
                    direction = EyeEvent.Direction.DOWN_LEFT;
                } else {
                    direction = EyeEvent.Direction.DOWN_RIGHT;
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
        eventObserver.onEyeEvent(new EyeEvent(
                EyeEvent.Type.SACCADE, direction, Math.abs(amplitude), 0));
        return true;
    }

    @Override
    public int[] feature1() {
        return new int[]{};
    }

    @Override
    public Pair<Integer, Integer> feature1Range() {
        return Pair.create(-1, 1);
    }

    @Override
    public int[] feature2() {
        return new int[]{};
    }

    @Override
    public Pair<Integer, Integer> feature2Range() {
        return Pair.create(-1, 1);
    }
}
