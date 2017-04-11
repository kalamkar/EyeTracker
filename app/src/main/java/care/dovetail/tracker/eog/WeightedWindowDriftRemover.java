package care.dovetail.tracker.eog;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Created by abhi on 4/11/17.
 */

public class WeightedWindowDriftRemover {

    private final int window[];
    private final double windowMask[];

    public WeightedWindowDriftRemover(int windowSize) {
        this.window = new int[windowSize];
        double xx[] = linspace(-3, 0, windowSize);
        this.windowMask = getWindowMask(xx);
    }

    public int update(int value) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = value;

        double sum = 0;
        for (int i = 0; i < window.length && i < windowMask.length; i++) {
            sum += windowMask[i] * window[i];
        }
        int meanEstimate = (int) (sum / window.length);
        return value - meanEstimate;
    }

    private static double[] linspace(double start, double end, int length) {
        double vector[] = new double[length];
        double step = (end - start) / length;
        for (int i = 0; i < length; i++) {
            vector[i] = start + (i * step);
        }
        return vector;
    }

    private static double[] getWindowMask(double xx[]) {
        double windowMask[] = new double[xx.length];
        NormalDistribution function = new NormalDistribution();
        double sum = 0;
        for (int i = 0; i < xx.length && i < windowMask.length; i++) {
            windowMask[i] = function.density(xx[i]);
            sum += windowMask[i];
        }
        for (int i = 0; i < windowMask.length; i++) {
            windowMask[i] = windowMask.length * windowMask[i] / sum;
        }
        return windowMask;
    }
}
