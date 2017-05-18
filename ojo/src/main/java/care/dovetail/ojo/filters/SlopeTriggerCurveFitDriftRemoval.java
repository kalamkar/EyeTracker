package care.dovetail.ojo.filters;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import care.dovetail.ojo.Config;
import care.dovetail.ojo.RawBlinkDetector;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class SlopeTriggerCurveFitDriftRemoval implements Filter {
    private static final String TAG = "SlopeTriggerCurveFitDriftRemoval";

    private static final int POLYNOMIAL_DEGREE = 2;
    private static final double DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY = 1.66667;
    private static final int DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR
            = (int) Math.round(Config.SAMPLING_FREQ / DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY);

    private final int window[];
    private final int slopeWindow[];
    private final double thresholdWindow[];

    private final float thresholdMultiplier;

    private int threshold = 10000;

    private int base = 0;

    private int countSinceThresholdUpdate = 0;
    private int countSinceCurveUpdate = 0;

    public SlopeTriggerCurveFitDriftRemoval(int slopeWindowSize, int windowSize,
                                            float thresholdMultiplier, int thresholdUpdateInterval) {
        this.window = new int[windowSize];
        this.slopeWindow = new int[slopeWindowSize];
        this.thresholdWindow = new double[thresholdUpdateInterval];
        this.thresholdMultiplier = thresholdMultiplier;
    }

    @Override
    public int filter(int value) {
        System.arraycopy(slopeWindow, 1, slopeWindow, 0, slopeWindow.length - 1);
        slopeWindow[slopeWindow.length - 1] = value;
        float slope = Stats.calculateSlope(slopeWindow);

        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        System.arraycopy(thresholdWindow, 1, thresholdWindow, 0, thresholdWindow.length - 1);
        thresholdWindow[thresholdWindow.length - 1] = slope;
        if (countSinceThresholdUpdate == thresholdWindow.length) {
            double stddev = new StandardDeviation().evaluate(thresholdWindow);
            threshold = (int) (stddev * thresholdMultiplier);
            countSinceThresholdUpdate = 0;
        } else {
            countSinceThresholdUpdate++;
        }

        if (Math.abs(slope) > Math.abs(threshold) && countSinceCurveUpdate > 5) {
            PolynomialFunction function = getCurve(window, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
            base = (int) function.value(window.length + 1);
            countSinceCurveUpdate = 0;
        }

        countSinceCurveUpdate++;

        return value - base;
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }

    private static PolynomialFunction getCurve(int[] values, int downSampleFactor) {
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i = 0; i < values.length; i++) {
            // Down sample to speed up curve fitting
            if (i % downSampleFactor == 0) {
                points.add(i, values[i]);
            }
        }

        // Instantiate a third-degree polynomial fitter.
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(POLYNOMIAL_DEGREE);

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return new PolynomialFunction(fitter.fit(points.toList()));
    }
}
