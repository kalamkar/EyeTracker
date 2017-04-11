package care.dovetail.tracker.eog;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class SlopeFeatureRemover {

    private final int featureWindow[];
    private final int driftWindow[];
    private final int baseline[];
    private final int thresholdMultiplier;

    private float currentSlope = 0;

    private long countSinceUpdate = 0;

    private int threshold;

    private int featureShift = 0;

    public SlopeFeatureRemover(int windowSize, int thresholdMultiplier,
                               int thresholdUpdateInterval) {
        this.featureWindow = new int[windowSize];
        this.baseline = new int[windowSize];
        this.driftWindow = new int[thresholdUpdateInterval];
        this.thresholdMultiplier = thresholdMultiplier;

        threshold = thresholdMultiplier;
    }

    public int update(int raw, int driftless) {
        System.arraycopy(featureWindow, 1, featureWindow, 0, featureWindow.length - 1);
        featureWindow[featureWindow.length - 1] = raw;
        float slope = Stats.calculateSlope(featureWindow);

        System.arraycopy(driftWindow, 1, driftWindow, 0, driftWindow.length - 1);
        driftWindow[driftWindow.length - 1] = raw;
        if (countSinceUpdate == driftWindow.length) {
            float drift = Stats.calculateSlope(driftWindow);
            if (Math.abs(drift) > 0) {
                threshold = (int) (Math.log10(Math.abs(drift)) * thresholdMultiplier);
            }
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        if (Math.abs(slope) > Math.abs(threshold)) {
            featureShift = driftless - baseline[0];
        }

        // Keep few values in baseline memory to carry forward later if a feaure is removed.
        System.arraycopy(baseline, 1, baseline, 0, baseline.length - 1);
        baseline[baseline.length - 1] = driftless - featureShift;
        return baseline[baseline.length - 1];
    }
}
