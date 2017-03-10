package care.dovetail.tracker.processing;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class CurveFitSignalProcessor extends SignalProcessor {
    private static final String TAG = "CurveFitSignalProcessor";

    private static final int POLYNOMIAL_DEGREE = 2;
    private static final double DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY = 10;
    private static final int DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR
            = (int) Math.round(Config.SAMPLING_FREQ / DRIFT_REMOVAL_DOWNSAMPLE_FREQUENCY);

    private static final int FUNCTION_CALCULATE_INTERVAL = 5;

    private static final int MIN_HALF_GRAPH_HEIGHT = 2000;
    private static final int MAX_HALF_GRAPH_HEIGHT = 8000;

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 0;

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

    public CurveFitSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    protected int processHorizontal(int value) {
        System.arraycopy(hFiltered, 1, hFiltered, 0, hFiltered.length - 1);
        hFiltered[hFiltered.length - 1] = (int) hFilter.step(value);

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
        vFiltered[vFiltered.length - 1] = (int) vFilter.step(value);

        if (++vFunctionIntervalCount == FUNCTION_CALCULATE_INTERVAL) {
            vFunctionIntervalCount = 0;
            vFunction = getCurve(vFiltered, DRIFT_REMOVAL_DOWN_SAMPLE_FACTOR);
        }

        return vFiltered[vFiltered.length - 1]
                - (int) vFunction.value(vertical.length + vFunctionIntervalCount);
    }

    @Override
    protected int horizontalBase() {
        return 0;
    }

    @Override
    protected int verticalBase() {
        return 0;
    }

    @Override
    protected int minGraphHeight() {
        return MIN_HALF_GRAPH_HEIGHT;
    }

    @Override
    protected int maxGraphHeight() {
        return MAX_HALF_GRAPH_HEIGHT;
    }

    @Override
    protected boolean isStableSignal() {
        return goodSignalMillis > WAIT_TIME_FOR_STABILITY_MILLIS;
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
