package care.dovetail.tracker.processing;

import android.util.Pair;

/**
 * Interface for signal processor algorithms to determine cell / sector on the grid for eye gaze.
 */
public interface SignalProcessor {
    interface FeatureObserver {
        void onFeature(Feature feature);
    }

    /**
     * Update the processor with any newly acquired signal values from all the channels
     * @param hValue horizontal channel value
     * @param vValue vertical channel value
     */
    void update(int hValue, int vValue);

    /**
     * Get the cell or sector in the grid for current eye gaze.
     * @return Pair of horizontal (column) and vertical (row) value in that order.
     */
    Pair<Integer, Integer> getSector();

    /**
     *
     * @return String with numbers to be displayed on screen for debug purpose.
     */
    String getDebugNumbers();

    /**
     * Get signal quality
     * @return int from 0 to 100 indicating signal quality (higher the better).
     */
    int getSignalQuality();

    /**
     * Time series of processed values from horizontal channel to be displayed as chart.
     * @return Array of ints
     */
    int[] horizontal();

    /**
     * Time series of processed values from vertical channel to be displayed as chart.
     * @return Array of ints
     */
    int[] vertical();

    /**
     * Time series of processed values from blink channel to be displayed as chart.
     * @return Array of ints
     */
    int[] blinks();

    /**
     * Time series of processed values from any feature to be displayed as chart.
     * @return Array of ints
     */
    int[] feature1();

    /**
     * Time series of processed values from any feature to be displayed as chart.
     * @return Array of ints
     */
    int[] feature2();

    /**
     * Range of minimum and maximum values for chart from horizontal channel. This does NOT have to
     * be min and max from horizontal() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> horizontalRange();

    /**
     * Range of minimum and maximum values for chart from vertical channel. This does NOT have to
     * be min and max from vertical() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> verticalRange();

    /**
     * Range of minimum and maximum values for chart from blink channel. This does NOT have to
     * be min and max from blinks() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> blinkRange();
}
