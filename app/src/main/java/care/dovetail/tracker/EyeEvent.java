package care.dovetail.tracker;

/**
 * Created by abhi on 4/24/17.
 */

public class EyeEvent {
    public enum Type {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        UP_LEFT,
        UP_RIGHT,
        DOWN_LEFT,
        DOWN_RIGHT,
        SMALL_BLINK,
        LARGE_BLINK,
        WHACKAMOLE_POSITION,
        POSITION
    }

    public interface Observer {
        void onEyeEvent(EyeEvent event);
    }

    public final Type type;
    public final int amplitude;
    public final int column;
    public final int row;

    public EyeEvent(Type type) {
        this(type, 0, -1, -1);
    }

    public EyeEvent(Type type, int amplitude) {
        this(type, amplitude, -1, -1);
    }

    public EyeEvent(Type type, int column, int row) {
        this(type, 0, column, row);
    }

    private EyeEvent(Type type, int amplitude, int column, int row) {
        this.type = type;
        this.amplitude = amplitude;
        this.column = column;
        this.row = row;
    }
}
