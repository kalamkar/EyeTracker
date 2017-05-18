package care.dovetail.ojo.events;

import java.util.HashSet;
import java.util.Set;

import care.dovetail.ojo.Config;
import care.dovetail.ojo.EyeEvent;
import care.dovetail.ojo.Stats;

/**
 * Created by abhi on 4/25/17.
 */

public class BandpassEyeEventRecognizer implements EyeEventRecognizer {
    private static final int SACCADE_WINDOW_SIZE = 5;
    private static final int FIXATION_WINDOW_SIZE = 40;

    private static final int MIN_GAZE_STDDEV = 100;

    private final int gestureThreshold;

    private final int hWindow[] = new int[SACCADE_WINDOW_SIZE + FIXATION_WINDOW_SIZE];
    private final int vWindow[] = new int[SACCADE_WINDOW_SIZE + FIXATION_WINDOW_SIZE];

    private Set<EyeEvent> events = new HashSet<>();
    private int skipWindow = 0;

    int hSlope;

    public BandpassEyeEventRecognizer(int gestureThreshold) {
        this.gestureThreshold = gestureThreshold;
    }

    @Override
    public void update(int horizontal, int vertical) {
        System.arraycopy(hWindow, 1, hWindow, 0, hWindow.length - 1);
        hWindow[hWindow.length - 1] = horizontal;

        System.arraycopy(vWindow, 1, vWindow, 0, vWindow.length - 1);
        vWindow[vWindow.length - 1] = vertical;

        if (skipWindow > 0) {
            skipWindow--;
            events.clear();
            return;
        }

        Stats hGazeStats = new Stats(hWindow, 0, FIXATION_WINDOW_SIZE);
//        Stats vGazeStats = new Stats(vWindow, 0, FIXATION_WINDOW_SIZE);

        if (hGazeStats.stdDev < MIN_GAZE_STDDEV) {
            events.add(new EyeEvent(EyeEvent.Type.FIXATION));
        }

        Stats hGestureStats = new Stats(hWindow, FIXATION_WINDOW_SIZE, SACCADE_WINDOW_SIZE);
//        Stats vGestureStats = new Stats(vWindow, FIXATION_WINDOW_SIZE, SACCADE_WINDOW_SIZE);

        hSlope = hGestureStats.max - hGestureStats.min;
//        int vSlope = vGestureStats.max - vGestureStats.min;

        hSlope = hSlope > gestureThreshold ? hSlope : 0;

        EyeEvent gestureEvent = checkGestureSlopes(hSlope, 0); // Ignoring vertical for now
        if (gestureEvent != null) {
            events.add(gestureEvent);
            skipWindow = (int) (Config.SAMPLING_FREQ * (Config.GESTURE_VISIBILITY_MILLIS / 1000));
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
