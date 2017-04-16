package care.dovetail.tracker.eog;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class FixedIntervalMedianCalibration implements Calibration {
    private static final String TAG = "FixedWindowMedianCalibration";

    private final int window[];

    private final int numSteps;
    private final float stddevMultiplier;

    private int min = 0;
    private int max = 0;
    private int level = 0;

    private int countSinceUpdate = 0;

    public FixedIntervalMedianCalibration(int windowSize, int numSteps, float stddevMultiplier) {
        this.window = new int[windowSize];
        this.numSteps = numSteps;
        this.stddevMultiplier = stddevMultiplier;
    }

    @Override
    public int min() {
        return min;
    }

    @Override
    public int max() {
        return max;
    }

    @Override
    public int level() {
        return level;
    }

    @Override
    public int update(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        if (countSinceUpdate % window.length == 0) {
            Stats stats = new Stats(window);
            int baseline = stats.median + (int) ((countSinceUpdate / 2) * stats.slope);
            min = baseline - (int) (stddevMultiplier * stats.stdDev);
            max = baseline + (int) (stddevMultiplier * stats.stdDev);
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        level = DriftingMedianCalibration.getLevel(value, min, max, numSteps);
        return level;
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
