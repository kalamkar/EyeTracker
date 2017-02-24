package care.dovetail.tracker;

import java.util.Arrays;

import care.dovetail.tracker.processing.Feature;

/**
 * Created by abhi on 9/22/16.
 */

public class Stats {
    private final static String TAG = "Stats";

    private final int values[];

    public final int min;
    public final int minIndex;

    public final int max;
    public final int maxIndex;

    public final long sum;
    public final int average;
    public final int median;

    public final int stdDev;

    public final float slope;

    public final int changes;

    public Stats(int source[]) {
        this(source, 0, source != null ? source.length : 0);
    }

    public Stats(int source[], int start, int length) {
        if (source != null) {
            values = new int[Math.max(0, Math.min(length, source.length - start))];
            System.arraycopy(source, start, values, 0, values.length);
        } else {
            values = new int[0];
        }

        Stats basicStats = Stats.getBasicStats(values);
        this.min = basicStats.min;
        this.max = basicStats.max;
        this.minIndex = start + basicStats.minIndex;
        this.maxIndex = start + basicStats.maxIndex;
        this.sum = basicStats.sum;
        this.average = basicStats.average;
        this.changes = basicStats.changes;

        this.median = calculateMedian(values);
        this.stdDev = calculateStdDeviation(values, average);
        this.slope = calculateSlope(values);
    }

    private Stats(int min, int minIndex, int max, int maxIndex, long sum, int average, int changes) {
        values = new int[0];
        this.min = min;
        this.minIndex = minIndex;
        this.max = maxIndex;
        this.maxIndex = maxIndex;
        this.sum = sum;
        this.average = average;
        this.changes = changes;

        this.stdDev = 0;
        this.median = 0;
        this.slope = 0;
    }

    private static Stats getBasicStats(int values[]) {
        int min = Integer.MAX_VALUE;
        int minIndex = 0;
        int max = Integer.MIN_VALUE;
        int maxIndex = 0;
        long sum = 0;
        int changes = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i];
            if (values[i] < min) {
                min = values[i];
                minIndex = i;
            }
            if (values[i] > max) {
                max = values[i];
                maxIndex = i;
            }
            changes += i == 0 || values[i] == values[i - 1] ? 0 : 1;
        }
        return new Stats(min, minIndex, max, maxIndex, sum, (int) (sum / values.length), changes);
    }

    private static float calculateSlope(int values[]) {
        int medianWindow = Math.min(1, values.length / 20); // 5% size for median window
        int start = calculateMedian(values, 0, medianWindow);
        int end = calculateMedian(values, values.length - medianWindow, medianWindow);
        return (start - end) / values.length;
    }

    private static int calculateMedian(int values[]) {
        int copyOfValues[] = values.clone();
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }

    public static int calculateMedian(int values[], int start, int length) {
        int copyOfValues[] = new int[Math.min(length, values.length - start)];
        System.arraycopy(values, start, copyOfValues, 0, copyOfValues.length);
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }

    private static int calculateStdDeviation(int values[], int mean) {
        double total = 0;
        for (int value : values) {
            total += Math.pow(Math.abs(value - mean), 2);
        }
        return (int) Math.sqrt(total / values.length);
    }

    public static int random(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    public static Feature maybeGetBlink(int values[], int smallBlinkHeight, int minBlinkHeight,
                                        int maxBlinkHeight) {
        int last = values.length - 1;
        int first = Math.max(0, last - (Config.BLINK_WINDOW * 2) + 1);

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
            if (height > smallBlinkHeight && height < minBlinkHeight) {
                return new Feature(Feature.Type.SMALL_BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            } else if (height > minBlinkHeight && height < maxBlinkHeight) {
                return new Feature(Feature.Type.BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            }
        }
        return null;
    }
}
