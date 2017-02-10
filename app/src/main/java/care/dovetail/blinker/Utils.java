package care.dovetail.blinker;

import android.util.Pair;

import java.util.Arrays;
import java.util.List;

import care.dovetail.blinker.processing.Feature;

/**
 * Created by abhi on 9/22/16.
 */

public class Utils {
    public static Pair<Integer, Integer> calculateMinMax(int values[]) {
        return calculateMinMax(values, 0, values.length);
    }

    public static Pair<Integer, Integer> calculateMinMax(int values[], int start, int length) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = start; i < start + length && i < values.length; i++) {
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
        }
        return Pair.create(min, max);
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

    public static int calculateMedian(int values[]) {
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

    public static int calculateStdDeviation(int values[]) {
        double total = 0;
        for (int value : values) {
            total += value;
        }
        int mean = (int) (total / values.length);
        total = 0;
        for (int value : values) {
            total += Math.pow(Math.abs(value - mean), 2);
        }
        return (int) Math.sqrt(total / values.length);
    }
}
