package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

public class SignalProcessor3 implements SignalProcessor {
    private static final String TAG = "SignalProcessor3";

    private static final int MAX_BLINK_HEIGHT = 30000;
    private static final int LENGTH_FOR_QUALITY =  200;

    private static final int MIN_SIGNAL_QUALITY = 95;

    private static final int FUNCTION_CALCULATE_INTERVAL = 1;

    private static final Pair<Integer, Integer> HALF_GRAPH_HEIGHT = new Pair<>(2000, 4000);

    private final int numSteps;
    private final FeatureObserver observer;

    private int vHalfGraphHeight = (HALF_GRAPH_HEIGHT.first + HALF_GRAPH_HEIGHT.second) / 2;
    private int hHalfGraphHeight = (HALF_GRAPH_HEIGHT.first + HALF_GRAPH_HEIGHT.second) / 2;

    private int updateCount = 0;

    private Stats blinkStats = new Stats(null);

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final int hClean[] = new int[Config.GRAPH_LENGTH];
    private final int vClean[] = new int[Config.GRAPH_LENGTH];

    private PolynomialFunction hFunction = null;
    private PolynomialFunction vFunction = null;
    private int functionIntervalCount = FUNCTION_CALCULATE_INTERVAL - 1;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.bessel, 2 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 0));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.bessel, 2 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 0));

    private int horizontalBase;
    private int verticalBase;

    private Pair<Integer, Integer> sector = new Pair<>(2, 2);

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 10.0 / Config.SAMPLING_FREQ));

    public SignalProcessor3(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", vHalfGraphHeight, getSignalQuality());
    }

    @Override
    public int getSignalQuality() {
        if (blinkStats.stdDev == 0) {
            // Bad connection with sensor
            return 0;
        }
        return 100 - Math.min(100, 100 * blinkStats.stdDev / (MAX_BLINK_HEIGHT * 2));
    }

    @Override
    public boolean isBadContact() {
        return blinkStats.stdDev == 0 && updateCount >= blinks.length;
    }

    @Override
    public synchronized void update(int hValue, int vValue) {
        updateCount += updateCount < blinks.length ? 1 : 0;
        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);
        blinkStats = new Stats(blinks, blinks.length - LENGTH_FOR_QUALITY, LENGTH_FOR_QUALITY);
        if (getSignalQuality() < MIN_SIGNAL_QUALITY) {
            sector = Pair.create(numSteps / 2, numSteps / 2);
            return;
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = (int) hFilter.step(hValue);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = (int) vFilter.step(vValue);

        if (++functionIntervalCount == FUNCTION_CALCULATE_INTERVAL) {
            functionIntervalCount = 0;
            hFunction = getCurve(horizontal);
            vFunction = getCurve(vertical);
        }

        System.arraycopy(hClean, 1, hClean, 0, hClean.length - 1);
        hClean[hClean.length - 1] = horizontal[horizontal.length - 1]
                - (int) hFunction.value(hClean.length + functionIntervalCount);

        System.arraycopy(vClean, 1, vClean, 0, vClean.length - 1);
        vClean[vClean.length - 1] = vertical[vertical.length - 1]
                - (int) vFunction.value(vClean.length + functionIntervalCount);

        if (getSignalQuality() > MIN_SIGNAL_QUALITY) {
            Stats hStats = new Stats(hClean);
            hHalfGraphHeight = Math.min(HALF_GRAPH_HEIGHT.second,
                    Math.max(HALF_GRAPH_HEIGHT.first, (hStats.max - hStats.min) / 2));

            Stats vStats = new Stats(vClean);
            vHalfGraphHeight = Math.min(HALF_GRAPH_HEIGHT.second,
                    Math.max(HALF_GRAPH_HEIGHT.first, (vStats.max - vStats.min) / 2));
        }
        horizontalBase = 0; // hStats.median; // (int) hFunction.value(horizontal.length);
        verticalBase = 0; // vStats.median; // (int) vFunction.value(vertical.length);
        sector = getSector(hClean, vClean, numSteps, horizontalBase, verticalBase,
                hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    public int[] horizontal() {
        return hClean;
    }

    @Override
    public int[] vertical() {
        return vClean;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        return Pair.create(horizontalBase - hHalfGraphHeight * 2, horizontalBase + hHalfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(verticalBase - vHalfGraphHeight * 2, verticalBase + vHalfGraphHeight * 2);
    }

    @Override
    public int[] blinks() {
        return blinks;
    }

    @Override
    public int[] feature1() {
        return new int[0];
    }

    @Override
    public int[] feature2() {
        return new int[0];
    }

    @Override
    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkStats.median - MAX_BLINK_HEIGHT,
                blinkStats.median + MAX_BLINK_HEIGHT);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    private static Pair<Integer, Integer> getSector(int horizontal[], int vertical[], int numSteps,
                                                    int horizontalBase, int verticalBase,
                                                    int hHalfGraphHeight, int vHalfGraphHeight) {
        int hLevel = getLevel(horizontal[horizontal.length - 1], numSteps, horizontalBase,
                hHalfGraphHeight);
        int vLevel = getLevel(vertical[vertical.length - 1], numSteps, verticalBase,
                vHalfGraphHeight);
        return Pair.create(hLevel, vLevel);
    }

    private static int getLevel(int value, int numSteps, int median, int halfGraphHeight) {
        int min = median - halfGraphHeight + 1;
        int max = median + halfGraphHeight - 1;
        // Limiting the value between +ve and -ve maximums
        // Shift the graph up so that it is between 0 and 2*halfGraph Height
        int currentValue = Math.max(min, Math.min(max, value)) - min ;
        if (currentValue >= 2 * halfGraphHeight || currentValue < 0) {
            Log.w(TAG, String.format("Incorrect normalized value %d for value %d, median %d,"
                    + "half height %d", currentValue, value, median, halfGraphHeight));
        }
        float stepHeight = (halfGraphHeight * 2) / numSteps;
        int level = (int) Math.floor(currentValue / stepHeight);
        // Inverse the level
        return (numSteps - 1) - Math.min(numSteps - 1, level);
    }

    private static PolynomialFunction getCurve(int[] values) {
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i = 0; i < values.length; i++) {
            // Down sample to speed up curve fitting
            if (i % 30 == 0) {
                points.add(i, values[i]);
            }
        }

        // Instantiate a third-degree polynomial fitter.
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return new PolynomialFunction(fitter.fit(points.toList()));
    }
}
