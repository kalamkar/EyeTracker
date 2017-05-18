package care.dovetail.ojo.filters;

import care.dovetail.ojo.RawBlinkDetector;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class FeatureHoldDriftRemoval implements Filter {
    private static final String TAG = "FeatureHoldDriftRemoval";

    private final int window[];

    private int base = 0;
    private int newBase = 0;

    private float cumulativeDrift;

    private int countSinceUpdate = 0;

    public FeatureHoldDriftRemoval(int windowSize) {
        this.window = new int[windowSize];
    }

    @Override
    public int filter(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        float drift = Stats.calculateSlope(window);

        if (countSinceUpdate % window.length == 0) {
            int median = Stats.calculateMedian(window, 0, window.length);
            newBase = median + (int) ((countSinceUpdate / 2) * drift);

//            Stats stats = new Stats(window);
//            int min = stats.min + (int) ((window.length - stats.minIndex) * drift);
//            int max = stats.max + (int) ((window.length - stats.maxIndex) * drift);
//            newBase = ((min + max) / 2);

            countSinceUpdate = 0;
            cumulativeDrift = 0;
        }

        countSinceUpdate++;

        // Hold the drift updates to min max during fixation / gaze. Since it is comparing simple
        // values (and not slopes) this filter should be after SlopeFeaturePassthrough.
        if (window[window.length - 1] != window[window.length - 2]) {
            base = newBase;
            base += cumulativeDrift;
            cumulativeDrift = 0;
        } else {
            cumulativeDrift += drift;
        }

        return value - base;
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
