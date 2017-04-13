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
    private static final int DRIFT1_WINDOW_LENGTH = 500;
    private static final int DRIFT2_WINDOW_LENGTH = 1024;
    private static final int THRESHOLD_UPDATE_WINDOW_LENGTH = 500;
    private static final float FEATURE_THRESHOLD_MULTIPLIER = 2.0f;
    private static final int CALIBRATION_WINDOW_LENGTH = 500;
    private static final float CALIBRATION_RANGE_STDDEV_MULTIPLIER = 2.0f;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private final Transformation hPolyFit = new CurveFitDriftRemover(DRIFT1_WINDOW_LENGTH);
    private final Transformation vPolyFit = new CurveFitDriftRemover(DRIFT1_WINDOW_LENGTH);

    private final Transformation hDrift1 = new FixedWindowSlopeRemover(DRIFT1_WINDOW_LENGTH);
    private final Transformation vDrift1 = new FixedWindowSlopeRemover(DRIFT1_WINDOW_LENGTH);

    private final Transformation hDrift2 = new WeightedWindowDriftRemover(DRIFT2_WINDOW_LENGTH);
    private final Transformation vDrift2 = new WeightedWindowDriftRemover(DRIFT2_WINDOW_LENGTH);

    private final Transformation hFeatures = new SlopeFeaturePassthrough(
            SLOPE_FEATURE_WINDOW_LENGTH, FEATURE_THRESHOLD_MULTIPLIER,
            THRESHOLD_UPDATE_WINDOW_LENGTH);
    private final Transformation vFeatures = new SlopeFeaturePassthrough(
            SLOPE_FEATURE_WINDOW_LENGTH, FEATURE_THRESHOLD_MULTIPLIER,
            THRESHOLD_UPDATE_WINDOW_LENGTH);

    private final RawBlinkDetector blinkDetector = new RawBlinkDetector(BLINK_WINDOW_LENGTH);

    private final Calibration hCalibration;
    private final Calibration vCalibration;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public HybridEogProcessor(int numSteps) {
        hCalibration = new DriftingMedianCalibration(
                CALIBRATION_WINDOW_LENGTH, numSteps, CALIBRATION_RANGE_STDDEV_MULTIPLIER);
        vCalibration = new DriftingMedianCalibration(
                CALIBRATION_WINDOW_LENGTH, numSteps, CALIBRATION_RANGE_STDDEV_MULTIPLIER);
        firstUpdateTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void update(int hRaw, int vRaw) {
        updateCount++;
        long startTime = System.currentTimeMillis();
        firstUpdateTimeMillis = updateCount == 1 ? startTime : firstUpdateTimeMillis;

        int hValue = hRaw;
        int vValue = vRaw;

        hValue = hDrift1.update(hValue);
        vValue = vDrift1.update(vValue);

        hValue = hPolyFit.update(hValue);
        vValue = vPolyFit.update(vValue);

//        hValue = hDrift2.update(hValue);
//        vValue = vDrift2.update(vValue);

        hValue = hFeatures.update(hValue);
        vValue = vFeatures.update(vValue);

        if (blinkDetector.check(vValue)) {
            hDrift1.removeSpike(BLINK_WINDOW_LENGTH);
            vDrift1.removeSpike(BLINK_WINDOW_LENGTH);
            hDrift2.removeSpike(BLINK_WINDOW_LENGTH);
            vDrift2.removeSpike(BLINK_WINDOW_LENGTH);
            RawBlinkDetector.removeSpike(horizontal, BLINK_WINDOW_LENGTH);
            RawBlinkDetector.removeSpike(vertical, BLINK_WINDOW_LENGTH);
            hCalibration.removeSpike(BLINK_WINDOW_LENGTH);
            vCalibration.removeSpike(BLINK_WINDOW_LENGTH);
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        hCalibration.update(hValue);
        vCalibration.update(vValue);
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
        return Pair.create(hCalibration.min(), hCalibration.max());
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(vCalibration.min(), vCalibration.max());
    }
}
