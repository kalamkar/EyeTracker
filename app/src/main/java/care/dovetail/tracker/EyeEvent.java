package care.dovetail.tracker;

import static care.dovetail.tracker.EyeEvent.Direction.NONE;

/**
 * Created by abhi on 4/24/17.
 */

public class EyeEvent {
    public enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        UP_LEFT,
        UP_RIGHT,
        DOWN_LEFT,
        DOWN_RIGHT,
        CENTER,
        NONE
    }

    public enum Type {
        SACCADE,
        GAZE,
        SMALL_BLINK,
        LARGE_BLINK,
        WHACKAMOLE_POSITION,
        POSITION,
        BAD_CONTACT
    }

    public interface Observer {
        void onEyeEvent(EyeEvent event);
    }

    public final Type type;
    public final Direction direction;
    public final int amplitude;
    public final long durationMillis;
    public final int column;
    public final int row;

    public EyeEvent(Type type) {
        this(type, NONE, 0, 0, -1, -1);
    }

    public EyeEvent(Type type, Direction direction, int amplitude, long durationMillis) {
        this(type, direction, amplitude, durationMillis, -1, -1);
    }

    public EyeEvent(Type type, int column, int row) {
        this(type, NONE, 0, 0, column, row);
    }

    private EyeEvent(Type type, Direction direction, int amplitude, long durationMillis,
                     int column, int row) {
        this.type = type;
        this.direction = direction;
        this.amplitude = amplitude;
        this.durationMillis = durationMillis;
        this.column = column;
        this.row = row;
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, direction);
    }
}
