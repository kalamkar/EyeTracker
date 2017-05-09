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
    protected final int feature1[] = new int[Config.GRAPH_LENGTH];
    protected final int feature2[] = new int[Config.GRAPH_LENGTH];

    private Pair<Integer, Integer> sector = Pair.create(-1, -1);

    private Stats hStats = new Stats(new int[]{});
    private Stats vStats = new Stats(new int[]{});

    private final EyeEvent.Observer eventObserver;

    private final GestureRecognizer gestures;

    private long updateCount = 0;
    private long processingMillis;
    private long firstUpdateTimeMillis = 0;

    public BandpassEogProcessor(EyeEvent.Observer eventObserver, int gestureThreshold) {
        this.eventObserver = eventObserver;
        gestures = new VariableLengthGestureRecognizer();
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

        hStats = new Stats(horizontal);
        vStats = new Stats(vertical);

        System.arraycopy(feature1, 1, feature1, 0, feature1.length - 1);
        feature1[feature1.length - 1] = 0;
        System.arraycopy(feature2, 1, feature2, 0, feature2.length - 1);
        feature2[feature2.length - 1] = 0;

        gestures.update(hValue, vValue);
        if (isGoodSignal() && gestures.hasEyeEvent()) {
            for (EyeEvent event : gestures.getEyeEvents()) {
                if (eventObserver.getCriteria().isMatching(event)) {
                    eventObserver.onEyeEvent(event);

                    if (event.type == EyeEvent.Type.SACCADE) {
                        feature1[feature1.length - 1] = event.amplitude;
                        int start = feature2.length - 1 -
                                (int) (event.durationMillis * Config.SAMPLING_FREQ / 1000);
                        feature2[start >= 0 ? start : 0] = 1;
                    }
                }
            }
        }

        processingMillis = System.currentTimeMillis() - startTime;
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return isGoodSignal() ? sector : Pair.create(-1, -1);
    }

    @Override
    public String getDebugNumbers() {
        int seconds = (int) ((System.currentTimeMillis() - firstUpdateTimeMillis) / 1000);
        int maxDev = Math.max(hStats.stdDev, vStats.stdDev);
        String dev = maxDev > 1000 ? Integer.toString(maxDev/1000) + "k" : Integer.toString(maxDev);
        return updateCount > 0 ? String.format("%d\n%s", seconds, dev) : "";
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
    public Pair<Integer, Integer> horizontalRange() {
        if (isGoodSignal()) {
            return Pair.create(-5000, 5000);
        } else {
            return Pair.create(hStats.min, hStats.max);
        }
    }

    @Override
    public int[] vertical() {
        return vertical;
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        if (isGoodSignal()) {
            return Pair.create(-5000, 5000);
        } else {
            return Pair.create(vStats.min, vStats.max);
        }
    }

    @Override
    public int[] feature1() {
        return feature1;
    }

    @Override
    public Pair<Integer, Integer> feature1Range() {
        if (isGoodSignal()) {
            return Pair.create(-5000, 5000);
        } else {
            return Pair.create(vStats.min, vStats.max);
        }
    }

    @Override
    public int[] feature2() {
        return feature2;
    }

    @Override
    public Pair<Integer, Integer> feature2Range() {
        if (isGoodSignal()) {
            return Pair.create(-5000, 5000);
        } else {
            return Pair.create(vStats.min, vStats.max);
        }
    }
}
