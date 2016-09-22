package care.dovetail.blinker;

import android.util.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Created by abhi on 9/22/16.
 */

public class Utils {
    public static Pair<Integer, Integer> calculateMinMax(int values[]) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return Pair.create(min, max);
    }

    public static int calculateMedianHeight(List<Feature> features) {
        if (features.size() == 0) {
            return 0;
        }
        int copyOfValues[] = new int[features.size()];
        for (int i = 0; i < copyOfValues.length; i++) {
            copyOfValues[i] = features.get(i).height;
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
        int copyOfValues[] = new int[length];
        System.arraycopy(values, start, copyOfValues, 0, length);
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }
}
