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

    private static final int MEDIAN_WINDOW = 50;

    private final static float VERTICAL_TO_HORIZONTAL_MULTIPLIER = 0.6f;

    private static final int HALF_GRAPH_HEIGHT = 2000;

    private final int numSteps;
    private final FeatureObserver observer;

    private int halfGraphHeight = HALF_GRAPH_HEIGHT;
    private int blinkWindowIndex = 0;
    private int numBlinks = 0;

    private Stats hStats = new Stats(null);
    private Stats vStats = new Stats(null);
    private Stats blinkStats = new Stats(null);

    private Stats hMedianStats = new Stats(null);
    private Stats vMedianStats = new Stats(null);

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final int hMedian[] = new int[Config.GRAPH_LENGTH];
    private final int vMedian[] = new int[Config.GRAPH_LENGTH];

//    private final int hMedianDiff[] = new int[Config.GRAPH_LENGTH];
//    private final int vMedianDiff[] = new int[Config.GRAPH_LENGTH];

    private final int hClean[] = new int[Config.GRAPH_LENGTH];
    private final int vClean[] = new int[Config.GRAPH_LENGTH];

    private int horizontalBase;
    private int verticalBase;

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(2, 2);

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 10.0 / Config.SAMPLING_FREQ));

    public SignalProcessor3(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", numBlinks, getSignalQuality());
    }

    @Override
    public int getSignalQuality() {
        return 100 - Math.min(100, 100 * blinkStats.stdDev / (MAX_BLINK_HEIGHT * 2));
    }

    @Override
    public synchronized void update(int hValue, int vValue) {
        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);
        blinkStats = new Stats(blinks, blinks.length - LENGTH_FOR_QUALITY, LENGTH_FOR_QUALITY);
        if (getSignalQuality() < MIN_SIGNAL_QUALITY) {
            sector = Pair.create(numSteps / 2, numSteps / 2);
            numBlinks = 0;
            return;
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        System.arraycopy(hMedian, 1, hMedian, 0, hMedian.length - 1);
        hMedian[hMedian.length - 1] =
                Stats.calculateMedian(horizontal, horizontal.length - MEDIAN_WINDOW, MEDIAN_WINDOW);

        System.arraycopy(vMedian, 1, vMedian, 0, vMedian.length - 1);
        vMedian[vMedian.length - 1] =
                Stats.calculateMedian(vertical, vertical.length - MEDIAN_WINDOW, MEDIAN_WINDOW);

        hMedianStats = new Stats(hMedian);
        vMedianStats = new Stats(vMedian);

        removeDrift(hMedian, hClean); //, (int) hMedianStats.slope);
        removeDrift(vMedian, vClean); //, (int) vMedianStats.slope);

        hStats = new Stats(hClean); //, hClean.length - 100, 100);
        vStats = new Stats(vClean); // , vClean.length - 100, 100);

        horizontalBase = hStats.median;
        verticalBase = vStats.median;

        sector = getSector(hClean, vClean, numSteps, horizontalBase, verticalBase, halfGraphHeight);
    }

    @Override
    public int[] horizontal() {
//         return hMedian;
        return hClean;
    }

    @Override
    public int[] vertical() {
//         return vMedian;
        return vClean;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
//        return Pair.create(hMedianStats.min, hMedianStats.max);
        return  Pair.create(hStats.min, hStats.max);
//        return Pair.create(horizontalBase - halfGraphHeight * 2, horizontalBase + halfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
//        return Pair.create(vMedianStats.min, vMedianStats.max);
        return Pair.create(vStats.min, vStats.max);
//        return Pair.create(verticalBase - halfGraphHeight * 2, verticalBase + halfGraphHeight * 2);
    }

    @Override
    public int[] blinks() {
        return hMedian;
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
        return Pair.create(hMedianStats.min, hMedianStats.max);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    private static Pair<Integer, Integer> getSector(int horizontal[], int vertical[], int numSteps,
                                                    int horizontalBase, int verticalBase,
                                                    int halfGraphHeight) {
        int hLevel = getLevel(horizontal[horizontal.length - 1], numSteps, horizontalBase,
                (int) (halfGraphHeight * VERTICAL_TO_HORIZONTAL_MULTIPLIER));
        int vLevel = getLevel(vertical[vertical.length - 1], numSteps, verticalBase,
                halfGraphHeight);
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

    private static void removeDrift(int source[], int destination[], int slope) {
        for (int i = 0; i < source.length && i < destination.length; i++) {
            destination[i] = source[i] + (slope * i);
        }
    }

    private static void removeDrift(int source[], int destination[]) {
        PolynomialFunction fuction = getCurve(source);
        for (int i = 0; i < source.length && i < destination.length; i++) {
            destination[i] = source[i] - (int) fuction.value(i);
        }
    }

    private static PolynomialFunction getCurve(int[] values) {
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i = 0; i < values.length; i++) {
            if (i % 10 == 0) {
                points.add(i, values[i]);
            }
        }

        // Instantiate a third-degree polynomial fitter.
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);

        // Retrieve fitted parameters (coefficients of the polynomial function).
        return new PolynomialFunction(fitter.fit(points.toList()));
    }
}
