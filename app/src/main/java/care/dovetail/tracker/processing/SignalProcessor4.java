package care.dovetail.tracker.processing;

import android.util.Pair;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class SignalProcessor4 extends SignalProcessor {
    private static final String TAG = "SignalProcessor4";

    private static final double DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY = 6.6666667;
    private static final int DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR
            = (int) Math.round(Config.SAMPLING_FREQ / DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY);

    private static final int FUNCTION_CALCULATE_INTERVAL = 5;

    private static final Pair<Integer, Integer> HALF_GRAPH_HEIGHT = new Pair<>(2000, 4000);

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 10000;

    private final int hFiltered[] = new int[Config.GRAPH_LENGTH];
    private final int vFiltered[] = new int[Config.GRAPH_LENGTH];

    private PolynomialFunction hFunction = null;
    private PolynomialFunction vFunction = null;
    private int hFunctionIntervalCount = FUNCTION_CALCULATE_INTERVAL - 1;
    private int vFunctionIntervalCount = FUNCTION_CALCULATE_INTERVAL - 1;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 0));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 0));

    public SignalProcessor4(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    protected int processHorizontal(int value) {
        System.arraycopy(hFiltered, 1, hFiltered, 0, hFiltered.length - 1);
        hFiltered[hFiltered.length - 1] = /* hValue; // */ (int) hFilter.step(value);

        if (++hFunctionIntervalCount == FUNCTION_CALCULATE_INTERVAL) {
            hFunctionIntervalCount = 0;
            hFunction = getCurve(hFiltered, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
        }

        return hFiltered[hFiltered.length - 1]
                - (int) hFunction.value(horizontal.length + hFunctionIntervalCount);
    }

    @Override
    protected int processVertical(int value) {
        System.arraycopy(vFiltered, 1, vFiltered, 0, vFiltered.length - 1);
        vFiltered[vFiltered.length - 1] = /* vValue; // */ (int) vFilter.step(value);

        if (++vFunctionIntervalCount == FUNCTION_CALCULATE_INTERVAL) {
            vFunctionIntervalCount = 0;
            vFunction = getCurve(vFiltered, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
        }

        return vFiltered[vFiltered.length - 1]
                - (int) vFunction.value(vertical.length + vFunctionIntervalCount);
    }

    @Override
    protected int minGraphHeight() {
        return HALF_GRAPH_HEIGHT.first;
    }

    @Override
    protected int maxGraphHeight() {
        return HALF_GRAPH_HEIGHT.second;
    }

    @Override
    protected int waitMillisForStability() {
        return WAIT_TIME_FOR_STABILITY_MILLIS;
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
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return new PolynomialFunction(fitter.fit(points.toList()));
    }
}
