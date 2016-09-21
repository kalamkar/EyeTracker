package care.dovetail.blinker;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final int LENGTH_FOR_MEDIAN = Config.GRAPH_LENGTH;

    private static final int BLINK_WINDOW = 20;

    private int halfGraphHeight = (int) (Math.pow(2, 24) * 0.001);
    private int stepHeight = (int) (halfGraphHeight * 0.4) / 3;

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];

    private int median1;
    private int median2;

    private final int positions1[] = new int[Config.GRAPH_LENGTH];
    private final int positions2[] = new int[Config.GRAPH_LENGTH];

    private int featureProcessCounter = 0;
    private final Set<Feature> features = new HashSet<Feature>();


    public synchronized void update(int[] chunk1, int[] chunk2) {
        System.arraycopy(values1, chunk1.length, values1, 0, values1.length - chunk1.length);
        System.arraycopy(chunk1, 0, values1, values1.length - chunk1.length, chunk1.length);

        System.arraycopy(values2, chunk2.length, values2, 0, values2.length - chunk2.length);
        System.arraycopy(chunk2, 0, values2, values2.length - chunk2.length, chunk2.length);

        median1 = calculateMedian(values1, values1.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);
        median2 = calculateMedian(values2, values2.length - LENGTH_FOR_MEDIAN, LENGTH_FOR_MEDIAN);

        int currentValue1 = Math.max(median1 - halfGraphHeight,
                Math.min(median1 + halfGraphHeight, calculateMedian(chunk1)));
        System.arraycopy(positions1, chunk1.length, positions1, 0, positions1.length - chunk1.length);
        for (int i = positions1.length - chunk1.length; i < positions1.length; i++) {
            int level = (currentValue1 - median1) / stepHeight;
            positions1[i] = median1 + level * stepHeight;
        }

        int currentValue2 = Math.max(median2 - halfGraphHeight,
                Math.min(median2 + halfGraphHeight, calculateMedian(chunk2)));
        System.arraycopy(positions2, chunk2.length, positions2, 0, positions2.length - chunk2.length);
        for (int i = positions2.length - chunk2.length; i < positions2.length; i++) {
            int level = (currentValue2 - median2) / stepHeight;
            positions2[i] = median2 + level * stepHeight;
        }

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

    public synchronized List<Feature> getFeatures(Feature.Type type, Feature.Channel channel) {
        List<Feature> subset = new ArrayList<Feature>();
        for (Feature fp : features) {
            if (type != null && type.equals(fp.type)
                    && channel != null && channel.equals(fp.channel)) {
                subset.add(fp);
            }
        }
        return subset;
    }

    private synchronized void processFeatures() {
        features.addAll(findBlinks(values1, median1, Feature.Channel.LEFT));
        features.addAll(findBlinks(values2, median2, Feature.Channel.RIGHT));
    }

    private static Set<Feature> findBlinks(int values[], int median, Feature.Channel channel) {
        Set<Feature> blinks = new HashSet<>();
        Pair<Integer, Integer> minMax = calculateMinMax(values);
        // Spike height should be within 85% of max (and median difference)
        int minSpikeHeight = (int) (Math.abs(minMax.second - median) * 0.85);
        for (int i = 0; i < values.length - BLINK_WINDOW; i++) {
            int middle = i + (BLINK_WINDOW / 2);
            int last =  i + BLINK_WINDOW - 1;
            // If middle is peak AND middle height from left base or right base is more than
            // min spike height AND difference between left and right base is within tolerance
            boolean isPeak = (values[middle - 1] < values[middle])
                    && (values[middle] > values[middle + 1]);
            int leftHeight = Math.abs(values[middle] - values[i]);
            int rightHeight = Math.abs(values[middle] - values[last]);
            boolean isBigEnough = (leftHeight > minSpikeHeight) || (rightHeight > minSpikeHeight);
            int minBaseDifference = (int) (Math.max(leftHeight, rightHeight) * 0.20);
            boolean isFlat = Math.abs(values[last] - values[i]) < minBaseDifference;
            if (isPeak && isBigEnough && isFlat) {
                Feature blink = new Feature(Feature.Type.BLINK, middle, values[middle], channel);
                blink.startIndex = i;
                blink.endIndex = last;
                blinks.add(blink);
            }
        }
        return blinks;
    }

    private static Pair<Integer, Integer> calculateMinMax(int values[]) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return Pair.create(min, max);
    }

    private static int calculateMedian(int values[]) {
        int copyOfValues[] = values.clone();
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }

    private static int calculateMedian(int values[], int start, int length) {
        int copyOfValues[] = new int[length];
        System.arraycopy(values, start, copyOfValues, 0, length);
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }
}
