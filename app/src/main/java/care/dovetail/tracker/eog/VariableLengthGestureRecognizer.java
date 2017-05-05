package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public class VariableLengthGestureRecognizer implements GestureRecognizer {
    private int hPrevValue = 0;
    private int vPrevValue = 0;

    private int hDirection = 0;  // Up or Down, Direction of change of values, not eyes
    private int vDirection = 0;  // Up or Down, Direction of change of values, not eyes

    private int hLength = 0;
    private int vLength = 0;

    private int hLatestChangeValue = 0;
    private int vLatestChangeValue = 0;

    private EyeEvent event;

    public VariableLengthGestureRecognizer() {
    }

    @Override
    public void update(int horizontal, int vertical) {
        int hNewDirection = horizontal - hPrevValue;
        hNewDirection /= hNewDirection != 0 ? Math.abs(hNewDirection) : 1;
        int vNewDirection = vertical - vPrevValue;
        vNewDirection /= vNewDirection != 0 ? Math.abs(vNewDirection) : 1;

        int length = 0;
        if (hDirection != hNewDirection && hNewDirection != 0) {
            length = hLength;
            hLength = 0;
            vLength++;
            hDirection = hNewDirection;
        } else if (vDirection != vNewDirection && vNewDirection != 0) {
            length = vLength;
            vLength = 0;
            hLength++;
            vDirection = vNewDirection;
        } else {
            hLength++;
            vLength++;
        }

        if (length > 0) {
            int hAmplitude = hPrevValue - hLatestChangeValue;
            int vAmplitude = vPrevValue - vLatestChangeValue;

            hLatestChangeValue = hPrevValue;
            vLatestChangeValue = vPrevValue;

            event = checkGestureSlopes(hAmplitude, vAmplitude, length);
        } else {
            event = null;
        }

        hPrevValue = horizontal;
        vPrevValue = vertical;
    }

    @Override
    public boolean hasEyeEvent() {
        return event == null;
    }

    @Override
    public EyeEvent getEyeEvent() {
        return event;
    }

    private static EyeEvent checkGestureSlopes(int hAmplitude, int vAmplitude, int length) {
        EyeEvent.Direction hDirection = hAmplitude > 0 ? EyeEvent.Direction.LEFT
                : hAmplitude < 0 ? EyeEvent.Direction.RIGHT : null;
        EyeEvent.Direction vDirection = vAmplitude > 0 ? EyeEvent.Direction.UP
                : vAmplitude < 0 ? EyeEvent.Direction.DOWN : null;

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
            amplitude = Math.max(Math.abs(hAmplitude), Math.abs(vAmplitude));
        } else if (hDirection != null) {
            direction = hDirection;
            amplitude = hAmplitude;
        } else if (vDirection != null) {
            direction = vDirection;
            amplitude = vAmplitude;
        } else {
            return null;
        }
        long durationMillis = (long) (length * 1000 / Config.SAMPLING_FREQ);
        return new EyeEvent(EyeEvent.Type.SACCADE, direction, Math.abs(amplitude), durationMillis);
    }
}
