package care.dovetail.tracker.eog;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import care.dovetail.tracker.Config;

/**
 * Created by abhi on 4/10/17.
 */

public class CurveFitDriftRemover implements Transformation {

    private static final int POLYNOMIAL_DEGREE = 2;
    private static final double DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY = 1.66667;
    private static final int DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR
            = (int) Math.round(Config.SAMPLING_FREQ / DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY);
    private static final int FUNCTION_CALCULATE_INTERVAL = 5;

    private final int window[];

    private float currentSlope = 0;
    private PolynomialFunction function = null;
    private int functionIntervalCount = FUNCTION_CALCULATE_INTERVAL - 1;

    private int shift = 0;

    private long countSinceUpdate = 0;

    public CurveFitDriftRemover(int windowSize) {
        this.window = new int[windowSize];
    }

    @Override
    public int update(int raw) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = raw;

        if (++functionIntervalCount == FUNCTION_CALCULATE_INTERVAL) {
            functionIntervalCount = 0;
            function = getCurve(window, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
        }

        return raw - (int) function.value(window.length + functionIntervalCount);
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
