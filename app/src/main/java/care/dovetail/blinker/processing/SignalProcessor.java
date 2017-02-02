package care.dovetail.blinker.processing;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.blinker.Config;
import care.dovetail.blinker.Utils;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final double SAMPLING_FREQ  = 200.0;

    private static final double LOW_FREQUENCY = 0.5;
    private static final double HIGH_FREQUENCY = 3.0;
    private static final int FILTER_ORDER = 2;

    private static final float MAX_GAZE_TO_BLINK_RATIO = 1.0f;

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_BLINK_MEDIAN = BLINK_WINDOW * 3;
    private static final int LENGTH_FOR_MEDIAN =  500;

    private static final int MAX_RECENT_BLINKS = 10;
    private static final int MIN_RECENT_BLINKS = 5;

    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    private final FeatureObserver observer;

    private int halfGraphHeight = 4000;

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];
    private final int feature1[] = new int[Config.GRAPH_LENGTH];
    private final int feature2[] = new int[Config.GRAPH_LENGTH];

    private int median1;
    private int median2;
    private int medianForBlink;

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
    private int lastBlinkIndex = -1;
    private int lastBlinkHeight = 0;

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(0, 0);

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    public SignalProcessor(FeatureObserver observer) {
        this.observer = observer;
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

        lastBlinkIndex--;

        median1 = Utils.calculateMedian(
                values1, values1.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        median2 = Utils.calculateMedian(
                values2, values2.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        medianForBlink = Utils.calculateMedian(
                blinks, blinks.length - LENGTH_FOR_BLINK_MEDIAN, LENGTH_FOR_BLINK_MEDIAN);

        Feature blink = maybeGetBlink(blinks);
        if (blink != null) {
            if (blink.startIndex - BLINK_WINDOW > lastBlinkIndex) {
                // New blink outside of last blink window
                onFeature(blink);
            } else if (lastBlinkHeight < (blink.values[0] - blink.values[1])) {
                // New blink within last blink's window but biggger than last blink.
                // Remove smaller last blink and add new blink.
                Feature removedBlink = recentBlinks.remove(recentBlinks.size() - 1);
                feature1[removedBlink.startIndex] = 0;
                feature2[removedBlink.endIndex] = 0;
                onFeature(blink);
            }
            // else ignore the new blink with height less than last blink height and within window
        }

        int horizLevel = getLevel(values1[values1.length - 1], median1, halfGraphHeight);
        int vertLevel = getLevel(values2[values2.length - 1], median2, halfGraphHeight);
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
        return Pair.create(median1 - halfGraphHeight * 2, median1 + halfGraphHeight * 2);
    }

    public Pair<Integer, Integer> range2() {
         return Pair.create(median2 - halfGraphHeight * 2, median2 + halfGraphHeight * 2);
    }

    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(medianForBlink - MAX_BLINK_HEIGHT, medianForBlink + MAX_BLINK_HEIGHT);
    }

    public Pair<Integer, Integer> getSector() {
        return sector;
    }

    private void onFeature(Feature feature) {
        if (feature.type == Feature.Type.BLINK) {
            // Use vertical channel values for blink height for gaze calculations. They are more
            // relevant than the blink channel which are much higher.
            int max = Math.max(values2[feature.startIndex], values2[feature.endIndex]);
            int min = Math.min(values2[feature.startIndex], values2[feature.endIndex]);
            Feature blink = new Feature(feature.type, feature.startIndex, feature.endIndex,
                    new int[]{max, min});

            lastBlinkHeight = max - min;
            lastBlinkIndex = blink.endIndex;
            recentBlinks.add(blink);
            if (recentBlinks.size() > MAX_RECENT_BLINKS) {
                recentBlinks.remove(0);
            }

            if (recentBlinks.size() >= MIN_RECENT_BLINKS) {
                halfGraphHeight = (int) (((float) Utils.calculateMedianHeight(recentBlinks))
                        * MAX_GAZE_TO_BLINK_RATIO);
            }

            feature1[blink.startIndex] = blink.values[0];
            feature2[blink.endIndex] = blink.values[1];
        }

        observer.onFeature(feature);
    }

    private static int getLevel(int value, int median, int halfGraphHeight) {
        float stepHeight = halfGraphHeight / (Config.NUM_STEPS / 2); // divide by 2 as values  +ve and -ve
        int min = median - halfGraphHeight;
        int max = median + halfGraphHeight;
        int currentValue = Math.max(min, Math.min(max, value));
        int level = (int) ((currentValue - median) / stepHeight);
        return (Config.NUM_STEPS - 1) - (level + (Config.NUM_STEPS / 2));
    }

    private static Feature maybeGetBlink(int values[]) {
        int last = values.length - 1;
        int first = last - BLINK_WINDOW + 1;

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
        if (height > MIN_BLINK_HEIGHT && height < MAX_BLINK_HEIGHT
                && localMaxima && localMinima) {
            return new Feature(Feature.Type.BLINK, Math.min(minIndex, maxIndex),
                    Math.max(minIndex, maxIndex),
                    new int[] {values[maxIndex], values[minIndex]});
        }
        return null;
    }
}
