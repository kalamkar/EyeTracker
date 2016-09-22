package care.dovetail.blinker;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_MEDIAN = BLINK_WINDOW * 3;

    private static final float BLINK_HEIGHT_TOLERANCE = 0.65f;
    private static final float BLINK_BASE_TOLERANCE = 0.40f;
    private static final int MAX_RECENT_BLINKS = 10;
    private static final int MIN_RECENT_BLINKS = 10;
    private static final int NUM_STEPS = 3;

    private final FeatureObserver observer;

    private int halfGraphHeight = (int) (Math.pow(2, 24) * 0.001);

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];

    private int median1;
    private int median2;

    private int recentMedian1;
    private int recentMedian2;

    private final List<Feature> recentBlinks = new ArrayList<>(MAX_RECENT_BLINKS);

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    public SignalProcessor(FeatureObserver observer) {
        this.observer = observer;
    }


    public synchronized void update(int channel1, int channel2) {
        System.arraycopy(values1, 1, values1, 0, values1.length - 1);
        values1[values1.length - 1] = channel1;

        System.arraycopy(values2, 1, values2, 0, values2.length - 1);
        values2[values2.length - 1] = channel2;

        median1 = Utils.calculateMedian(values1);
        median2 = Utils.calculateMedian(values2);

        recentMedian1 = Utils
                .calculateMedian(values1, values1.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        recentMedian2 = Utils
                .calculateMedian(values2, values2.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);

        Feature blink = maybeGetBlink(values2, getMinSpikeHeight());
        if (blink != null) {
            onFeature(blink);
        }
    }

    public int[] channel1() {
        return values1;
    }

    public int[] channel2() {
        return values2;
    }

    public Pair<Integer, String> getSector() {
        int level1 = getLevel(values1[values1.length - 1], median1, halfGraphHeight);
        int level2 = getLevel(values2[values2.length - 1], median2, halfGraphHeight);
        String levels = String.format("%d,%d", level1, level2);
        level1 = level1 > 1 ? 1 : level1 < -1 ? -1 : level1;
        level2 = level2 > 1 ? 1 : level2 < -1 ? -1 : level2;
        int sectors[][] = new int[][] {{8, 5, 2}, {7, 4, 1}, {6, 3, 0}};
        return Pair.create(sectors[level1 + 1][level2 + 1], levels);
    }

    public Pair<Integer, Integer> range1() {
        return Pair.create(median1 - halfGraphHeight, median1 + halfGraphHeight);
    }

    public Pair<Integer, Integer> range2() {
        return Pair.create(median2 - halfGraphHeight, median2 + halfGraphHeight);
    }

    public int[] positions1() {
        return getStepPositions(values1, median1, halfGraphHeight);
    }

    public int[] positions2() {
        return getStepPositions(values2, median2, halfGraphHeight);
    }

    private void onFeature(Feature feature) {
        if (feature.type == Feature.Type.BLINK) {
            recentBlinks.add(feature);
            if (recentBlinks.size() > MAX_RECENT_BLINKS) {
                recentBlinks.remove(0);
            }

            if (recentBlinks.size() >= MIN_RECENT_BLINKS) {
                halfGraphHeight = (int) (Utils.calculateMedianHeight(recentBlinks) * 1.2);
            }
        }

        observer.onFeature(feature);
    }

    private int getMinSpikeHeight() {
        if (recentBlinks.size() < MIN_RECENT_BLINKS) {
            Pair<Integer, Integer> minMax = Utils.calculateMinMax(values2);
            return (int) (Math.abs(minMax.second - recentMedian2) * BLINK_HEIGHT_TOLERANCE);
        }
        return (int) (Utils.calculateMedianHeight(recentBlinks) * BLINK_HEIGHT_TOLERANCE);
    }

    private static int getLevel(int value, int median, int halfGraphHeight) {
        int stepHeight = halfGraphHeight / NUM_STEPS;
        int min = median - halfGraphHeight;
        int max = median + halfGraphHeight;
        int currentValue = Math.max(min, Math.min(max, value));
        return Math.round((currentValue - median) / stepHeight);
    }

    private static int[] getStepPositions(int values[], int median, int halfGraphHeight) {
        int positions[] = new int[values.length];
        for (int i = 0; i < positions.length; i++) {
            int level = getLevel(values[i], median, halfGraphHeight);
            positions[i] = median + level * (halfGraphHeight / NUM_STEPS);
        }
        return positions;
    }

    private static Feature maybeGetBlink(int values[], int minSpikeHeight) {
        int last = values.length - 1;
        int middle = last - (BLINK_WINDOW / 2);
        int first = last - BLINK_WINDOW + 1;
        if (isBlink(values[first], values[middle - 1], values[middle], values[middle + 1],
                values[last], minSpikeHeight)) {
            Feature blink = new Feature(Feature.Type.BLINK, middle, values[middle],
                    Feature.Channel.VERTICAL);
            blink.height = Math.min(values[middle] - values[last], values[middle] - values[first])
                    + (Math.abs(values[last] - values[first]) / 2);
            blink.startIndex = first;
            blink.endIndex = last;
            return blink;
        }
        return null;
    }

    private static boolean isBlink(int first, int beforeMiddle, int middle, int afterMiddle,
                                   int last, int minSpikeHeight) {
        // If middle is peak AND middle height from left base or right base is more than
        // min spike height AND difference between left and right base is within tolerance
        boolean isPeak = (beforeMiddle < middle) && (middle > afterMiddle);
        int leftHeight = middle - first;
        int rightHeight = middle - last;
        boolean isBigEnough = (leftHeight > minSpikeHeight) || (rightHeight > minSpikeHeight);
        int minBaseDifference = (int) (Math.max(leftHeight, rightHeight) * BLINK_BASE_TOLERANCE);
        boolean isFlat = Math.abs(last - first) < minBaseDifference;
        return isPeak && isBigEnough && isFlat;
    }
}
