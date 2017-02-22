package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Utils;

public class SignalProcessor2 implements SignalProcessor {
    private static final String TAG = "SignalProcessor2";

    private static final double SAMPLING_FREQ  = 200.0;

    private static final double LOW_FREQUENCY = 0.5;
    private static final double HIGH_FREQUENCY = 3.0;
    private static final int FILTER_ORDER = 2;

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_BLINK_MEDIAN = BLINK_WINDOW * 3;

    private static final int SMALL_BLINK_HEIGHT = 4000;
    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    private final static float VERTICAL_TO_HORIZONTAL_MULTIPLIER = 0.6f;
    
    private static final int HALF_GRAPH_HEIGHT = 150;
    private static final int MIN_STEP = 72;

    private static final int MAX_SIGNAL_QUALITY_FOR_BLINK_BASELINE = 100;

    private final int numSteps;
    private final FeatureObserver observer;

    private int hLastValue;
    private int vLastValue;

    private int halfGraphHeight = HALF_GRAPH_HEIGHT;
    private int blinkWindowIndex = 0;
    private int numBlinks = 0;
    private int signalQuality = 0;

    private int hStandardDeviation = (halfGraphHeight * 5) + 1;
    private int vStandardDeviation = (halfGraphHeight * 5) + 1;

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];
    private final int feature1[] = new int[Config.GRAPH_LENGTH];
    private final int feature2[] = new int[Config.GRAPH_LENGTH];

    private int horizontalBase;
    private int verticalBase;
    private int blinkBaseline;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            HIGH_FREQUENCY / SAMPLING_FREQ, 0));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.lowpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            HIGH_FREQUENCY / SAMPLING_FREQ, 0));

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / SAMPLING_FREQ, 10.0 / SAMPLING_FREQ));

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(2, 2);

    public SignalProcessor2(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        int quality = getSignalQuality();
        return String.format("%d,%d\n%d,%d", getHorizontalBase(), getVerticalBase(),
                numBlinks, getSignalQuality());
    }

