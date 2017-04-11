package care.dovetail.tracker.eog;

import android.util.Pair;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.processing.EOGProcessor;

/**
 * Created by abhi on 4/10/17.
 */

public class HybridEogProcessor implements EOGProcessor {

    private static final int SLOPE_FEATURE_WINDOW_LENGTH = 5;
    private static final int BLINK_WINDOW_LENGTH = 50;
    private static final int DRIFT_WINDOW_LENGTH = 500;
    private static final int THRESHOLD_UPDATE_WINDOW_LENGTH = 500;
    private static final int FEATURE_THRESHOLD_MULTIPLIER = 200;
    private static final int MINMAX_WINDOW_LENGTH = 500;
    private static final float MINMAX_STDDEV_MULTIPLIER = 2;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private final int numSteps;

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private final FixedWindowSlopeRemover hDrift = new FixedWindowSlopeRemover(DRIFT_WINDOW_LENGTH);
    private final FixedWindowSlopeRemover vDrift = new FixedWindowSlopeRemover(DRIFT_WINDOW_LENGTH);

    private final SlopeFeatureRemover hFeatureRemover = new SlopeFeatureRemover(
            SLOPE_FEATURE_WINDOW_LENGTH, FEATURE_THRESHOLD_MULTIPLIER,
            THRESHOLD_UPDATE_WINDOW_LENGTH);
    private final SlopeFeatureRemover vFeatureRemover = new SlopeFeatureRemover(
            SLOPE_FEATURE_WINDOW_LENGTH, FEATURE_THRESHOLD_MULTIPLIER,
            THRESHOLD_UPDATE_WINDOW_LENGTH);

    private final RawBlinkDetector blinkDetector = new RawBlinkDetector(BLINK_WINDOW_LENGTH);

    private final FeatureBasedMinMaxTracker hMinMax;
    private final FeatureBasedMinMaxTracker vMinMax;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public HybridEogProcessor(int numSteps) {
        this.numSteps = numSteps;
        hMinMax = new FeatureBasedMinMaxTracker(
                MINMAX_WINDOW_LENGTH, numSteps, MINMAX_STDDEV_MULTIPLIER);
        vMinMax = new FeatureBasedMinMaxTracker(
                MINMAX_WINDOW_LENGTH, numSteps, MINMAX_STDDEV_MULTIPLIER);
        firstUpdateTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void update(int hRaw, int vRaw) {
        updateCount++;
        long startTime = System.currentTimeMillis();
        firstUpdateTimeMillis = updateCount == 1 ? startTime : firstUpdateTimeMillis;

        int hDriftless = hDrift.update(hRaw);
        int vDriftless = vDrift.update(vRaw);

        int hBaseline = hFeatureRemover.update(hRaw, hDriftless);
        int vBaseline = vFeatureRemover.update(vRaw, vDriftless);

        if (blinkDetector.check(vDriftless)) {
            hDrift.removeSpike(BLINK_WINDOW_LENGTH);
            vDrift.removeSpike(BLINK_WINDOW_LENGTH);
            RawBlinkDetector.removeSpike(horizontal, BLINK_WINDOW_LENGTH);
            RawBlinkDetector.removeSpike(vertical, BLINK_WINDOW_LENGTH);
            hMinMax.removeSpike(BLINK_WINDOW_LENGTH);
            vMinMax.removeSpike(BLINK_WINDOW_LENGTH);
        }

        hDriftless = hDriftless - hBaseline;
        vDriftless = vDriftless - vBaseline;

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hDriftless;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vDriftless;

        int hLevel = hMinMax.update(hDriftless);
        int vLevel = vMinMax.update(vDriftless);
        sector = Pair.create(hLevel, vLevel);

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    @Override
    public String getDebugNumbers() {
        return updateCount > 0 ? String.format("%d\n%d",
                (System.currentTimeMillis() - firstUpdateTimeMillis) / 1000, processingMillis) : "";
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
        return Pair.create(hMinMax.min(), hMinMax.max());
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(vMinMax.min(), vMinMax.max());
    }
}
