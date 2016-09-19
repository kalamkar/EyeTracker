package care.dovetail.blinker;

import java.util.Arrays;

public class SignalProcessor {
    private static final String TAG = "SignalProcessor";

    private static final int STEP_HEIGHT = (int) (Config.GRAPH_HEIGHT * 0.05);

    private final int values1[] = new int[Config.GRAPH_LENGTH];
    private final int values2[] = new int[Config.GRAPH_LENGTH];

    private int median1;
    private int median2;

    private int position1;
    private int position2;

    public synchronized void update(int[] chunk1, int[] chunk2) {
        System.arraycopy(values1, chunk1.length, values1, 0, values1.length - chunk1.length);
        System.arraycopy(chunk1, 0, values1, values1.length - chunk1.length, chunk1.length);

        System.arraycopy(values2, chunk2.length, values2, 0, values2.length - chunk2.length);
        System.arraycopy(chunk2, 0, values2, values2.length - chunk2.length, chunk2.length);

        median1 = calculateMedian(values1);
        median2 = calculateMedian(values2);

        int currentValue1 = Math.max(median1 - Config.GRAPH_HEIGHT,
                Math.min(median1 + Config.GRAPH_HEIGHT, calculateMedian(chunk1)));
        int currentValue2 = Math.max(median2 - Config.GRAPH_HEIGHT,
                Math.min(median2 + Config.GRAPH_HEIGHT, calculateMedian(chunk2)));
        position1 = (currentValue1 - median1) / STEP_HEIGHT;
        position2 = (currentValue2 - median2) / STEP_HEIGHT;
    }

    public int[] channel1() {
        return values1;
    }

    public int[] channel2() {
        return values2;
    }

    public int median1() {
        return median1;
    }

    public int median2() {
        return median2;
    }

    public int position1() {
        return position1;
    }

    public int position2() {
        return position2;
    }

    private static int calculateMedian(int values[]) {
        int copyOfValues[] = values.clone();
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }
}
