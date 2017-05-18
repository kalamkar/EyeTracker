package care.dovetail.ojo.filters;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import care.dovetail.ojo.RawBlinkDetector;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class SimpleDriftRemoval implements Filter {
    private static final String TAG = "SimpleDriftRemoval";

    private final int window[];

    private int base = 0;
    private int newBase = 0;

    private float cumulativeDrift;

    private int countSinceUpdate = 0;

    private final double thresholdWindow[];
    private final float thresholdMultiplier;
    private int threshold;

    public SimpleDriftRemoval(int windowSize, float thresholdMultiplier) {
        this.window = new int[windowSize];
        this.thresholdWindow = new double[windowSize];
        this.thresholdMultiplier = thresholdMultiplier;
    }

    @Override
    public int filter(int value) {

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;
        float drift = Stats.calculateSlope(window);

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = drift;

        if (countSinceUpdate % window.length == 0) {
            int median = Stats.calculateMedian(window, 0, window.length);
            newBase = median + (int) ((countSinceUpdate / 2) * drift);

            double stddev = new StandardDeviation().evaluate(thresholdWindow);
            threshold = (int) (stddev * thresholdMultiplier);

            countSinceUpdate = 0;
            cumulativeDrift = 0;
        }

        countSinceUpdate++;

        // TODO(abhi) Use a separate feature slope window here for better feature detection.
        if (Math.abs(drift) > Math.abs(threshold)) {
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
