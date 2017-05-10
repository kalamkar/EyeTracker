package care.dovetail.tracker.processing;

import android.util.Pair;

import java.util.HashSet;
import java.util.Set;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 3/31/17.
 */

public class DriftlessEogProcessor implements EOGProcessor {

    private static final int DRIFT_WINDOW_SIZE = 100;
    private static final int DRIFT_UPDATE_INTERVAL = 100;

    private final Set<EyeEvent.Observer> observers = new HashSet<>();

    protected final int hRaw[] = new int[DRIFT_WINDOW_SIZE];
    protected final int vRaw[] = new int[DRIFT_WINDOW_SIZE];

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    protected final int numSteps;

    private float hCurrentDrift = 0;
    private float vCurrentDrift = 0;
    private int hAdjustment = 0;
    private int vAdjustment = 0;
    private int countSinceDriftUpdate = 0;

    private long updateCount = 0;

    public DriftlessEogProcessor(int numSteps) {
        this.numSteps = numSteps;
    }

    @Override
    public void update(int hValue, int vValue) {
        System.arraycopy(hRaw, 1, hRaw, 0, hRaw.length - 1);
        hRaw[hRaw.length - 1] = hValue;

        System.arraycopy(vRaw, 1, vRaw, 0, vRaw.length - 1);
        vRaw[vRaw.length - 1] = vValue;

        if (updateCount != 0 && updateCount % DRIFT_UPDATE_INTERVAL == 0) {
            float previousDrift = hCurrentDrift;
            hCurrentDrift = Stats.calculateSlope(hRaw);
            hAdjustment -= DRIFT_UPDATE_INTERVAL * previousDrift;

            previousDrift = vCurrentDrift;
            vCurrentDrift = Stats.calculateSlope(vRaw);
            vAdjustment -= DRIFT_UPDATE_INTERVAL * previousDrift;

            countSinceDriftUpdate = 0;
        } else {
            countSinceDriftUpdate++;
        }

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] =
                hValue - (int) (countSinceDriftUpdate * hCurrentDrift) + hAdjustment;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] =
                vValue - (int) (countSinceDriftUpdate * vCurrentDrift) + vAdjustment;

        updateCount++;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return Pair.create(-1, -1);
    }

    @Override
    public void addObserver(EyeEvent.Observer observer) {
        this.observers.add(observer);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", (int) hCurrentDrift, (int) vCurrentDrift);
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
        Stats hStats = new Stats(horizontal);
        return Pair.create(hStats.min, hStats.max);
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        Stats vStats = new Stats(vertical);
        return Pair.create(vStats.min, vStats.max);
    }

    @Override
    public int[] feature1() {
        return new int[]{};
    }

    @Override
    public Pair<Integer, Integer> feature1Range() {
        return Pair.create(-1, 1);
    }

    @Override
    public int[] feature2() {
        return new int[]{};
    }

    @Override
    public Pair<Integer, Integer> feature2Range() {
        return Pair.create(-1, 1);
    }
}
