package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import java.util.Arrays;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

/**
 * Interface for signal processor algorithms to determine cell / sector on the grid for eye gaze.
 */
public abstract class SignalProcessor implements EOGProcessor, Feature.FeatureObserver {
    private static final String TAG = "SignalProcessor";

    private static final int QUALITY_WINDOW = 200; // 4 seconds

    private static final int MAX_STABLE_SLOPE = 25;

    private static final double QUALITY_UNIT = Math.sqrt(1000);
    private static final int MAX_NOISE_DEVIATION = 5;

    protected final int numSteps;

    protected long lastUpdateTime;
    protected long sumMillisBetweenUpdates;

    protected long goodSignalMillis;
    protected boolean lastUpdateWasGood = false;

    protected long sumProcessingMillis = 0;
    protected long updateCount = 1;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    protected Stats hStats = new Stats(null);
    protected Stats vStats = new Stats(null);

    protected Stats hQualityStats = new Stats(null);
    protected Stats vQualityStats = new Stats(null);

    protected final Calibrator calibrator;

    public SignalProcessor(int numSteps, Calibrator calibrator) {
        this.numSteps = numSteps;
        this.calibrator = calibrator;
        resetSignal();
    }

    @Override
    public synchronized final void update(int hValue, int vValue) {
        long startTime = System.currentTimeMillis();
        sumMillisBetweenUpdates += startTime - lastUpdateTime;

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = processHorizontal(hValue);
        hStats = new Stats(horizontal);
        calibrator.setHorizontalStats(hStats);
        hQualityStats = new Stats(horizontal, horizontal.length - QUALITY_WINDOW, QUALITY_WINDOW);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = processVertical(vValue);
        vStats = new Stats(vertical);
        calibrator.setVerticalStats(vStats);
        vQualityStats = new Stats(vertical, vertical.length - QUALITY_WINDOW, QUALITY_WINDOW);

        boolean isGoodSignal = isGoodSignal();
        if (isGoodSignal) {
            goodSignalMillis += Config.MILLIS_PER_UPDATE;
        } else if (!isGoodSignal && lastUpdateWasGood) {
            resetSignal();
            calibrator.reset();
        }
        lastUpdateWasGood = isGoodSignal;

        calibrator.setStableHorizontalMillis(isStableHorizontal()
                ? calibrator.getStableHorizontalMillis() + Config.MILLIS_PER_UPDATE : 0);
        calibrator.setStableVerticalMillis(isStableVertical()
                ? calibrator.getStableVerticalMillis() + Config.MILLIS_PER_UPDATE : 0);

        calibrator.update();

        sumProcessingMillis += (System.currentTimeMillis() - startTime);
        updateCount++;
        lastUpdateTime = startTime;
    }

    private void resetSignal() {
        lastUpdateTime = System.currentTimeMillis();
        goodSignalMillis = 0;
        Arrays.fill(horizontal, 0);
        Arrays.fill(vertical, 0);
    }

    @Override
    public void onFeature(Feature feature) {
        int blinkWindow = BlinkDetector.BLINK_WINDOW;
        if (Feature.Type.BLINK.equals(feature.type)) {
            int index = feature.startIndex - blinkWindow;
            feature.sector = getSector(horizontal[index], vertical[index]);
        }
        if (Feature.Type.BLINK.equals(feature.type)
                || Feature.Type.SMALL_BLINK.equals(feature.type)) {
            removeSpike(vertical,
                    feature.startIndex - blinkWindow/2, feature.endIndex + blinkWindow/2);
            removeSpike(horizontal,
                    feature.startIndex - blinkWindow/2, feature.endIndex + blinkWindow/2);
        }
    }

    /**
     * Process the newly acquired signal value from horizontal channel
     * @param value horizontal channel value
     */
    abstract protected int processHorizontal(int value);

    /**
     * Process the newly acquired signal values from vertical channel
     * @param value vertical channel value
     */
    abstract protected int processVertical(int value);


    @Override
    public final Pair<Integer, Integer> getSector() {
        int hValue = horizontal[horizontal.length - 1];
        int vValue = vertical[vertical.length - 1];
        return getSector(hValue, vValue);
    }

    /**
     * Get the cell or sector in the grid for given values of eye gaze.
     * @return Pair of horizontal (column) and vertical (row) value in that order.
     */
    private Pair<Integer, Integer> getSector(int hValue, int vValue) {
        int hLevel = getLevel(hValue, numSteps, calibrator.horizontalBase(),
                calibrator.horizontalGraphHeight());
        int vLevel = getLevel(vValue, numSteps, calibrator.verticalBase(),
                calibrator.verticalGraphHeight());
        return Pair.create(hLevel, vLevel);
    }

    @Override
    public boolean isGoodSignal() {
        double deviation = Math.sqrt(Math.max(hQualityStats.stdDev, vQualityStats.stdDev));
        return deviation != 0 && (deviation / QUALITY_UNIT) < MAX_NOISE_DEVIATION;
    }

    @Override
    public int getSignalQuality() {
        double deviation = Math.sqrt(Math.max(hQualityStats.stdDev, vQualityStats.stdDev));
        return 100 - Math.min(100, (int) (deviation / QUALITY_UNIT));
    }

    @Override
    public boolean isStableHorizontal() {
        return hStats != null && Math.abs(hStats.slope) < MAX_STABLE_SLOPE ;
    }

    @Override
    public boolean isStableVertical() {
        return vStats != null && Math.abs(vStats.slope) < MAX_STABLE_SLOPE ;
    }

    @Override
    public final int[] horizontal() {
        return horizontal;
    }

    @Override
    public final int[] vertical() {
        return vertical;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        if (calibrator.horizontalGraphHeight() * 2 < (hStats.max - hStats.min) / 2) {
            return Pair.create(hStats.min, hStats.max);
        }
        return Pair.create(-calibrator.horizontalGraphHeight(), calibrator.horizontalGraphHeight());
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        if (calibrator.verticalGraphHeight() * 2 < (vStats.max - vStats.min) / 2) {
            return Pair.create(vStats.min, vStats.max);
        }
        return Pair.create(-calibrator.verticalGraphHeight(), calibrator.verticalGraphHeight());
    }

    private static void removeSpike(int values[], int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(values.length - 1, endIndex);
        float slope = (values[startIndex] - values[endIndex]) / (endIndex - startIndex);
        for (int i = startIndex; i < endIndex; i++) {
            values[i] = Math.round(values[startIndex] - ((i - startIndex) * slope));
        }
    }

    private static int getLevel(int value, int numSteps, int median, int halfGraphHeight) {
        int min = median - halfGraphHeight + 1;
        int max = median + halfGraphHeight - 1;
        // Limiting the value between +ve and -ve maximums
        // Shift the graph up so that it is between 0 and 2*halfGraph Height
        int currentValue = Math.max(min, Math.min(max, value)) - min ;
        if (currentValue >= 2 * halfGraphHeight || currentValue < 0) {
            Log.w(TAG, String.format("Incorrect normalized value %d for value %d, median %d,"
                    + "half height %d", currentValue, value, median, halfGraphHeight));
        }
        float stepHeight = (halfGraphHeight * 2) / numSteps;
        int level = (int) Math.floor(currentValue / stepHeight);
        // Inverse the level
        return (numSteps - 1) - Math.min(numSteps - 1, level);
    }
}
