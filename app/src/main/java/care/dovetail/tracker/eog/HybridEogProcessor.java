package care.dovetail.tracker.eog;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;
import care.dovetail.tracker.processing.EOGProcessor;

/**
 * Created by abhi on 4/10/17.
 */

public class HybridEogProcessor implements EOGProcessor {

    private static final int BLINK_WINDOW_LENGTH = 50;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private Stats hStats = new Stats(new int[]{});
    private Stats vStats = new Stats(new int[]{});

    private final EyeEvent.Observer eventObserver;

    private final List<Filter> filters = new ArrayList<>();

    private final Filter hDrift1;
    private final Filter vDrift1;

    private final Filter hDrift2;
    private final Filter vDrift2;

    private final Filter hCurveFit;
    private final Filter vCurveFit;

    private final Filter hFeatures;
    private final Filter vFeatures;

    private final RawBlinkDetector blinkDetector = new RawBlinkDetector(BLINK_WINDOW_LENGTH);

    private final Calibration hCalibration;
    private final Calibration vCalibration;

    private final StepSlopeGestureFilter hGesture;
    private final StepSlopeGestureFilter vGesture;

    private int skipWindow = 0;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public HybridEogProcessor(EyeEvent.Observer eventObserver, int numSteps, int gestureThreshold) {
        this.eventObserver = eventObserver;

        hDrift1 = new FixedWindowSlopeRemover(1024);
        vDrift1 = new FixedWindowSlopeRemover(1024);
        filters.add(hDrift1);
        filters.add(vDrift1);

        hDrift2 = new FixedWindowSlopeRemover(512);
        vDrift2 = new FixedWindowSlopeRemover(512);
        filters.add(hDrift2);
        filters.add(vDrift2);

        hFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);
        vFeatures = new SlopeFeaturePassthrough(5, 1.0f, 512);
        filters.add(hFeatures);
        filters.add(vFeatures);

        hCurveFit = new ValueChangeCurveFitDriftRemoval(512);
        vCurveFit = new ValueChangeCurveFitDriftRemoval(512);
        filters.add(hCurveFit);
        filters.add(vCurveFit);

        hCalibration = new FixedRangeCalibration(numSteps, 8000);
        vCalibration = new FixedRangeCalibration(numSteps, 8000);
        filters.add(hCalibration);
        filters.add(vCalibration);

        hGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
        vGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
        filters.add(hGesture);
        filters.add(vGesture);

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

        if (blinkDetector.check(vValue)) {
            RawBlinkDetector.removeSpike(horizontal, BLINK_WINDOW_LENGTH);
            RawBlinkDetector.removeSpike(vertical, BLINK_WINDOW_LENGTH);
            for (Filter filter : filters) {
                filter.removeSpike(BLINK_WINDOW_LENGTH);
            }
        }

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

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        hCalibration.filter(hValue);
        vCalibration.filter(vValue);
        sector = Pair.create(hCalibration.level(), vCalibration.level());

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        int dev = Math.round(Math.max(hStats.stdDev, vStats.stdDev) / 1000);
        return updateCount > 0 ? String.format("%d\n%dk", seconds, dev) : "";
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
        return hStats.stdDev < 20000;
    }

    @Override
    public boolean isStableVertical() {
        return vStats.stdDev < 20000;
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
        return Pair.create(hCalibration.min(), hCalibration.max());
        // return Pair.create(hStats.min, hStats.max);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(vCalibration.min(), vCalibration.max());
        // return Pair.create(vStats.min, vStats.max);
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
            return false;
        }
        eventObserver.onEyeEvent(new EyeEvent(
                EyeEvent.Type.GESTURE, direction, Math.abs(amplitude)));
        return true;
    }
}
