package care.dovetail.tracker.eog;

import android.util.Pair;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.EyeEvent;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 4/10/17.
 */

public class BandpassEogProcessor implements EOGProcessor {
    private static final String TAG = "BandpassEogProcessor";

    private final IirFilter hFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 1, 1.024 / Config.SAMPLING_FREQ, 2.56 / Config.SAMPLING_FREQ));
    private final IirFilter vFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 1, 1.024 / Config.SAMPLING_FREQ, 2.56 / Config.SAMPLING_FREQ));

    protected final int horizontal[] = new int[Config.GRAPH_LENGTH];
    protected final int vertical[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private Stats hStats = new Stats(new int[]{});
    private Stats vStats = new Stats(new int[]{});

    private final EyeEvent.Observer eventObserver;

    private final GestureRecognizer gestures;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public BandpassEogProcessor(EyeEvent.Observer eventObserver, int numSteps, int gestureThreshold) {
        this.eventObserver = eventObserver;
        gestures = new BandpassGestureRecognizer(gestureThreshold);
        firstUpdateTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void update(int hRaw, int vRaw) {
        updateCount++;
        long startTime = System.currentTimeMillis();
        firstUpdateTimeMillis = updateCount == 1 ? startTime : firstUpdateTimeMillis;

        int hValue = (int) hFilter.step(hRaw);
        int vValue = (int) vFilter.step(vRaw);

        System.arraycopy(horizontal, 1, horizontal, 0, horizontal.length - 1);
        horizontal[horizontal.length - 1] = hValue;

        System.arraycopy(vertical, 1, vertical, 0, vertical.length - 1);
        vertical[vertical.length - 1] = vValue;

        gestures.update(hValue, vValue);
        if (isGoodSignal() && gestures.hasEyeEvent()) {
            eventObserver.onEyeEvent(gestures.getEyeEvent());
        }

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return isGoodSignal() ? sector : Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        int dev = Math.round(Math.max(hStats.stdDev, vStats.stdDev) / 1000);
        return updateCount > 0 ? String.format("%d\n%dk", seconds, dev) : "";
    }

    @Override
    public boolean isGoodSignal() {
        return getSignalQuality() > 95;
    }

    @Override
    public int getSignalQuality() {
        return 100 - Math.min(100, Math.round(Math.max(hStats.stdDev, vStats.stdDev) / 10000));
    }

    @Override
    public boolean isStableHorizontal() {
        return hStats.stdDev < 20000;
    }

    @Override
    public boolean isStableVertical() {
        return vStats.stdDev < 20000;
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
