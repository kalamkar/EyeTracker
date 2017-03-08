package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

public class BandpassSignalProcessor implements SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private static final float HORIZONTAL_FOV_FACTOR = 0.7f;
    private static final float VERTICAL_FOV_FACTOR = 0.7f;

    private static final int MAX_BLINK_HEIGHT = 30000;

    private static final int MIN_BLINK_SIGNAL_QUALITY = 95;

    private static final Pair<Integer, Integer> HALF_GRAPH_HEIGHT = new Pair<>(2000, 4000);

    private final int numSteps;
    private final FeatureObserver observer;

    private int hHalfGraphHeight = HALF_GRAPH_HEIGHT.first;
    private int vHalfGraphHeight = HALF_GRAPH_HEIGHT.first;

    private int maxHHalfGraphHeight = HALF_GRAPH_HEIGHT.first;
    private int maxVHalfGraphHeight = HALF_GRAPH_HEIGHT.first;

    private int maxHHeightAge = 0;
    private int maxVHeightAge = 0;

    private int blinkUpdateCount = 0;

    private Stats hStats = new Stats(null);
    private Stats vStats = new Stats(null);
    private Stats blinkStats = new Stats(null);

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.5 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.5 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 10.0 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    public int getSignalQuality() {
        int stdDev = Math.max(hStats.stdDev, vStats.stdDev);
        return 100 - Math.min(100, 100 * stdDev / (Math.max(hHalfGraphHeight, vHalfGraphHeight) * 200));
    }

    @Override
    public boolean isBadContact() {
        return blinkStats.stdDev == 0 && blinkUpdateCount >= blinks.length;
    }

    @Override
    public synchronized void update(int hValue, int vValue) {
        blinkUpdateCount++;
        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);
        blinkStats = new Stats(blinks);

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = /* hValue; // */ (int) hFilter.step(hValue);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = /* vValue; // */ (int) vFilter.step(vValue);

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        maxHHeightAge++;
        maxVHeightAge++;
        if (getSignalQuality() > MIN_BLINK_SIGNAL_QUALITY) {
            int newHHalfGraphHeight = Math.min(HALF_GRAPH_HEIGHT.second,
                    Math.max(HALF_GRAPH_HEIGHT.first, (hStats.max - hStats.min) / 2));
            if (newHHalfGraphHeight > maxHHalfGraphHeight - (maxHHeightAge * 2)) {
                hHalfGraphHeight = newHHalfGraphHeight;
                maxHHalfGraphHeight = newHHalfGraphHeight;
                maxHHeightAge = 0;
            }

            int newVHalfGraphHeight = Math.min(HALF_GRAPH_HEIGHT.second,
                    Math.max(HALF_GRAPH_HEIGHT.first, (vStats.max - vStats.min) / 2));
            if (newVHalfGraphHeight > maxVHalfGraphHeight - (maxVHeightAge * 2)) {
                vHalfGraphHeight = newVHalfGraphHeight;
                maxVHalfGraphHeight = newVHalfGraphHeight;
                maxVHeightAge = 0;
            }
        }
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
        if (hHalfGraphHeight * 2 < (hStats.max - hStats.min) / 2) {
            return Pair.create(hStats.min, hStats.max);
        }
        return Pair.create(-hHalfGraphHeight * 2, hHalfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        if (vHalfGraphHeight * 2 < (vStats.max - vStats.min) / 2) {
            return Pair.create(vStats.min, vStats.max);
        }
        return Pair.create(-vHalfGraphHeight * 2, vHalfGraphHeight * 2);
    }

    @Override
    public int[] blinks() {
        return blinks;
    }

    @Override
    public int[] feature1() {
        return new int[0];
    }

    @Override
    public int[] feature2() {
        return new int[0];
    }

    @Override
    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkStats.median - MAX_BLINK_HEIGHT,
                blinkStats.median + MAX_BLINK_HEIGHT);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return getSector(horizontal, vertical, numSteps, hHalfGraphHeight, vHalfGraphHeight);
    }

    private static Pair<Integer, Integer> getSector(int horizontal[], int vertical[], int numSteps,
                                                    int hHalfGraphHeight, int vHalfGraphHeight) {
        int hValue = horizontal[horizontal.length - 1];
        int vValue = vertical[vertical.length - 1];

        int hLevel = getLevel(hValue, numSteps, 0, (int) (hHalfGraphHeight * HORIZONTAL_FOV_FACTOR));
        int vLevel = getLevel(vValue, numSteps, 0, (int) (vHalfGraphHeight * VERTICAL_FOV_FACTOR));
        return Pair.create(hLevel, vLevel);
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
