package care.dovetail.tracker;

import android.util.Pair;

/**
 * Created by abhi on 3/22/17.
 */

public interface EOGProcessor {

    /**
     * Update the processor with any newly acquired signal values from all the channels
     * @param hValue horizontal channel value
     * @param vValue vertical channel value
     */
    void update(int hValue, int vValue);

    /**
     * Get the cell or sector in the grid for current (latest) eye gaze.
     * @return Pair of horizontal (column) and vertical (row) value in that order.
     */
    Pair<Integer, Integer> getSector();

    /**
     *
     * @return String with numbers to be displayed on screen for debug purpose.
     */
    String getDebugNumbers();

    /**
     * Check if we are getting good signal i.e. low noise from rubbing, movements etc.
     * @return true if the signal is good
     */
    boolean isGoodSignal();

    /**
     * Get signal quality
     * @return int from 0 to 100 indicating signal quality (higher the better).
     */
    int getSignalQuality();

    /**
     * Is the horizontal signal stable so that we can calibrate on the fly.
     * @return true if horizontal signal is stable
     */
    boolean isStableHorizontal();

    /**
     * Is the vertical signal stable so that we can calibrate on the fly.
     * @return true if the vertical signal is stable
     */
    boolean isStableVertical();

    /**
     * Time series of processed values from vertical channel to be displayed as chart.
     * @return Array of ints
     */
    int[] horizontal();

    /**
     * Range of minimum and maximum values for chart from horizontal channel. This does NOT have to
     * be min and max from horizontal() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> horizontalRange();

    /**
     * Time series of processed values from horizontal channel to be displayed as chart.
     * @return Array of ints
     */
    int[] vertical();

    /**
     * Range of minimum and maximum values for chart from vertical channel. This does NOT have to
     * be min and max from vertical() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> verticalRange();

    /**
     * Time series of feature1 points to be displayed on the chart.
     * @return Array of ints
     */
    int[] feature1();

    /**
     * Range of minimum and maximum values for chart of the feature1 points. This does NOT have to
     * be min and max from feature1() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> feature1Range();

    /**
     * Time series of feature2 points to be displayed on the chart.
     * @return Array of ints
     */
    int[] feature2();

    /**
     * Range of minimum and maximum values for chart of the feature2 points. This does NOT have to
     * be min and max from feature2() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> feature2Range();
}
