package care.dovetail.blinker.processing;

import java.util.Arrays;

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
    public final int startIndex;
    public final int endIndex;
    public final int values[];
    public Channel channel;
    public int confidence;

    public Feature(Type type, int startIndex, int endIndex, int values[]) {
        this.type = type;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format("%s start = %d, end = %d values = %s", type, startIndex, endIndex,
                Arrays.toString(values));
    }
}
