package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public class StepSlopeGestureRecognizer implements GestureRecognizer {
    private final StepSlopeGestureFilter hGesture;
    private final StepSlopeGestureFilter vGesture;

    private EyeEvent event;
    private int gestureSkipWindow = 0;
    private int gazeSkipWindow = 0;

    public StepSlopeGestureRecognizer(int gestureThreshold) {
        hGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
        vGesture = new StepSlopeGestureFilter(5, 512, 3.0f, gestureThreshold, 40);
    }

    @Override
    public void update(int horizontal, int vertical) {
        int hSlope = hGesture.filter(horizontal);
        int vSlope = vGesture.filter(vertical);

        if (gestureSkipWindow > 0) {
            gestureSkipWindow--;
            event = null;
            return;
        }

        if (gazeSkipWindow <= 0 && hGesture.getGazeSize() > 40) {
            event = new EyeEvent(EyeEvent.Type.GAZE);
            gazeSkipWindow = (int) (Config.SAMPLING_FREQ * (Config.GAZE_VISIBILITY_MILLIS / 1000));
        } else {
            gazeSkipWindow = gazeSkipWindow > 0 ? gazeSkipWindow - 1 : 0;
        }

        EyeEvent gestureEvent = checkGestureSlopes(hSlope, 0); // Ignoring vertical for now
        if (gestureEvent != null) {
            event = gestureEvent;
            gazeSkipWindow = 0;
            gestureSkipWindow =
                    (int) (Config.SAMPLING_FREQ * (Config.GESTURE_VISIBILITY_MILLIS / 1000));
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
        return new EyeEvent(EyeEvent.Type.GESTURE, direction, Math.abs(amplitude));
    }
}