//    @Override
//    public int getSignalQuality() {
//        // (halfGraphHeight/3) is 5% signal quality loss
//        int maxStdDev = 100 * (halfGraphHeight / 3) / 5;
//        int horizLoss = 100 * Math.abs(hStandardDeviation - (halfGraphHeight / 3)) / maxStdDev;
//        int vertLoss = 100 * Math.abs(vStandardDeviation - (halfGraphHeight / 3)) / maxStdDev;
//        return 100 - Math.min(100, Math.max(horizLoss, vertLoss));
//    }

    @Override
    public int getSignalQuality() {
        return signalQuality;
    }

    private int getHorizontalBase() {
        return signalQuality < MAX_SIGNAL_QUALITY_FOR_BLINK_BASELINE ? horizontalBase : 0;
    }

    private int getVerticalBase() {
        return signalQuality < MAX_SIGNAL_QUALITY_FOR_BLINK_BASELINE ? verticalBase : 0;
    }

    @Override
    public synchronized void update(int hValue, int vValue) {
        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        int hFiltered = (int) hFilter.step(hValue - hLastValue);
        boolean hChange = Math.abs(horizontal[horizontal.length - 2] - hFiltered) > MIN_STEP;
        horizontal[horizontal.length - 1] = hChange ? hFiltered : horizontal[horizontal.length - 2];
        hLastValue = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        int vFiltered = (int) vFilter.step(vValue - vLastValue);
        boolean vChange = Math.abs(vertical[vertical.length - 2] - vFiltered) > MIN_STEP;
        vertical[vertical.length - 1] = vChange ? vFiltered : vertical[vertical.length - 2];
        vLastValue = vValue;

        int maxChanges =
                Math.max(Utils.calculateChanges(horizontal), Utils.calculateChanges(vertical));
        signalQuality = 100 - (maxChanges * 100 / horizontal.length);

        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = 0;

        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        feature2[feature2.length - 1] = 0;

        hStandardDeviation = Utils.calculateStdDeviation(horizontal);
        vStandardDeviation = Utils.calculateStdDeviation(vertical);

        blinkBaseline = Utils.calculateMedian(
                blinks, blinks.length - LENGTH_FOR_BLINK_MEDIAN, LENGTH_FOR_BLINK_MEDIAN);

        if (++blinkWindowIndex == BLINK_WINDOW) {
            blinkWindowIndex = 0;
            Feature blink = maybeGetBlink(blinks);
            if (blink != null) {
                onFeature(blink);
            }
        }

        int hLevel = getLevel(horizontal[horizontal.length - 1], numSteps, getHorizontalBase(),
                (int) (halfGraphHeight * VERTICAL_TO_HORIZONTAL_MULTIPLIER));
        int vLevel = getLevel(vertical[vertical.length - 1], numSteps, getVerticalBase(),
                halfGraphHeight);
        sector = Pair.create(hLevel, vLevel);
    }

    @Override
    public int[] horizontal() {
        return horizontal;
    }

    @Override
    public int[] vertical() {
        return vertical;
    }

    @Override
    public int[] blinks() {
        return blinks;
    }

    @Override
    public int[] feature1() {
        return feature1;
    }

    @Override
    public int[] feature2() {
        return feature2;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        return Pair.create(
                getHorizontalBase() - halfGraphHeight * 2, getHorizontalBase() + halfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
         return Pair.create(
                 getVerticalBase() - halfGraphHeight * 2, getVerticalBase() + halfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkBaseline - MAX_BLINK_HEIGHT, blinkBaseline + MAX_BLINK_HEIGHT);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    private void onFeature(Feature feature) {
        if (feature.type == Feature.Type.BLINK || feature.type == Feature.Type.SMALL_BLINK) {
            numBlinks++;
            // Use vertical channel values for blink height for gaze calculations. They are more
            // relevant than the blink channel which are much higher.
            Pair<Integer, Integer> vMinMax = Utils.calculateMinMax(
                    vertical, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);
            Feature blink = new Feature(feature.type, feature.startIndex, feature.endIndex,
                    new int[]{vMinMax.second, vMinMax.first});
            Pair<Integer, Integer> hMinMax = Utils.calculateMinMax(
                    horizontal, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);

            verticalBase = (vMinMax.second - vMinMax.first) / 3 + vMinMax.first;
            horizontalBase = (hMinMax.first + hMinMax.second) / 2;

            feature1[blink.startIndex] = blink.values[0];
            feature2[blink.endIndex] = blink.values[1];
        }
        observer.onFeature(feature);
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

    private static Feature maybeGetBlink(int values[]) {
        int last = values.length - 1;
        int first = Math.max(0, last - (BLINK_WINDOW * 2) + 1);

        int maxIndex = first;
        int minIndex = first;
        for (int i = first; i <= last; i++) {
            if (values[maxIndex] < values[i]) {
                maxIndex = i;
            }
            if (values[minIndex] > values[i]) {
                minIndex = i;
            }
        }

        if (maxIndex == last || minIndex == last || maxIndex == 0 || minIndex == 0) {
            // Ignore edges for blink to detect strict local minima and maxima.
            return null;
        }

        boolean localMaxima = (values[maxIndex - 1] < values[maxIndex])
                && (values[maxIndex] > values[maxIndex + 1]);
        boolean localMinima = (values[minIndex - 1] > values[minIndex])
                && (values[minIndex] < values[minIndex + 1]);

        int height = values[maxIndex] - values[minIndex];
        if (localMaxima && localMinima && maxIndex < minIndex) {
            if (height > SMALL_BLINK_HEIGHT && height < MIN_BLINK_HEIGHT) {
                return new Feature(Feature.Type.SMALL_BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            } else if (height > MIN_BLINK_HEIGHT && height < MAX_BLINK_HEIGHT) {
                return new Feature(Feature.Type.BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            }
        }
        return null;
    }
}
