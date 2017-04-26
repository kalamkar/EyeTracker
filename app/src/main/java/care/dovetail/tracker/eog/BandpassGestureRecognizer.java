package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/25/17.
 */

public class BandpassGestureRecognizer implements GestureRecognizer {
    private final int hWindow[] = new int[5];
    private final int vWindow[] = new int[5];

    private EyeEvent event;
    private int skipWindow = 0;

    int hSlope;

    public BandpassGestureRecognizer(int gestureThreshold) {
    }

    @Override
    public void update(int horizontal, int vertical) {
        System.arraycopy(hWindow, 1, hWindow, 0, hWindow.length - 1);
        hWindow[hWindow.length - 1] = horizontal;

        System.arraycopy(vWindow, 1, vWindow, 0, vWindow.length - 1);
        vWindow[vWindow.length - 1] = vertical;

        hSlope = (int) Stats.calculateSlope(hWindow);
        int vSlope = (int) Stats.calculateSlope(vWindow);

        hSlope = hSlope > 200 ? hSlope : 0;

        if (skipWindow > 0) {
            skipWindow--;
            event = null;
            return;
        }

        event = checkGestureSlopes(hSlope, 0); // Ignoring vertical for now
        if (event != null) {
            skipWindow = (int) (Config.SAMPLING_FREQ * (Config.GESTURE_VISIBILITY_MILLIS / 1000));
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
