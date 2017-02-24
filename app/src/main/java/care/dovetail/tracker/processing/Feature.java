package care.dovetail.tracker.processing;

import java.util.Arrays;

import care.dovetail.tracker.Config;

/**
 * Created by abhi on 9/21/16.
 */

public class Feature {

    public enum Type {
        BLINK,
        SMALL_BLINK,
        TRIPLE_BLINK,
        BAD_SIGNAL
    }

    public enum Channel {
        LEFT,
        RIGHT,
        VERTICAL,
        HORIZONTAL,
        NONE,
        ALL
    }

    public final Type type;
    public final int startIndex;
    public final int endIndex;
    public final int values[];
    public Channel channel;
    public int confidence;

    public Feature(Type type, int startIndex, int endIndex, int values[]) {
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format("%s start = %d, end = %d values = %s", type, startIndex, endIndex,
                Arrays.toString(values));
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
                return new Feature(Type.SMALL_BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            } else if (height > minBlinkHeight && height < maxBlinkHeight) {
                return new Feature(Type.BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            }
        }
        return null;
    }
}
