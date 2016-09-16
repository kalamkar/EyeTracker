package care.dovetail.blinker;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private final int values[] = new int[Config.GRAPH_LENGTH];

    public synchronized void update(int[] chunk) {
        System.arraycopy(values, chunk.length, values, 0, values.length - chunk.length);
        System.arraycopy(chunk, 0, values, values.length - chunk.length, chunk.length);
    }

    public synchronized int[] getValues() {
        return values;
    }
}
