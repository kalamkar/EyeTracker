package care.dovetail.tracker.eog;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import care.dovetail.tracker.Config;

/**
 * Created by abhi on 4/10/17.
 */

public class ValueChangeCurveFitDriftRemoval implements Filter {
    private static final String TAG = "ValueChangeCurveFitDriftRemoval";

    private static final int POLYNOMIAL_DEGREE = 2;
    private static final double DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY = 1.66667;
    private static final int DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR
            = (int) Math.round(Config.SAMPLING_FREQ / DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY);

    private final int window[];

    private int base = 0;

    private int countSinceUpdate = 0;

    public ValueChangeCurveFitDriftRemoval(int windowSize) {
        this.window = new int[windowSize];
    }

    @Override
    public int filter(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        if (window[window.length - 1] != window[window.length - 2] && countSinceUpdate > 5) {
            PolynomialFunction function = getCurve(window, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
            base = (int) function.value(window.length + 1);
            countSinceUpdate = 0;
        }

        countSinceUpdate++;

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
