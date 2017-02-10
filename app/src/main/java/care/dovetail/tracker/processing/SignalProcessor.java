package care.dovetail.tracker.processing;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Utils;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final double SAMPLING_FREQ  = 200.0;

    private static final double LOW_FREQUENCY = 0.5;
    private static final double HIGH_FREQUENCY = 3.0;
    private static final int FILTER_ORDER = 2;

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_BLINK_MEDIAN = BLINK_WINDOW * 3;
    private static final int LENGTH_FOR_MEDIAN =  500;

    private static final int MAX_RECENT_BLINKS = 10;
    private static final int MIN_RECENT_BLINKS = 5;

    private static final int SMALL_BLINK_HEIGHT = 4000;
    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    private static final int MAX_GRAPH_HEIGHT = 4000;
    private static final float MAX_HEIGHT_CHANGE = 0.15f;

    private final int numSteps;
    private final float blinkToGazeMultiplier;
    private final float verticalToHorizontalMultiplier;
    private final FeatureObserver observer;

    private int halfGraphHeight = 2000;
    private int numBlinks = 0;
    private int blinkWindowIndex = 0;

    private int standardDeviation = (halfGraphHeight * 5) + 1;

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];
    private final int feature1[] = new int[Config.GRAPH_LENGTH];
    private final int feature2[] = new int[Config.GRAPH_LENGTH];

    private int horizontalBase;
    private int verticalBase;
    private int blinkBaseline;

    private final int horizontalBaseline[] = new int[Config.GRAPH_LENGTH];
    private final int verticalBaseline[] = new int[Config.GRAPH_LENGTH];

    private final IirFilter filter1 = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            LOW_FREQUENCY / SAMPLING_FREQ, HIGH_FREQUENCY / SAMPLING_FREQ));

    private final IirFilter filter2 = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            LOW_FREQUENCY / SAMPLING_FREQ, HIGH_FREQUENCY / SAMPLING_FREQ));

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / SAMPLING_FREQ, 10.0 / SAMPLING_FREQ));

    private final List<Feature> recentBlinks = new ArrayList<>(MAX_RECENT_BLINKS);

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(0, 0);

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    public SignalProcessor(FeatureObserver observer, int numSteps, float blinkToGazeMultiplier,
                           float verticalToHorizontalMultiplier) {
        this.numSteps = numSteps;
        this.blinkToGazeMultiplier = blinkToGazeMultiplier;
        this.verticalToHorizontalMultiplier = verticalToHorizontalMultiplier;
        this.observer = observer;
    }

    public int getHalfGraphHeight() {
        return halfGraphHeight;
    }

    public int getStandardDeviation() {
        return standardDeviation;
    }

    public int getNumBlinks() {
        return numBlinks;
    }

    public boolean isGoodSignal() {
        return standardDeviation < (halfGraphHeight * 5);
    }

    public synchronized void update(int channel1, int channel2) {
        System.arraycopy(values1, 1, values1, 0, values1.length - 1);
        values1[values1.length - 1] = (int) filter1.step(channel1);

        System.arraycopy(values2, 1, values2, 0, values2.length - 1);
        values2[values2.length - 1] = (int) filter2.step(channel2);

        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(channel2);

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = 0;

        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        feature2[feature2.length - 1] = 0;

        standardDeviation = Utils.calculateStdDeviation(values1);

        if (recentBlinks.size() < MIN_RECENT_BLINKS) {
            horizontalBase = Utils.calculateMedian(
                    values1, values1.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
            verticalBase = Utils.calculateMedian(
                    values2, values2.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        }
        blinkBaseline = Utils.calculateMedian(
                blinks, blinks.length - LENGTH_FOR_BLINK_MEDIAN, LENGTH_FOR_BLINK_MEDIAN);

        if (++blinkWindowIndex == BLINK_WINDOW) {
            blinkWindowIndex = 0;
            Feature blink = maybeGetBlink(blinks);
            if (blink != null) {
                onFeature(blink);
            }
        }

        int horizLevel = getLevel(values1[values1.length - 1], numSteps, horizontalBase,
                (int) (halfGraphHeight * verticalToHorizontalMultiplier));
        int vertLevel = getLevel(values2[values2.length - 1], numSteps, verticalBase,
                halfGraphHeight);
        sector = Pair.create(horizLevel, vertLevel);
    }

    public int[] channel1() {
        return values1;
    }

    public int[] channel2() {
        return values2;
    }

    public int[] blinks() {
        return blinks;
    }

    public int[] feature1() {
        return feature1;
    }

    public int[] feature2() {
        return feature2;
    }

    public Pair<Integer, Integer> range1() {
        return Pair.create(
                horizontalBase - halfGraphHeight * 2, horizontalBase + halfGraphHeight * 2);
    }

    public Pair<Integer, Integer> range2() {
         return Pair.create(
                 verticalBase - halfGraphHeight * 2, verticalBase + halfGraphHeight * 2);
    }

    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkBaseline - MAX_BLINK_HEIGHT, blinkBaseline + MAX_BLINK_HEIGHT);
    }

    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    private void onFeature(Feature feature) {
        if (feature.type == Feature.Type.SMALL_BLINK || feature.type == Feature.Type.BLINK) {
            numBlinks++;
            // Use vertical channel values for blink height for gaze calculations. They are more
            // relevant than the blink channel which are much higher.
            Pair<Integer, Integer> vMinMax = Utils.calculateMinMax(
                    values2, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);
            Feature blink = new Feature(feature.type, feature.startIndex, feature.endIndex,
                    new int[]{vMinMax.second, vMinMax.first});
            Pair<Integer, Integer> hMinMax = Utils.calculateMinMax(
                    values1, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);

            System.arraycopy(verticalBaseline, 1, verticalBaseline, 0, verticalBaseline.length - 1);
            verticalBaseline[verticalBaseline.length - 1] = vMinMax.first;

            System.arraycopy(horizontalBaseline, 1, horizontalBaseline, 0,
                    horizontalBaseline.length - 1);
            horizontalBaseline[horizontalBaseline.length - 1] = hMinMax.first;

            verticalBase = Utils.calculateMedian(verticalBaseline);
            horizontalBase = Utils.calculateMedian(horizontalBaseline);

            recentBlinks.add(blink);
            if (recentBlinks.size() > MAX_RECENT_BLINKS) {
                recentBlinks.remove(0);
            }

            if (recentBlinks.size() >= MIN_RECENT_BLINKS && isGoodSignal()) {
                int newHalfGraphHeight = (int) (((float) Utils.calculateMedianHeight(recentBlinks))
                        * blinkToGazeMultiplier);
                // If the increase or decrease in new graph height is more than 25% then increase or
                // decrease only by 25%
                if (Math.abs(newHalfGraphHeight - halfGraphHeight) / halfGraphHeight
                        < MAX_HEIGHT_CHANGE) {
                    halfGraphHeight = newHalfGraphHeight;
                } else {
                    halfGraphHeight = newHalfGraphHeight > halfGraphHeight
                            ? (int) (halfGraphHeight * (1 + MAX_HEIGHT_CHANGE))
                            : (int) (halfGraphHeight * (1 - MAX_HEIGHT_CHANGE));
                }
                // Limit half graph height to MAX_GRAPH_HEIGHT
                halfGraphHeight = Math.min(halfGraphHeight, MAX_GRAPH_HEIGHT);
            }

            feature1[blink.startIndex] = blink.values[0];
            feature2[blink.endIndex] = blink.values[1];
        }

        observer.onFeature(feature);
    }

    private static int getLevel(int value, int numSteps, int median, int halfGraphHeight) {
        float stepHeight = halfGraphHeight / (numSteps / 2); // divide by 2 as values  +ve and -ve
        int min = median - halfGraphHeight;
        int max = median + halfGraphHeight;
        int currentValue = Math.max(min, Math.min(max, value));
        int level = (int) ((currentValue - median) / stepHeight);
        return (numSteps - 1) - (level + (numSteps / 2));
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
