package care.dovetail.tracker.eog.calibration;

import care.dovetail.tracker.Stats;
import care.dovetail.tracker.eog.RawBlinkDetector;

/**
 * Created by abhi on 4/10/17.
 */

public class FixedIntervalMedianCalibration implements Calibration {
    private static final String TAG = "FixedIntervalMedianCalibration";

    private final int window[];

    private final int numSteps;
    private final int range;

    private int min = 0;
    private int max = 0;
    private int level = 0;

    private float cumulativeDrift;

    private int countSinceUpdate = 0;

    public FixedIntervalMedianCalibration(int windowSize, int numSteps, int range) {
        this.window = new int[windowSize];
        this.numSteps = numSteps;
        this.range = range;
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

        float drift = Stats.calculateSlope(window);

        if (countSinceUpdate % window.length == 0) {
            int median = Stats.calculateMedian(window, 0, window.length);
            int baseline = median + (int) ((countSinceUpdate / 2) * drift);
            min = baseline - (range / 2);
            max = baseline + (range / 2);
            countSinceUpdate = 0;
        }

        countSinceUpdate++;

        // Hold the drift updates to min max during fixation / gaze. Since it is comparing simple
        // values (and not slopes) this filter should be after SlopeFeaturePassthrough.
        if (window[window.length - 1] != window[window.length - 2]) {
            min += cumulativeDrift;
            max += cumulativeDrift;
            cumulativeDrift = 0;
        } else {
            cumulativeDrift += drift;
        }

        level = DriftingMedianCalibration.getLevel(value, min, max, numSteps);
        return level;
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
