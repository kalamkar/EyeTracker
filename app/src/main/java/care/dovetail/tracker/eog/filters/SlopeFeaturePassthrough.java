package care.dovetail.tracker.eog.filters;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class SlopeFeaturePassthrough implements Filter {

    private final int featureWindow[];
    private final double thresholdWindow[];
    private final float thresholdMultiplier;

    private long countSinceUpdate = 0;

    private int lowThreshold;
    private int highThreshold;

    private int latestFeatureValue = 0;

    public SlopeFeaturePassthrough(int windowSize, float thresholdMultiplier,
                                   int thresholdUpdateInterval) {
        this.featureWindow = new int[windowSize];
        this.thresholdWindow = new double[thresholdUpdateInterval];
        this.thresholdMultiplier = thresholdMultiplier;

        lowThreshold = 0;
        highThreshold = 0;
    }

    @Override
    public int filter(int value) {
        System.arraycopy(featureWindow, 1, featureWindow, 0, featureWindow.length - 1);
        featureWindow[featureWindow.length - 1] = value;
        float slope = Stats.calculateSlope(featureWindow);

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = slope;
        if (countSinceUpdate == thresholdWindow.length) {
            int stddev = (int) new StandardDeviation().evaluate(thresholdWindow);
            int median = (int) new Percentile().evaluate(thresholdWindow, 50);
            lowThreshold = median > 0 ? 0 : median - stddev;
            highThreshold = median < 0 ? 0 : median + stddev;
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        if (slope < lowThreshold || slope > highThreshold) {
            latestFeatureValue = value;
        }

        return latestFeatureValue;
    }

    @Override
    public void removeSpike(int size) {
        // RawBlinkDetector.removeSpike(thresholdWindow, size);
    }
}
