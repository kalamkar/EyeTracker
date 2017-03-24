package care.dovetail.tracker.processing;

import android.util.Pair;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

public class PassThroughSignalProcessor implements EOGProcessor {
    private static final String TAG = "PassThroughSignalProcessor";

    private long goodSignalMillis;
    private long sumTimeBetweenUpdateMillis = 0;
    private long lastUpdateMillis = 0;
    private long updateCount = 1;

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    protected Stats hStats = new Stats(null);
    protected Stats vStats = new Stats(null);

    @Override
    public void update(int hValue, int vValue) {
        updateCount++;
        long currentTime = System.currentTimeMillis();
        lastUpdateMillis = lastUpdateMillis == 0 ? currentTime : lastUpdateMillis;
        sumTimeBetweenUpdateMillis += (currentTime - lastUpdateMillis);
        lastUpdateMillis = currentTime;
        goodSignalMillis += Config.MILLIS_PER_UPDATE;

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;
        hStats = new Stats(horizontal);

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;
        vStats = new Stats(vertical);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", goodSignalMillis / 1000,
                sumTimeBetweenUpdateMillis / updateCount);
    }

    @Override
    public boolean isGoodSignal() {
        return true;
    }

    @Override
    public int getSignalQuality() {
        return 100;
    }

    @Override
    public boolean isStableHorizontal() {
        return true;
    }

    @Override
    public boolean isStableVertical() {
        return true;
    }

    @Override
    public int[] horizontal() {
        return horizontal;
    }

    @Override
    public int[] vertical() {
        return vertical;
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        return Pair.create(hStats.min, hStats.max);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return Pair.create(vStats.min, vStats.max);
    }
}
