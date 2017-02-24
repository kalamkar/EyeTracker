package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

public class SignalProcessor1 implements SignalProcessor {
    private static final String TAG = "SignalProcessor1";

    private static final double LOW_FREQUENCY = 0.5;
    private static final double HIGH_FREQUENCY = 3.0;
    private static final int FILTER_ORDER = 2;

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_BLINK_MEDIAN = BLINK_WINDOW * 3;
    private static final int LENGTH_FOR_MEDIAN =  500;

    private static final int MAX_RECENT_BLINKS = 20;
    private static final int MIN_RECENT_BLINKS = 5;

    private static final int SMALL_BLINK_HEIGHT = 4000;
    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    private static final int MAX_GRAPH_HEIGHT = 2200;
    private static final int MIN_GRAPH_HEIGHT = 1800;
    private static final float MAX_HEIGHT_CHANGE = 0.25f;

    private static final int MIN_SIGNAL_QUALITY_FOR_BLINK_CALIBRATION = 95;

    private final int numSteps;
    private final static float BLINK_TO_GAZE_MULTIPLIER = 0.8f;
    private final static float VERTICAL_TO_HORIZONTAL_MULTIPLIER = 0.6f;
    private final FeatureObserver observer;

    private int halfGraphHeight = 2000;
    private int blinkWindowIndex = 0;
    private int numBlinks = 0;

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];
    private final int blinks[] = new int[Config.GRAPH_LENGTH];
    private final int feature1[] = new int[Config.GRAPH_LENGTH];
    private final int feature2[] = new int[Config.GRAPH_LENGTH];

    private Stats hStats = new Stats(null);
    private Stats vStats = new Stats(null);

    private int horizontalBase;
    private int verticalBase;
    private int blinkBaseline;

    private final int horizontalBaseline[] = new int[MAX_RECENT_BLINKS];
    private final int verticalBaseline[] = new int[MAX_RECENT_BLINKS];

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            LOW_FREQUENCY / Config.SAMPLING_FREQ, HIGH_FREQUENCY / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, FILTER_ORDER, 0,
            LOW_FREQUENCY / Config.SAMPLING_FREQ, HIGH_FREQUENCY / Config.SAMPLING_FREQ));

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.bessel, 1 /* order */, 0,
            4.0 / Config.SAMPLING_FREQ, 10.0 / Config.SAMPLING_FREQ));

    private final List<Feature> recentBlinks = new ArrayList<>(MAX_RECENT_BLINKS);

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(0, 0);

    public SignalProcessor1(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", numBlinks, getSignalQuality());
    }

    @Override
    public int getSignalQuality() {
        // (halfGraphHeight/3) is 5% signal quality loss
        int maxStdDev = 100 * (halfGraphHeight / 3) / 5;
        int horizLoss = 100 * Math.abs(hStats.stdDev - (halfGraphHeight / 3)) / maxStdDev;
        int vertLoss = 100 * Math.abs(vStats.stdDev - (halfGraphHeight / 3)) / maxStdDev;
        return 100 - Math.min(100, Math.max(horizLoss, vertLoss));
    }

    @Override
    public synchronized void update(int hValue, int vValue) {
        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = (int) hFilter.step(hValue);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = (int) vFilter.step(vValue);

        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(vValue);

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = 0;

        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        feature2[feature2.length - 1] = 0;

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        if (recentBlinks.size() < MIN_RECENT_BLINKS) {
            horizontalBase = hStats.median;
            verticalBase = vStats.median;
        }
        blinkBaseline = Stats.calculateMedian(
                blinks, blinks.length - LENGTH_FOR_BLINK_MEDIAN, LENGTH_FOR_BLINK_MEDIAN);

        if (++blinkWindowIndex == BLINK_WINDOW) {
            blinkWindowIndex = 0;
            Feature blink = Stats.maybeGetBlink(blinks, SMALL_BLINK_HEIGHT, MIN_BLINK_HEIGHT,
                    MAX_BLINK_HEIGHT);
            if (blink != null) {
                onFeature(blink);
            }
        }

        int horizLevel = getLevel(horizontal[horizontal.length - 1], numSteps, horizontalBase,
                (int) (halfGraphHeight * VERTICAL_TO_HORIZONTAL_MULTIPLIER));
        int vertLevel = getLevel(vertical[vertical.length - 1], numSteps, verticalBase,
                halfGraphHeight);
        sector = Pair.create(horizLevel, vertLevel);
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
                horizontalBase - halfGraphHeight * 2, horizontalBase + halfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
         return Pair.create(
                 verticalBase - halfGraphHeight * 2, verticalBase + halfGraphHeight * 2);
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
        if (feature.type == Feature.Type.SMALL_BLINK || feature.type == Feature.Type.BLINK) {
            numBlinks++;
            // Use vertical channel values for blink height for gaze calculations. They are more
            // relevant than the blink channel which are much higher.
            Stats blinkStatsVertical =
                    new Stats(vertical, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);
            Feature blink = new Feature(feature.type, feature.startIndex, feature.endIndex,
                    new int[]{blinkStatsVertical.min, blinkStatsVertical.max});
            Stats blinkStatsHorizontal =
                    new Stats(horizontal, feature.startIndex - BLINK_WINDOW / 2, BLINK_WINDOW * 2);

            System.arraycopy(verticalBaseline, 1, verticalBaseline, 0, verticalBaseline.length - 1);
            verticalBaseline[verticalBaseline.length - 1] = blinkStatsVertical.min;

            System.arraycopy(horizontalBaseline, 1, horizontalBaseline, 0,
                    horizontalBaseline.length - 1);
            horizontalBaseline[horizontalBaseline.length - 1] = blinkStatsHorizontal.min;

            verticalBase = new Stats(verticalBaseline).median;
            horizontalBase = new Stats(horizontalBaseline).median;

            if (getSignalQuality() >= MIN_SIGNAL_QUALITY_FOR_BLINK_CALIBRATION) {
                recentBlinks.add(blink);
                if (recentBlinks.size() > MAX_RECENT_BLINKS) {
                    recentBlinks.remove(0);
                }
            }

            if (recentBlinks.size() >= MIN_RECENT_BLINKS) {
                int newHalfGraphHeight = (int) (((float) calculateMedianHeight(recentBlinks))
                        * BLINK_TO_GAZE_MULTIPLIER);
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
                // Limit half graph height between MIN_GRAPH_HEIGHT and MAX_GRAPH_HEIGHT
                halfGraphHeight =
                        Math.max(MIN_GRAPH_HEIGHT, Math.min(halfGraphHeight, MAX_GRAPH_HEIGHT));
            }

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

    public static int calculateMedianHeight(List<Feature> features) {
        if (features.size() == 0) {
            return 0;
        }
        int copyOfValues[] = new int[features.size()];
        for (int i = 0; i < copyOfValues.length; i++) {
            Feature feature = features.get(i);
            copyOfValues[i] = feature.values[0] - feature.values[1];
        }
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }
}
