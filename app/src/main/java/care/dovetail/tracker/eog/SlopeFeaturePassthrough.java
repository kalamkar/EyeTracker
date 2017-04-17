package care.dovetail.tracker.eog;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class SlopeFeaturePassthrough implements Filter {

    private final int featureWindow[];
    private final double thresholdWindow[];
    private final float thresholdMultiplier;

    private long countSinceUpdate = 0;

    private int threshold;

    private int latestFeatureValue = 0;

    public SlopeFeaturePassthrough(int windowSize, float thresholdMultiplier,
                                   int thresholdUpdateInterval) {
        this.featureWindow = new int[windowSize];
        this.thresholdWindow = new double[thresholdUpdateInterval];
        this.thresholdMultiplier = thresholdMultiplier;

        threshold = (int) (1000 * thresholdMultiplier);
    }

    @Override
    public int update(int value) {
        System.arraycopy(featureWindow, 1, featureWindow, 0, featureWindow.length - 1);
        featureWindow[featureWindow.length - 1] = value;
        float slope = Stats.calculateSlope(featureWindow);

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = slope;
        if (countSinceUpdate == thresholdWindow.length) {
            double stddev = new StandardDeviation().evaluate(thresholdWindow);
            threshold = (int) (stddev * thresholdMultiplier);
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        if (Math.abs(slope) > Math.abs(threshold)) {
            latestFeatureValue = value;
        }

        return latestFeatureValue;
    }

    @Override
    public void removeSpike(int size) {
        // RawBlinkDetector.removeSpike(thresholdWindow, size);
    }
}
