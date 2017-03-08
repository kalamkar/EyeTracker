package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
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

    private static final int BLINK_WINDOW = 20;

    private static final int MIN_QUALITY_FOR_HEIGHT_UPDATE = 95;

    private static final float HORIZONTAL_FOV_FACTOR = 0.7f;
    private static final float VERTICAL_FOV_FACTOR = 0.7f;

    private static final int MIN_BLINK_HEIGHT = 5000;
    private static final int SMALL_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    protected final int numSteps;
    protected final FeatureObserver observer;

    private int blinkUpdateCount = 0;
    private int blinkWindowIndex = 0;
    private Stats blinkStats = new Stats(null);
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 10.0 / Config.SAMPLING_FREQ));

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    protected int hHalfGraphHeight = minGraphHeight();
    protected int vHalfGraphHeight = maxGraphHeight();

    protected int maxHHalfGraphHeight = minGraphHeight();
    protected int maxVHalfGraphHeight = minGraphHeight();

    protected int maxHHeightAge = 0;
    protected int maxVHeightAge = 0;

    protected Stats hStats = new Stats(null);
    protected Stats vStats = new Stats(null);


    public SignalProcessor(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
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
        blinkStats = new Stats(blinks);

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

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = processVertical(vValue);

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        maxHHeightAge++;
        maxVHeightAge++;
        if (getSignalQuality() > MIN_QUALITY_FOR_HEIGHT_UPDATE) {
            int newHHalfGraphHeight = Math.min(maxGraphHeight(),
                    Math.max(minGraphHeight(), (hStats.max - hStats.min) / 2));
            if (newHHalfGraphHeight > maxHHalfGraphHeight - (maxHHeightAge * 2)) {
                hHalfGraphHeight = newHHalfGraphHeight;
                maxHHalfGraphHeight = newHHalfGraphHeight;
                maxHHeightAge = 0;
            }

            int newVHalfGraphHeight = Math.min(maxGraphHeight(),
                    Math.max(minGraphHeight(), (vStats.max - vStats.min) / 2));
            if (newVHalfGraphHeight > maxVHalfGraphHeight - (maxVHeightAge * 2)) {
                vHalfGraphHeight = newVHalfGraphHeight;
                maxVHalfGraphHeight = newVHalfGraphHeight;
                maxVHeightAge = 0;
            }
        }
    }

    protected void onFeature(Feature feature) {
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
     * Get the cell or sector in the grid for current eye gaze.
     * @return Pair of horizontal (column) and vertical (row) value in that order.
     */
    public abstract Pair<Integer, Integer> getSector();

    /**
     *
     * @return String with numbers to be displayed on screen for debug purpose.
     */
    public abstract String getDebugNumbers();

    /**
     * Get signal quality
     * @return int from 0 to 100 indicating signal quality (higher the better).
     */
    public abstract int getSignalQuality();

    /**
     * Check if the the electrode contact is not present or bad
     * @return true if the electrode contact is not present or bad
     */
    public boolean isBadContact() {
        return blinkStats.stdDev == 0 && blinkUpdateCount >= blinks.length;
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
     * Time series of processed values from any feature to be displayed as chart.
     * @return Array of ints
     */
    public final int[] feature1() {
        return new int[0];
    }

    /**
     * Time series of processed values from any feature to be displayed as chart.
     * @return Array of ints
     */
    public final int[] feature2() {
        return new int[0];
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

    protected static Pair<Integer, Integer> getSector(int horizontal[], int vertical[],
                                                      int numSteps, int hHalfGraphHeight,
                                                      int vHalfGraphHeight) {
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
