package care.dovetail.tracker.processing;

import android.util.Pair;

import java.util.Arrays;

/**
 * Created by abhi on 9/21/16.
 */

public class Feature {

    public interface FeatureObserver {
        void onFeature(Feature feature);
    }

    public enum Type {
        BLINK,
        SMALL_BLINK,
        TRIPLE_BLINK,
        BAD_CONTACT,
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
    public Pair<Integer, Integer> sector = Pair.create(-1, -1);

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
