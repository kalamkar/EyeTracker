package care.dovetail.tracker.eog;

/**
 * Created by abhi on 4/10/17.
 */

public class RawBlinkDetector {

    private static final int MIN_BLINK_HEIGHT = 10000;

    private final int window[];

    public int blinkCenter = -1;

    private int blinkSkipWindowLength = 0;


    public RawBlinkDetector(int windowSize) {
        this.window = new int[windowSize];
    }

    public boolean check(int driftless) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = driftless;

        int blinkWindowCenter = -1;
        if (blinkSkipWindowLength <= 0) {
            blinkWindowCenter = hasBlink(window);
            if (blinkWindowCenter > 0) {
                blinkCenter = window.length - blinkWindowCenter;
                blinkSkipWindowLength = window.length - blinkWindowCenter;
            }
        } else {
            blinkSkipWindowLength--;
        }

        return blinkWindowCenter > 0;
    }

    private static int hasBlink(int window[]) {
        int maxIndex = -1;
        int minIndex = -1;
        int maxValue = Integer.MIN_VALUE;
        int minValue = Integer.MAX_VALUE;
        for (int i=0; i < window.length; i++) {
            if (window[i] > maxValue) {
                maxValue = window[i];
                maxIndex = i;
            }
            if (window[i] < minValue) {
                minValue = window[i];
                minIndex = i;
            }
        }

        boolean isMaxima = maxIndex != 0 && maxIndex != window.length - 1
                && window[maxIndex - 1] < window[maxIndex]
                && window[maxIndex] > window[maxIndex + 1];
        // base = data[:len(data) * 1/3] + data[len(data) * 2/3:]
        // boolean isTallEnough = maxValue - np.median(base) > MIN_BLINK_HEIGHT;
        boolean isTallEnough = maxValue - minValue > MIN_BLINK_HEIGHT;
        boolean isCentered = (window.length * 1/3) < maxIndex && maxIndex < (window.length * 2/3);

        return isTallEnough && isMaxima && isCentered ? maxIndex : -1;
    }

    public static void removeSpike(int data[], int size) {
        int end = data.length - 1;
        if (end <= 0) {
            return;
        }
        int start = Math.max(0, end - size);
        float slope = (data[end] - data[start]) / size;
        for (int i=start; i <= end; i++) {
            data[i] = data[start] + (int)(slope * (i - start));
        }
    }
}
