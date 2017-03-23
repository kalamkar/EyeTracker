package care.dovetail.tracker.processing;

import android.util.Pair;

/**
 * Created by abhi on 3/22/17.
 */

public interface BlinkDetector {
    int BLINK_WINDOW = 20; // 400 millis

    /**
     * Update the detector with any newly acquired signal values from vertical channel
     * @param value vertical channel value
     */
    void update(int value);

    /**
     * Time series of processed values from blink channel to be displayed as chart.
     * @return Array of ints
     */
    int[] blinks();

    /**
     * Range of minimum and maximum values for chart from blink channel. This does NOT have to
     * be min and max from blinks() values above.
     * @return minimum and maximum values in that order.
     */
    Pair<Integer, Integer> blinkRange();

    /**
     * Add blink feature observers.
     * @param observer
     */
    void addFeatureObserver(Feature.FeatureObserver observer);

    int getQuality();
}
