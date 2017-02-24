package care.dovetail.tracker.processing;

import android.util.Log;
import android.util.Pair;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

public class SignalProcessor3 implements SignalProcessor {
    private static final String TAG = "SignalProcessor3";

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

    private int hSlope;
    private int vSlope;

    private final int horizontal[] = new int[Config.GRAPH_LENGTH];
    private final int vertical[] = new int[Config.GRAPH_LENGTH];

    private final int hMedian[] = new int[Config.GRAPH_LENGTH];
    private final int vMedian[] = new int[Config.GRAPH_LENGTH];

//    private final int hMedianDiff[] = new int[Config.GRAPH_LENGTH];
//    private final int vMedianDiff[] = new int[Config.GRAPH_LENGTH];

    private final int hClean[] = new int[Config.GRAPH_LENGTH];
    private final int vClean[] = new int[Config.GRAPH_LENGTH];

    private int horizontalBase;
    private int verticalBase;

    private Pair<Integer, Integer> sector = new Pair<Integer, Integer>(2, 2);

    public SignalProcessor3(FeatureObserver observer, int numSteps) {
        this.numSteps = numSteps;
        this.observer = observer;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hSlope, vSlope);
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
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        System.arraycopy(hMedian, 1, hMedian, 0, hMedian.length - 1);
        hMedian[hMedian.length - 1] =
                Stats.calculateMedian(horizontal, horizontal.length - MEDIAN_WINDOW, MEDIAN_WINDOW);

        System.arraycopy(vMedian, 1, vMedian, 0, vMedian.length - 1);
        vMedian[vMedian.length - 1] =
                Stats.calculateMedian(vertical, vertical.length - MEDIAN_WINDOW, MEDIAN_WINDOW);

//        System.arraycopy(hMedianDiff, 1, hMedianDiff, 0, hMedianDiff.length - 1);
//        hMedianDiff[hMedianDiff.length - 1] =
//                hMedian[hMedian.length - 1] - hMedian[hMedian.length - 2];
//
//        System.arraycopy(vMedianDiff, 1, vMedianDiff, 0, vMedianDiff.length - 1);
//        vMedianDiff[vMedianDiff.length - 1] =
//                vMedian[vMedian.length - 1] - vMedian[vMedian.length - 2];

        hSlope = getSlope(hMedian);
        vSlope = getSlope(vMedian);

        removeDrift(hMedian, hClean, hSlope);
        removeDrift(vMedian, vClean, vSlope);

        hStats = new Stats(hClean, hClean.length - 100, 100);
        vStats = new Stats(vClean, vClean.length - 100, 100);

        horizontalBase = hStats.median;
        verticalBase = vStats.median;

        sector = getSector(hClean, vClean, numSteps, horizontalBase, verticalBase, halfGraphHeight);
    }

    @Override
    public int[] horizontal() {
        // return hMedian;
        return hClean;
    }

    @Override
    public int[] vertical() {
        // return vMedian;
        return vClean;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        // return Utils.calculateMinMax(hMedian);
        return Pair.create(horizontalBase - halfGraphHeight * 2, horizontalBase + halfGraphHeight * 2);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        // return Utils.calculateMinMax(vMedian);
        return Pair.create(verticalBase - halfGraphHeight * 2, verticalBase + halfGraphHeight * 2);
    }

    @Override
    public int[] blinks() {
        return new int[0];
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
        return Pair.create(0, 0);
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

    private static int getSlope(int values[]) {
        int start = Stats.calculateMedian(values, 0, Config.BLINK_WINDOW);
        int end = Stats.calculateMedian(
                values, values.length - Config.BLINK_WINDOW, Config.BLINK_WINDOW);
        return (start - end) / values.length;
    }

    private static void removeDrift(int source[], int destination[], int slope) {
        for (int i = 0; i < source.length && i < destination.length; i++) {
            destination[i] = source[i] + (slope * i);
        }
    }
}
