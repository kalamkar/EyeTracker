package care.dovetail.tracker.eog;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class FeatureBasedMinMaxTracker {
    private static final String TAG = "FeatureBasedMinMaxTrack";

    private final int window[];

    private final int numSteps;

    private int min = 0;
    private int max = 0;
    private int level = 0;

    private long countSinceUpdate = 0;

    public FeatureBasedMinMaxTracker(int windowSize, int numSteps) {
        this.window = new int[windowSize];
        this.numSteps = numSteps;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int update(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        if (value != window[window.length - 2]) {
            Stats stats = new Stats(window);
            min = stats.median + (int) ((countSinceUpdate / 2) * stats.slope) - (3 * stats.stdDev);
            max = stats.median + (int) ((countSinceUpdate / 2) * stats.slope) + (3 * stats.stdDev);
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        level = getLevel(value, min, max, numSteps);
        return level;
    }

    private static int getLevel(int value, int min, int max, int numSteps) {
        float stepHeight = (max - min) / numSteps;
        value = Math.max(min, Math.min(max, value));
        int level = (int) Math.floor((value - min) / stepHeight);
        // Inverse the level
        return (numSteps - 1) - Math.min(numSteps - 1, level);
    }

    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
