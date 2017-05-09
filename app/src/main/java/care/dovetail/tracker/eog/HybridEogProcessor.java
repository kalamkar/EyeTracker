package care.dovetail.tracker.eog;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class HybridEogProcessor implements EOGProcessor {
    private static final String TAG = "HybridEogProcessor";

    private static final int BLINK_WINDOW_LENGTH = 50;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private Stats hStats = new Stats(new int[]{});
    private Stats vStats = new Stats(new int[]{});

    private final EyeEvent.Observer eventObserver;

    private final int numSteps;

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

    private final GestureRecognizer gestures;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public HybridEogProcessor(EyeEvent.Observer eventObserver, int numSteps, int gestureThreshold) {
        this.eventObserver = eventObserver;
        this.numSteps = numSteps;

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

        gestures = new StepSlopeGestureRecognizer(gestureThreshold);

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

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        hCalibration.filter(hValue);
        vCalibration.filter(vValue);
        sector = Pair.create(hCalibration.level(), vCalibration.level());

        gestures.update(hValue, vValue);
        if (isGoodSignal() && gestures.hasEyeEvent()) {
            EyeEvent event = gestures.getEyeEvents().iterator().next();
            if (event.type == EyeEvent.Type.GAZE) {
                boolean left = sector.first <= numSteps / 3;
                boolean right = sector.first >= numSteps * 2 / 3;
                EyeEvent.Direction direction =  left ? EyeEvent.Direction.LEFT
                        : right ? EyeEvent.Direction.RIGHT : EyeEvent.Direction.NONE;
                event = new EyeEvent(EyeEvent.Type.GAZE, direction, 0, 0);
            }
            eventObserver.onEyeEvent(event);
        }

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return isGoodSignal() ? sector : Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        int dev = Math.round(Math.max(hStats.stdDev, vStats.stdDev) / 1000);
        return updateCount > 0 ? String.format("%d\n%dk", seconds, dev) : "";
    }

    @Override
    public boolean isGoodSignal() {
        return getSignalQuality() > 95;
    }

    @Override
    public int getSignalQuality() {
        return 100 - Math.min(100, Math.round(Math.max(hStats.stdDev, vStats.stdDev) / 10000));
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
        if (isGoodSignal()) {
            return Pair.create(hCalibration.min(), hCalibration.max());
        } else {
            return Pair.create(hStats.min, hStats.max);
        }
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        if (isGoodSignal()) {
            return Pair.create(vCalibration.min(), vCalibration.max());
        } else {
            return Pair.create(vStats.min, vStats.max);
        }
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
