package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import java.util.Arrays;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

/**
 * Interface for signal processor algorithms to determine cell / sector on the grid for eye gaze.
 */
public abstract class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    private static final int MILLIS_PER_UPDATE = (int) Math.round(1000.0 / Config.SAMPLING_FREQ);

    private static final int BLINK_WINDOW = 20; // 400 millis

    private static final int QUALITY_WINDOW = 200; // 4 seconds

    private static final int MAX_STABLE_SLOPE = 25;

    private static final double QUALITY_UNIT = Math.sqrt(1000);
    private static final int MAX_NOISE_DEVIATION = 5;

    private static final float HORIZONTAL_FOV_FACTOR = 1.0f;
    private static final float VERTICAL_FOV_FACTOR = 1.0f;

    private static final int SMALL_BLINK_HEIGHT = 5000;
    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    protected final int numSteps;
    protected final FeatureObserver observer;

    private long blinkUpdateCount = 0;
    private int blinkWindowIndex = 0;
    private Stats blinkStats = new Stats(null);
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 1, 1.25 / Config.SAMPLING_FREQ, 2.5 / Config.SAMPLING_FREQ));

    protected long goodSignalMillis;
    protected boolean lastUpdateWasGood = false;

    protected long stableHorizontalMillis;
    protected long stableVerticalMillis;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    protected int hHalfGraphHeight = minGraphHeight();
    protected int vHalfGraphHeight = minGraphHeight();

    protected int horizontalBase = 0;
    protected int verticalBase = 0;

    private long maxHHeightAge = 0;
    private long maxVHeightAge = 0;

    protected Stats hStats = new Stats(null);
    protected Stats vStats = new Stats(null);

    protected Stats hQualityStats = new Stats(null);
    protected Stats vQualityStats = new Stats(null);

    public SignalProcessor(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
        resetSignal();
    }

    /**
     * Update the processor with any newly acquired signal values from all the channels
     * @param hValue horizontal channel value
     * @param vValue vertical channel value
     */
    public synchronized final void update(int hValue, int vValue) {
        blinkUpdateCount++;
        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);
        blinkStats = new Stats(blinks, blinks.length - QUALITY_WINDOW, QUALITY_WINDOW);

        if (++blinkWindowIndex == BLINK_WINDOW) {
            blinkWindowIndex = 0;
            Feature blink = Feature.maybeGetBlink(blinks, SMALL_BLINK_HEIGHT, MIN_BLINK_HEIGHT,
                    MAX_BLINK_HEIGHT);
            if (blink != null) {
                onFeature(blink);
            }
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = processHorizontal(hValue);
        hStats = new Stats(horizontal);
        hQualityStats = new Stats(horizontal, horizontal.length - QUALITY_WINDOW, QUALITY_WINDOW);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = processVertical(vValue);
        vStats = new Stats(vertical);
        vQualityStats = new Stats(vertical, vertical.length - QUALITY_WINDOW, QUALITY_WINDOW);

        boolean isGoodSignal = isGoodSignal();
        if (isGoodSignal) {
            goodSignalMillis += MILLIS_PER_UPDATE;
        } else if (!isGoodSignal && lastUpdateWasGood) {
            resetSignal();
            resetCalibration();
        }
        lastUpdateWasGood = isGoodSignal;

        stableHorizontalMillis = isStableHorizontal()
                ? stableHorizontalMillis + MILLIS_PER_UPDATE : 0;
        stableVerticalMillis = isStableVertical() ? stableVerticalMillis + MILLIS_PER_UPDATE : 0;

        maybeUpdateHorizontalHeight();
        maybeUpdateVerticalHeight();
    }

    private void resetSignal() {
        goodSignalMillis = 0;
        Arrays.fill(horizontal, 0);
        Arrays.fill(vertical, 0);
    }

    protected void resetCalibration() {
        hHalfGraphHeight = (minGraphHeight() + maxGraphHeight()) / 2;
        vHalfGraphHeight = hHalfGraphHeight;

        maxHHeightAge = 0;
        maxVHeightAge = 0;

        stableHorizontalMillis = 0;
        stableVerticalMillis = 0;
    }

    protected void maybeUpdateHorizontalHeight() {
        maxHHeightAge++;
        if (isGoodSignal() && isStableHorizontal()
                && Math.floor(maxHHeightAge / horizontal.length) > 0) {
            int newHHalfGraphHeight = Math.min(maxGraphHeight(),
                    Math.max(minGraphHeight(), (hStats.max - hStats.min) / 2));
            hHalfGraphHeight = newHHalfGraphHeight;
            maxHHeightAge = 0;
        }
    }

    protected void maybeUpdateVerticalHeight() {
        maxVHeightAge++;
        if (isGoodSignal() && isStableVertical()
                && Math.floor(maxVHeightAge / vertical.length) > 0) {
            int newVHalfGraphHeight = Math.min(maxGraphHeight(),
                    Math.max(minGraphHeight(), (vStats.max - vStats.min) / 2));
            vHalfGraphHeight = newVHalfGraphHeight;
            maxVHeightAge = 0;
        }
    }

    protected void onFeature(Feature feature) {
        if (Feature.Type.BLINK.equals(feature.type)) {
            int index = feature.startIndex - BLINK_WINDOW;
            feature.sector = getSector(horizontal[index], vertical[index]);
        }
        if (Feature.Type.BLINK.equals(feature.type)
                || Feature.Type.SMALL_BLINK.equals(feature.type)) {
            removeSpike(vertical,
                    feature.startIndex - BLINK_WINDOW/2, feature.endIndex + BLINK_WINDOW/2);
            removeSpike(horizontal,
                    feature.startIndex - BLINK_WINDOW/2, feature.endIndex + BLINK_WINDOW/2);
        }
        observer.onFeature(feature);
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

    /**
     * Get the cell or sector in the grid for current (latest) eye gaze.
     * @return Pair of horizontal (column) and vertical (row) value in that order.
     */
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
        int hLevel = getLevel(hValue, numSteps, horizontalBase,
                (int) (hHalfGraphHeight * HORIZONTAL_FOV_FACTOR));
        int vLevel = getLevel(vValue, numSteps, verticalBase,
                (int) (vHalfGraphHeight * VERTICAL_FOV_FACTOR));
        return Pair.create(hLevel, vLevel);
    }

    /**
     *
     * @return String with numbers to be displayed on screen for debug purpose.
     */
    public abstract String getDebugNumbers();

    /**
     * Check if we are getting good signal i.e. low noise from rubbing, movements etc.
     * @return true if the signal is good
     */
    public boolean isGoodSignal() {
        double deviation = Math.sqrt(Math.max(hQualityStats.stdDev, vQualityStats.stdDev));
        return deviation != 0 && (deviation / QUALITY_UNIT) < MAX_NOISE_DEVIATION;
    }

    /**
     * Get signal quality
     * @return int from 0 to 100 indicating signal quality (higher the better).
     */
    public int getSignalQuality() {
        double deviation = Math.sqrt(Math.max(hQualityStats.stdDev, vQualityStats.stdDev));
        return 100 - Math.min(100, (int) (deviation / QUALITY_UNIT));
    }

    /**
     * Check if the the electrode contact is not present or bad
     * @return true if the electrode contact is not present or bad
     */
    public final boolean isBadContact() {
        return blinkStats.stdDev == 0 && blinkUpdateCount >= blinks.length;
    }

    /**
     * Is the horizontal signal stable so that we can calibrate on the fly.
     * @return true if horizontal signal is stable
     */
    public boolean isStableHorizontal() {
        return hStats != null && Math.abs(hStats.slope) < MAX_STABLE_SLOPE ;
    }

    /**
     * Is the vertical signal stable so that we can calibrate on the fly.
     * @return true if the vertical signal is stable
     */
    public boolean isStableVertical() {
        return vStats != null && Math.abs(vStats.slope) < MAX_STABLE_SLOPE ;
    }

    /**
     * Time series of processed values from horizontal channel to be displayed as chart.
     * @return Array of ints
     */
    public final int[] horizontal() {
        return horizontal;
    }

    /**
     * Time series of processed values from vertical channel to be displayed as chart.
     * @return Array of ints
     */
    public final int[] vertical() {
        return vertical;
    }

    /**
     * Time series of processed values from blink channel to be displayed as chart.
     * @return Array of ints
     */
    public final int[] blinks() {
        return blinks;
    }

    /**
     * Range of minimum and maximum values for chart from horizontal channel. This does NOT have to
     * be min and max from horizontal() values above.
     * @return minimum and maximum values in that order.
     */
    public Pair<Integer, Integer> horizontalRange() {
        if (hHalfGraphHeight * 2 < (hStats.max - hStats.min) / 2) {
            return Pair.create(hStats.min, hStats.max);
        }
        return Pair.create(-hHalfGraphHeight * 2, hHalfGraphHeight * 2);
    }

    /**
     * Range of minimum and maximum values for chart from vertical channel. This does NOT have to
     * be min and max from vertical() values above.
     * @return minimum and maximum values in that order.
     */
    public Pair<Integer, Integer> verticalRange() {
        if (vHalfGraphHeight * 2 < (vStats.max - vStats.min) / 2) {
            return Pair.create(vStats.min, vStats.max);
        }
        return Pair.create(-vHalfGraphHeight * 2, vHalfGraphHeight * 2);
    }

    /**
     * Range of minimum and maximum values for chart from blink channel. This does NOT have to
     * be min and max from blinks() values above.
     * @return minimum and maximum values in that order.
     */
    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkStats.median - MAX_BLINK_HEIGHT,
                blinkStats.median + MAX_BLINK_HEIGHT);
    }

    /**
     * Minimum limit for half graph height
     * @return int value of minimum half graph height
     */
    abstract protected int minGraphHeight();

    /**
     * Maximum limit for half graph height
     * @return int value of maximum half graph height
     */
    abstract protected int maxGraphHeight();

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
