package care.dovetail.tracker;

public class Config {
    public static final int GRAPH_LENGTH = 500;

    public static final int BLUETOOTH_ENABLE_REQUEST = 0;

    public static final double SAMPLING_FREQ  = 51.2; // 204.8;

    public static final int MILLIS_PER_UPDATE = (int) Math.round(1000.0 / Config.SAMPLING_FREQ);

    public static final int BLINK_WINDOW  = 20;

    public static final int GESTURE_VISIBILITY_MILLIS = 1000;

    public static final int FIXATION_VISIBILITY_MILLIS = 1000;

    public static final int MOLE_NUM_STEPS = 5;
}
