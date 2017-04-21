package care.dovetail.tracker.eog;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/20/17.
 */

public class StepSlopeGestureFilter implements Filter {

    private final int window[];
    private final int gazeSizesWindow[];
    private final double thresholdWindow[];
    private final float thresholdMultiplier;
    private final int minThreshold;
    private final int minGazeSize;

    private int threshold = 0;

    private int countSinceThresholdUpdate = 0;



    public StepSlopeGestureFilter(int windowSize, int thresholdWindowSize, float thresholdMultiplier,
                                  int minThreshold, int minGazeSize) {
        window = new int[windowSize];
        gazeSizesWindow = new int[windowSize + 1];
        thresholdWindow = new double[thresholdWindowSize];
        this.thresholdMultiplier = thresholdMultiplier;
        this.minThreshold = minThreshold;
        this.minGazeSize = minGazeSize;
    }

    @Override
    public int filter(int value) {
        System.arraycopy(gazeSizesWindow, 1, gazeSizesWindow, 0, gazeSizesWindow.length - 1);
        gazeSizesWindow[gazeSizesWindow.length - 1] = window[window.length - 1] == value
                ? gazeSizesWindow[gazeSizesWindow.length - 1] + 1 : 0;

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        float slope = calculateStepSlope(window);

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = slope;

        if (countSinceThresholdUpdate == thresholdWindow.length) {
            double stddev = new StandardDeviation().evaluate(thresholdWindow);
            threshold = Math.max(minThreshold, (int) (stddev * thresholdMultiplier));
            countSinceThresholdUpdate = 0;
        } else {
            countSinceThresholdUpdate++;
        }

        return (Math.abs(slope) > Math.abs(threshold))
                && (gazeSizesWindow[0] >= minGazeSize) ? Math.round(slope) : 0;
    }

    @Override
    public void removeSpike(int size) {
        // RawBlinkDetector.removeSpike(thresholdWindow, size);
    }

    public int threshold() {
        return threshold;
    }

    public int getGazeSize() {
        return gazeSizesWindow[0];
    }

    private static float calculateStepSlope(int window[]) {
        Stats stats = new Stats(window);
        int amplitude = Math.abs(stats.max - stats.min);
        int sign = stats.maxIndex - stats.minIndex;
        sign = sign / (sign != 0 ? Math.abs(sign) : 1);
        return sign * amplitude;
    }
}
