package care.dovetail.ojo.events;

import java.util.HashSet;
import java.util.Set;

import care.dovetail.ojo.Config;
import care.dovetail.ojo.EyeEvent;
import care.dovetail.ojo.SignalChecker;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/25/17.
 */

public class VariableLengthEyeEventRecognizer implements EyeEventRecognizer {
    private static final int FIXATION_THRESHOLD = 800;
    private static final int NUM_SAMPLES_PER_FIXATION_EVENT =
            (int) (500 * Config.SAMPLING_FREQ / 1000); // 500millis

    private final SaccadeSegmenter horizontal = new SaccadeSegmenter();
    private final SaccadeSegmenter vertical = new SaccadeSegmenter();

    private final SignalChecker signalChecker = new SignalChecker();

    private Set<EyeEvent> events = new HashSet<>();

    private int countSinceFixation = 0;

    @Override
    public void update(int hValue, int vValue) {
        events.clear();

        signalChecker.update(hValue, vValue);
        horizontal.update(hValue);
        vertical.update(vValue);

        if (signalChecker.hasNoSignal()) {
            events.add(new EyeEvent(EyeEvent.Type.BAD_CONTACT, 0,
                    signalChecker.getSignalLossDurationMillis()));
        }

        if (horizontal.hasSaccade()) {
            long hMillis = (long) (horizontal.saccadeLength * 1000 / Config.SAMPLING_FREQ);
            events.add(new EyeEvent(EyeEvent.Type.SACCADE,
                    horizontal.saccadeAmplitude > 0
                            ? EyeEvent.Direction.LEFT : EyeEvent.Direction.RIGHT,
                    horizontal.saccadeAmplitude, hMillis));
        }

        if (vertical.hasSaccade()) {
            long vMillis = (long) (vertical.saccadeLength * 1000 / Config.SAMPLING_FREQ);
            events.add(new EyeEvent(EyeEvent.Type.SACCADE,
                    vertical.saccadeAmplitude > 0 ? EyeEvent.Direction.UP : EyeEvent.Direction.DOWN,
                    vertical.saccadeAmplitude, vMillis));
        }

        if (Math.abs(Math.max(horizontal.saccadeAmplitude, vertical.saccadeAmplitude))
                > FIXATION_THRESHOLD) {
            countSinceFixation = 0;
        } else {
            countSinceFixation++;
        }
        // If there is fixation, send gaze events only at ~500 millis interval
        if (countSinceFixation > 0 && countSinceFixation % NUM_SAMPLES_PER_FIXATION_EVENT == 0) {
            long durationMillis = (long) (countSinceFixation * 1000 / Config.SAMPLING_FREQ);
            events.add(new EyeEvent(EyeEvent.Type.FIXATION, FIXATION_THRESHOLD, durationMillis));
        }
    }

    @Override
    public boolean hasEyeEvent() {
        return !events.isEmpty();
    }

    @Override
    public Set<EyeEvent> getEyeEvents() {
        return events;
    }

    // Segment based saccade detection
    private static class SaccadeSegmenter {
        private final int window[] = new int[512];

        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestDirectionChange = 0;
        private int valueAtLatestDirectionChange = 0;

        private int saccadeLength = 0;
        private int saccadeAmplitude = 0;

        public void update(int value) {
            System.arraycopy(window, 1, window, 0, window.length - 1);
            window[window.length - 1] = value;

            int newDirection = value - prevValue;
            newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

            if (currentDirection != newDirection && newDirection != 0) {
                currentDirection = newDirection;

                saccadeLength = countSinceLatestDirectionChange;
                saccadeAmplitude = prevValue - valueAtLatestDirectionChange;

                countSinceLatestDirectionChange = 0;
                valueAtLatestDirectionChange = prevValue;
            } else {
                countSinceLatestDirectionChange++;
                saccadeLength = 0;
                saccadeAmplitude = 0;
            }

            prevValue = value;
        }

        public boolean hasSaccade() {
            return saccadeLength > 0 && saccadeAmplitude != 0;
        }
    }

    // Point based saccade detection
    private static class SaccadeRecognizer {
        private final int window[] = new int[512];

        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestDirectionChange = 0;

        private int saccadeLength = 0;
        private int saccadeAmplitude = 0;

        public void update(int value) {
            System.arraycopy(window, 1, window, 0, window.length - 1);
            window[window.length - 1] = value;

            int newDirection = value - prevValue;
            newDirection /= newDirection != 0 ? Math.abs(newDirection) : 1;

            if (currentDirection != newDirection && newDirection != 0) {
                currentDirection = newDirection;

                // Calculate median baseline instead of using zero, Needed during bootup time
                // when filters are adjusting for the drift and baseline is away from zero.
                int median = Stats.calculateMedian(window, 0, window.length);
                saccadeLength = countSinceLatestDirectionChange;
                saccadeAmplitude = prevValue - median;

                countSinceLatestDirectionChange = 0;
            } else {
                countSinceLatestDirectionChange++;
                saccadeLength = 0;
                saccadeAmplitude = 0;
            }

            prevValue = value;
        }

        public boolean hasSaccade() {
            return saccadeLength > 0 && saccadeAmplitude != 0;
        }
    }
}
