package care.dovetail.tracker.eog;

import java.util.HashSet;
import java.util.Set;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/25/17.
 */

public class VariableLengthEyeEventRecognizer implements EyeEventRecognizer {
    private static final int FIXATION_THRESHOLD = 800;

    private final SaccadeSegmenter horizontal = new SaccadeSegmenter();
    private final SaccadeSegmenter vertical = new SaccadeSegmenter();

    private final SignalChecker signalChecker = new SignalChecker();

    private Set<EyeEvent> events = new HashSet<>();

    private int countSinceStableGaze = 0;

    @Override
    public void update(int hValue, int vValue) {
        events.clear();

        signalChecker.update(hValue, vValue);
        horizontal.update(hValue);
        vertical.update(vValue);

        long signalLossDuration = signalChecker.getSignalLossDurationMillis();
        if (signalLossDuration > 0 && signalLossDuration % 100 == 0) {
            events.add(new EyeEvent(EyeEvent.Type.BAD_CONTACT, 0, signalLossDuration));
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
            countSinceStableGaze = 0;
        } else {
            countSinceStableGaze++;
        }
        long durationMillis = (long) (countSinceStableGaze * 1000 / Config.SAMPLING_FREQ);
        // Send gaze events only at 100 millis interval
        if (durationMillis > 0 && durationMillis % 100 == 0) {
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

    private static class SaccadeSegmenter {
        private final int window[] = new int[512];

        private int prevValue = 0;
        private int currentDirection = 0;  // Up or Down, Direction of change of values, not eyes
        private int countSinceLatestSaccade = 0;

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
                saccadeLength = countSinceLatestSaccade;
                saccadeAmplitude = prevValue - median;

                countSinceLatestSaccade = 0;
            } else {
                countSinceLatestSaccade++;
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
