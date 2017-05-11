package care.dovetail.tracker.eog.filters;

import care.dovetail.tracker.Stats;
import care.dovetail.tracker.eog.RawBlinkDetector;

/**
 * Created by abhi on 4/10/17.
 */

public class FixedWindowSlopeRemover implements Filter {

    private final int window[];

    private float currentSlope = 0;

    private int shift = 0;

    private long countSinceUpdate = 0;

    public FixedWindowSlopeRemover(int windowSize) {
        this.window = new int[windowSize];
    }

    @Override
    public int filter(int raw) {
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = raw;

        if (countSinceUpdate == window.length) {
            float previousSlope = currentSlope;
            currentSlope = Stats.calculateSlope(window);
            shift -= window.length * previousSlope;
            countSinceUpdate = 0;
        } else {
            countSinceUpdate++;
        }

        return raw - (int) (countSinceUpdate * currentSlope) + shift;
    }

    @Override
    public void removeSpike(int size) {
        RawBlinkDetector.removeSpike(window, size);
    }
}
