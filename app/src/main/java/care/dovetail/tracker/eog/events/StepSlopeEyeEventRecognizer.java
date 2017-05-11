package care.dovetail.tracker.eog.events;

import java.util.HashSet;
import java.util.Set;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.eog.SignalChecker;
import care.dovetail.tracker.eog.filters.StepSlopeGestureFilter;

/**
 * Created by abhi on 4/25/17.
 */

public class StepSlopeEyeEventRecognizer implements EyeEventRecognizer {
    private final SignalChecker signalChecker = new SignalChecker();

    private final StepSlopeGestureFilter hGesture;
    private final StepSlopeGestureFilter vGesture;

    private Set<EyeEvent> events = new HashSet<>();
    private int gestureSkipWindow = 0;
    private int fixationSkipWindow = 0;

    public StepSlopeEyeEventRecognizer(int gestureThreshold) {
        hGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
        vGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
    }

    @Override
    public void update(int horizontal, int vertical) {
        events.clear();

        signalChecker.update(horizontal, vertical);
        long signalLossDuration = signalChecker.getSignalLossDurationMillis();
        if (signalLossDuration > 0 && signalLossDuration % 100 == 0) {
            events.add(new EyeEvent(EyeEvent.Type.BAD_CONTACT, 0, signalLossDuration));
        }

        int hSlope = hGesture.filter(horizontal);
        int vSlope = vGesture.filter(vertical);

        if (gestureSkipWindow > 0) {
            gestureSkipWindow--;
            return;
        }

        if (fixationSkipWindow <= 0 && hGesture.getGazeSize() > 40) {
            events.add(new EyeEvent(EyeEvent.Type.FIXATION));
            fixationSkipWindow =
                    (int) (Config.SAMPLING_FREQ * (Config.FIXATION_VISIBILITY_MILLIS / 1000));
        } else {
            fixationSkipWindow = fixationSkipWindow > 0 ? fixationSkipWindow - 1 : 0;
        }

        EyeEvent gestureEvent = checkGestureSlopes(hSlope, 0); // Ignoring vertical for now
        if (gestureEvent != null) {
            events.add(gestureEvent);
            fixationSkipWindow = 0;
            gestureSkipWindow =
                    (int) (Config.SAMPLING_FREQ * (Config.GESTURE_VISIBILITY_MILLIS / 1000));
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

    private static EyeEvent checkGestureSlopes(int hSlope, int vSlope) {
        EyeEvent.Direction hDirection = hSlope > 0 ? EyeEvent.Direction.LEFT
                : hSlope < 0 ? EyeEvent.Direction.RIGHT : null;
        EyeEvent.Direction vDirection = vSlope > 0 ? EyeEvent.Direction.UP
                : vSlope < 0 ? EyeEvent.Direction.DOWN : null;

        EyeEvent.Direction direction;
        int amplitude = 0;
        if (hDirection != null && vDirection != null) {
            if (vDirection == EyeEvent.Direction.UP) {
                if (hDirection == EyeEvent.Direction.LEFT) {
                    direction = EyeEvent.Direction.UP_LEFT;
                } else {
                    direction = EyeEvent.Direction.UP_RIGHT;
                }
            } else {
                if (hDirection == EyeEvent.Direction.LEFT) {
                    direction = EyeEvent.Direction.DOWN_LEFT;
                } else {
                    direction = EyeEvent.Direction.DOWN_RIGHT;
                }
            }
            amplitude = Math.max(Math.abs(hSlope), Math.abs(vSlope));
        } else if (hDirection != null) {
            direction = hDirection;
            amplitude = hSlope;
        } else if (vDirection != null) {
            direction = vDirection;
            amplitude = vSlope;
        } else {
            return null;
        }
        return new EyeEvent(EyeEvent.Type.SACCADE, direction, Math.abs(amplitude), 0);
    }
}
