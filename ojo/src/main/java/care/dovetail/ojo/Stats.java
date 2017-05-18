package care.dovetail.ojo;

import java.util.Arrays;

/**
 * Created by abhi on 9/22/16.
 */

public class Stats {
    private final static String TAG = "Stats";

    public final int count;

    public final int min;
    public final int minIndex;

    public final int max;
    public final int maxIndex;

    public final long sum;
    public final int average;
    public final int median;
    public final int percentile5;
    public final int percentile10;
    public final int percentile90;
    public final int percentile95;

    public final int stdDev;
    public final int zeroCrossings;
    public final float slope;

    public final int changes;

    public Stats(int source[]) {
        this(source, 0, source != null ? source.length : 0);
    }

    public Stats(int source[], int start, int length) {
        int values[] = null;
        if (source != null) {
            values = new int[Math.max(0, Math.min(length, source.length - start))];
            System.arraycopy(source, start, values, 0, values.length);
        }

        Stats basicStats = Stats.getBasicStats(values == null ? new int[0] : values);
        this.count = basicStats.count;
        this.min = basicStats.min;
        this.max = basicStats.max;
        this.minIndex = start + basicStats.minIndex;
        this.maxIndex = start + basicStats.maxIndex;
        this.sum = basicStats.sum;
        this.average = basicStats.average;
        this.changes = basicStats.changes;

        if (values == null || values.length == 0) {
            this.median = 0;
            this.percentile5 = 0;
            this.percentile10 = 0;
            this.percentile90 = 0;
            this.percentile95 = 0;
            this.stdDev = 0;
            this.zeroCrossings = 0;
            this.slope = 0;
            return;
        }
        int copyOfValues[] = values.clone();
        Arrays.sort(copyOfValues);
        this.median = copyOfValues[(int) Math.round(copyOfValues.length * 0.50)];
        this.percentile5 = copyOfValues[(int) Math.round(copyOfValues.length * 0.05)];
        this.percentile10 = copyOfValues[(int) Math.round(copyOfValues.length * 0.10)];
        this.percentile90 = copyOfValues[Math.min(
                (int) Math.round(copyOfValues.length * 0.90), copyOfValues.length - 1)];
        this.percentile95 = copyOfValues[Math.min(
                (int) Math.round(copyOfValues.length * 0.95), copyOfValues.length - 1)];
        this.stdDev = calculateStdDeviation(values, average);
        this.zeroCrossings = calculateZeroCrossings(values, median);
        this.slope = calculateSlope(values);
    }

    private Stats(int count, int min, int minIndex, int max, int maxIndex, long sum, int average,
                  int changes) {
        this.count = count;
        this.min = min;
        this.minIndex = minIndex;
        this.max = max;
        this.maxIndex = maxIndex;
        this.sum = sum;
        this.average = average;
        this.changes = changes;

        this.stdDev = 0;
        this.median = 0;
        this.percentile5 = 0;
        this.percentile10 = 0;
        this.percentile90 = 0;
        this.percentile95 = 0;
        this.slope = 0;
        this.zeroCrossings = 0;
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
        int average = values.length == 0 ? 0 : (int) (sum / values.length);
        return new Stats(values.length, min, minIndex, max, maxIndex, sum, average, changes);
    }

    public static float calculateSlope(int values[]) {
        int medianWindow = Math.max(1, values.length / 20); // 5% size for median window
        int start = calculateMedian(values, 0, medianWindow);
        int end = calculateMedian(values, values.length - medianWindow, medianWindow);
        return (end- start) / values.length;
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

    private static int calculateZeroCrossings(int values[], int median) {
        int zeroCrossings = 0;
        boolean lastOneHigh = true;
        for (int value : values) {
            boolean thisOneHigh = value > median;
            if (thisOneHigh != lastOneHigh) {
                zeroCrossings++;
            }
            lastOneHigh = thisOneHigh;
        }
        return zeroCrossings;
    }

    public static int random(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

}
