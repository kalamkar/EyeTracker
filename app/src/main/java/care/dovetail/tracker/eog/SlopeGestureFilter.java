package care.dovetail.tracker.eog;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/20/17.
 */

public class SlopeGestureFilter implements Filter {

    private final int window[];
    private final double thresholdWindow[];
    private final float thresholdMultiplier;

    private int threshold = 0;

    private int countSinceUpdate = 0;

    public SlopeGestureFilter(int windowSize, int thresholdWindowSize, float thresholdMultiplier,
                              int minThreshold) {
        window = new int[windowSize];
        thresholdWindow = new double[thresholdWindowSize];
        this.thresholdMultiplier = thresholdMultiplier;
    }

    @Override
    public int filter(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        float slope = Stats.calculateSlope(window);

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = slope;

        if (countSinceUpdate == thresholdWindow.length) {
            double stddev = new StandardDeviation().evaluate(thresholdWindow);
            threshold = Math.max(500, (int) (stddev * thresholdMultiplier));
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        return Math.abs(slope) > Math.abs(threshold) ? Math.round(slope) : 0;
    }

    @Override
    public void removeSpike(int size) {
    }

    public int threshold() {
        return threshold;
    }
}
