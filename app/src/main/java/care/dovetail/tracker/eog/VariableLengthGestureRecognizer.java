package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public class VariableLengthGestureRecognizer implements GestureRecognizer {
    private static final int MAX_SACCADE_MILLIS = 300;

    private final SaccadeSegmenter horizontal = new SaccadeSegmenter();
    private final SaccadeSegmenter vertical = new SaccadeSegmenter();

    private int hSkipWindow = 0;
    private int vSkipWindow = 0;

    private EyeEvent event;

    @Override
    public void update(int hValue, int vValue) {
        horizontal.update(hValue);
        vertical.update(vValue);

        if (horizontal.hasSaccade() && hSkipWindow == 0) {
            long hMillis = (long) (horizontal.length * 1000 / Config.SAMPLING_FREQ);
            event = new EyeEvent(EyeEvent.Type.SACCADE,
                    horizontal.amplitude > 0 ? EyeEvent.Direction.LEFT : EyeEvent.Direction.RIGHT,
                    horizontal.amplitude, hMillis);
            if (hMillis < MAX_SACCADE_MILLIS) {
                hSkipWindow = horizontal.length;
            }
            return;
        }

        if (vertical.hasSaccade() && vSkipWindow == 0) {
            long vMillis = (long) (vertical.length * 1000 / Config.SAMPLING_FREQ);
            event = new EyeEvent(EyeEvent.Type.SACCADE,
                    vertical.amplitude > 0 ? EyeEvent.Direction.UP : EyeEvent.Direction.DOWN,
                    vertical.amplitude, vMillis);
            if (vMillis < MAX_SACCADE_MILLIS) {
                vSkipWindow = vertical.length;
            }
            return;
        }

        event = null;
        hSkipWindow = hSkipWindow > 0 ? hSkipWindow - 1 : 0;
        vSkipWindow = vSkipWindow > 0 ? vSkipWindow - 1 : 0;
    }

    @Override
    public boolean hasEyeEvent() {
        return event != null;
    }

    @Override
    public EyeEvent getEyeEvent() {
        return event;
    }

    private static class SaccadeSegmenter {
        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestSaccade = 0;
        private int latestSaccadeEndValue = 0;

        private int length = 0;
        private int amplitude = 0;

        public void update(int value) {
            int newDirection = value - prevValue;
            newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

            if (currentDirection != newDirection && newDirection != 0) {
                currentDirection = newDirection;

                length = countSinceLatestSaccade;
                countSinceLatestSaccade = 0;

                amplitude = prevValue - latestSaccadeEndValue;
                latestSaccadeEndValue = prevValue;
            } else {
                countSinceLatestSaccade++;
                length = 0;
                amplitude = 0;
            }

            prevValue = value;
        }

        public boolean hasSaccade() {
            return length > 0 && amplitude != 0;
        }

        public int getCurrentAmplitude() {
            return prevValue - latestSaccadeEndValue;
        }

        public int getCurrentLength() {
            return countSinceLatestSaccade;
        }
    }
}
