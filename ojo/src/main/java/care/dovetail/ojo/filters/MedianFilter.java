package care.dovetail.ojo.filters;

import care.dovetail.ojo.RawBlinkDetector;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/16/17.
 */

public class MedianFilter implements Filter {
    private final int window[];

    public MedianFilter(int windowSize) {
        this.window = new int[windowSize];
    }

    @Override
    public int filter(int raw) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = raw;
        return Stats.calculateMedian(window, 0, window.length);
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
