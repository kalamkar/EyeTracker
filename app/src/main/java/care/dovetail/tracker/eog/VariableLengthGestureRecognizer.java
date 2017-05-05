package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public class VariableLengthGestureRecognizer implements GestureRecognizer {
    private final SaccadeSegmenter horizontal;
    private final SaccadeSegmenter vertical;

    private EyeEvent event;

    public VariableLengthGestureRecognizer(int gestureThreshold) {
        horizontal = new SaccadeSegmenter(gestureThreshold);
        vertical = new SaccadeSegmenter(gestureThreshold);
    }

    @Override
    public void update(int hValue, int vValue) {
        horizontal.update(hValue);
        vertical.update(vValue);
        if (horizontal.hasSaccade()) {
            event = new EyeEvent(EyeEvent.Type.SACCADE,
                    horizontal.amplitude > 0 ? EyeEvent.Direction.LEFT : EyeEvent.Direction.RIGHT,
                    horizontal.amplitude, (long) (horizontal.length * 1000 / Config.SAMPLING_FREQ));
        } else if (vertical.hasSaccade()) {
            event = new EyeEvent(EyeEvent.Type.SACCADE,
                    vertical.amplitude > 0 ? EyeEvent.Direction.UP : EyeEvent.Direction.DOWN,
                    vertical.amplitude, (long) (vertical.length * 1000 / Config.SAMPLING_FREQ));
        } else {
            event = null;
        }
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
        private final int gestureThreshold;

        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestSaccade = 0;
        private int latestSaccadeEndValue = 0;

        public int length = 0;
        public int amplitude = 0;

        public SaccadeSegmenter(int gestureThreshold) {
            this.gestureThreshold = gestureThreshold;
        }

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
            return length > 0 && amplitude != 0
                    && (Math.abs(amplitude) * length > gestureThreshold);
        }

        public int getCurrentAmplitude() {
            return prevValue - latestSaccadeEndValue;
        }

        public int getCurrentLength() {
            return countSinceLatestSaccade;
        }
    }
}
