package care.dovetail.tracker.eog.calibration;

import care.dovetail.tracker.Stats;
import care.dovetail.tracker.eog.RawBlinkDetector;

/**
 * Created by abhi on 4/10/17.
 */

public class DriftingMedianCalibration implements Calibration {
    private static final String TAG = "FeatureBasedMinMaxTrack";

    private final int window[];

    private final int numSteps;
    private final float stddevMultiplier;

    private int min = 0;
    private int max = 0;
    private int level = 0;

    private long countSinceUpdate = 0;

    public DriftingMedianCalibration(int windowSize, int numSteps, float stddevMultiplier) {
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
    public int filter(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        if (value != window[window.length - 2]) {
            Stats stats = new Stats(window);
            int baseline = stats.median + (int) ((countSinceUpdate / 2) * stats.slope);
            min = baseline - (int) (stddevMultiplier * stats.stdDev);
            max = baseline + (int) (stddevMultiplier * stats.stdDev);
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        level = getLevel(value, min, max, numSteps);
        return level;
    }

    public static int getLevel(int value, int min, int max, int numSteps) {
        float stepHeight = (max - min) / numSteps;
        value = Math.max(min, Math.min(max - 1, value));
        int level = (int) Math.floor((value - min) / stepHeight);
        // Inverse the level
        return (numSteps - 1) - Math.min(numSteps - 1, level);
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
