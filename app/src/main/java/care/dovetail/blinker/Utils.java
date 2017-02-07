package care.dovetail.blinker;

import android.graphics.BitmapFactory;
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

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
