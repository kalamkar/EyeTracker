package care.dovetail.blinker.processing;

/**
 * Created by abhi on 9/21/16.
 */

public class Feature {

    public enum Type {
        BLINK,
        TRIPLE_BLINK,
        BAD_SIGNAL
    }

    public enum Channel {
        LEFT,
        RIGHT,
        VERTICAL,
        HORIZONTAL,
        NONE,
        ALL
    }

    public final Type type;
    public final int index;
    public final int value;
    public final Channel channel;
    public int confidence;
    public int height;
    public int startIndex;
    public int endIndex;

    public Feature(Type type, int index, int value, Channel channel) {
        this.type = type;
        this.index = index;
        this.value = value;
        this.channel = channel;
    }
}
