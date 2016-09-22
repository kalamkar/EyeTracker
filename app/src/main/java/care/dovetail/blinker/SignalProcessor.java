package care.dovetail.blinker;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final int BLINK_WINDOW = 20;
    private static final int LENGTH_FOR_MEDIAN = BLINK_WINDOW * 3;

    private static final float BLINK_HEIGHT_TOLERANCE = 0.65f;
    private static final float BLINK_BASE_TOLERANCE = 0.40f;
    private static final int MAX_RECENT_BLINKS = 10;
    private static final int MIN_RECENT_BLINKS = 10;

    private final FeatureObserver observer;

    private int halfGraphHeight = (int) (Math.pow(2, 24) * 0.001);

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];

    private int median1;
    private int median2;

    private int recentMedian1;
    private int recentMedian2;

    private final int positions1[] = new int[Config.GRAPH_LENGTH];
    private final int positions2[] = new int[Config.GRAPH_LENGTH];

    private final List<Feature> recentBlinks = new ArrayList<>(MAX_RECENT_BLINKS);

    private int featureProcessCounter = 0;
    private final Set<Feature> features = new HashSet<Feature>();

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    public SignalProcessor(FeatureObserver observer) {
        this.observer = observer;
    }


    public synchronized void update(int[] chunk1, int[] chunk2) {
        System.arraycopy(values1, chunk1.length, values1, 0, values1.length - chunk1.length);
        System.arraycopy(chunk1, 0, values1, values1.length - chunk1.length, chunk1.length);

        System.arraycopy(values2, chunk2.length, values2, 0, values2.length - chunk2.length);
        System.arraycopy(chunk2, 0, values2, values2.length - chunk2.length, chunk2.length);

        median1 = Utils.calculateMedian(values1);
        median2 = Utils.calculateMedian(values2);

        recentMedian1 = Utils
                .calculateMedian(values1, values1.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        recentMedian2 = Utils
                .calculateMedian(values2, values2.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);

        int stepHeight = (int) (halfGraphHeight * 0.4) / 3;
        updateStepPositions(positions1, chunk1, stepHeight,
                recentMedian1, recentMedian1 - halfGraphHeight, recentMedian1 + halfGraphHeight);
        updateStepPositions(positions2, chunk2, stepHeight,
                recentMedian2, recentMedian2 - halfGraphHeight, recentMedian2 + halfGraphHeight);

        checkAndPostBlink();

        if (featureProcessCounter == BLINK_WINDOW) {
            featureProcessCounter = 0;
            processFeatures();
        } else {
            featureProcessCounter++;
        }
    }

    public int[] channel1() {
        return values1;
    }

    public int[] channel2() {
        return values2;
    }

    public int median1() {
        return median1;
    }

    public int median2() {
        return median2;
    }

    public int getSector() {
        return (int) (Math.random() * 10);
    }

    public Pair<Integer, Integer> range1() {
        return Pair.create(median1 - halfGraphHeight, median1 + halfGraphHeight);
    }

    public Pair<Integer, Integer> range2() {
        return Pair.create(median2 - halfGraphHeight, median2 + halfGraphHeight);
    }

    public int[] positions1() {
        return positions1;
    }

    public int[] positions2() {
        return positions2;
    }

    public synchronized Set<Feature> getFeatures() {
        Set<Feature> subset = new HashSet<>();
        for (Feature fp : features) {
            subset.add(fp);
        }
        return subset;
    }

    private synchronized void checkAndPostBlink() {
        int minSpikeHeight = (int) (Utils.calculateMedianHeight(recentBlinks) * BLINK_HEIGHT_TOLERANCE);
        if (recentBlinks.size() < MIN_RECENT_BLINKS) {
            Pair<Integer, Integer> minMax = Utils.calculateMinMax(values2);
            minSpikeHeight = (int) (Math.abs(minMax.second - recentMedian2) * BLINK_HEIGHT_TOLERANCE);
        }
        int last = values2.length - 1;
        int middle = last - (BLINK_WINDOW / 2);
        int first = last - BLINK_WINDOW + 1;
        if (isBlink(values2[first], values2[middle - 1], values2[middle], values2[middle + 1],
                values2[last], minSpikeHeight)) {
            Feature blink = new Feature(Feature.Type.BLINK, middle, values2[middle],
                    Feature.Channel.VERTICAL);
            blink.height = Math.min(values2[middle] - values2[last], values2[middle] - values2[first])
                    + (Math.abs(values2[last] - values2[first]) / 2);
            blink.startIndex = first;
            blink.endIndex = last;
            onFeature(blink);
        }
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

    private synchronized void processFeatures() {
        features.clear();
        int maxBlinkHeight = 0;
        if (recentBlinks.size() >= MIN_RECENT_BLINKS) {
            maxBlinkHeight = Utils.calculateMedianHeight(recentBlinks);
        } else {
            Pair<Integer, Integer> minMax = Utils.calculateMinMax(values2);
            maxBlinkHeight = Math.abs(minMax.second - recentMedian2);
        }
        features.addAll(findBlinks(values2, maxBlinkHeight, Feature.Channel.VERTICAL));
    }

    private static Set<Feature> findBlinks(int values[], int maxBlinkHeight,
                                           Feature.Channel channel) {
        Set<Feature> blinks = new HashSet<>();
        // Spike height should be within tolerance level of max blink height
        int minSpikeHeight = (int) (maxBlinkHeight * BLINK_HEIGHT_TOLERANCE);
        for (int i = 0; i < values.length - BLINK_WINDOW; i++) {
            int middle = i + (BLINK_WINDOW / 2);
            int last =  i + BLINK_WINDOW - 1;
            if (isBlink(values[i], values[middle - 1], values[middle], values[middle + 1],
                    values[last], minSpikeHeight)) {
                Feature blink = new Feature(Feature.Type.BLINK, middle, values[middle], channel);
                blink.height = Math.min(values[middle] - values[last], values[middle] - values[i])
                        + (Math.abs(values[last] - values[i]) / 2);
                blink.startIndex = i;
                blink.endIndex = last;
                blinks.add(blink);
            }
        }
        return blinks;
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

    private static void updateStepPositions(int positions[], int chunk[], int stepHeight,
                                            int median, int min, int max) {
        int currentValue1 = Math.max(min, Math.min(max, Utils.calculateMedian(chunk)));
        System.arraycopy(positions, chunk.length, positions, 0, positions.length - chunk.length);
        for (int i = positions.length - chunk.length; i < positions.length; i++) {
            int level = (currentValue1 - median) / stepHeight;
            positions[i] = median + level * stepHeight;
        }
    }

}
