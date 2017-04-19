package care.dovetail.tracker.eog;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;
import care.dovetail.tracker.processing.EOGProcessor;

/**
 * Created by abhi on 4/10/17.
 */

public class HybridEogProcessor implements EOGProcessor {

    private static final int SLOPE_FEATURE_WINDOW_LENGTH = 5;
    private static final int BLINK_WINDOW_LENGTH = 50;
    private static final int THRESHOLD_UPDATE_WINDOW_LENGTH = 512;
    private static final float FEATURE_THRESHOLD_MULTIPLIER = 2.0f;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private final List<Filter> filters = new ArrayList<>();

    private final Filter hDrift1;
    private final Filter vDrift1;

//    private final Filter hMedian;
//    private final Filter vMedian;

    private final Filter hDrift2;
    private final Filter vDrift2;

//    private final Filter hLongDrift;
//    private final Filter vLongDrift;

    private final Filter hDrift3;
    private final Filter vDrift3;

    private final Filter hFeatures;
    private final Filter vFeatures;

    private final RawBlinkDetector blinkDetector = new RawBlinkDetector(BLINK_WINDOW_LENGTH);

    private final Calibration hCalibration;
    private final Calibration vCalibration;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public HybridEogProcessor(int numSteps) {
        hDrift1 = new FixedWindowSlopeRemover(1024);
        vDrift1 = new FixedWindowSlopeRemover(1024);
        filters.add(hDrift1);
        filters.add(vDrift1);

        hDrift2 = new FixedWindowSlopeRemover(512);
        vDrift2 = new FixedWindowSlopeRemover(512);
        filters.add(hDrift2);
        filters.add(vDrift2);

        hDrift3 = new ValueChangeCurveFitDriftRemoval(512);
        vDrift3 = new ValueChangeCurveFitDriftRemoval(512);
        filters.add(hDrift3);
        filters.add(vDrift3);

        hFeatures = new SlopeFeaturePassthrough(SLOPE_FEATURE_WINDOW_LENGTH,
                FEATURE_THRESHOLD_MULTIPLIER, THRESHOLD_UPDATE_WINDOW_LENGTH);
        vFeatures = new SlopeFeaturePassthrough(SLOPE_FEATURE_WINDOW_LENGTH,
                FEATURE_THRESHOLD_MULTIPLIER, THRESHOLD_UPDATE_WINDOW_LENGTH);
        filters.add(hFeatures);
        filters.add(vFeatures);

        hCalibration = new FixedRangeCalibration(numSteps, 10000);
        vCalibration = new FixedRangeCalibration(numSteps, 10000);
        filters.add(hCalibration);
        filters.add(vCalibration);

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

        hValue = hDrift3.filter(hValue);
        vValue = vDrift3.filter(vValue);

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        hCalibration.filter(hValue);
        vCalibration.filter(vValue);
        sector = Pair.create(hCalibration.level(), vCalibration.level());

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        return updateCount > 0 ? String.format("%d\n%d", seconds, processingMillis) : "";
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
        Stats stats = new Stats(horizontal);
        return Pair.create(stats.min, stats.max);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        Stats stats = new Stats(vertical);
        return Pair.create(stats.min, stats.max);
    }
}
