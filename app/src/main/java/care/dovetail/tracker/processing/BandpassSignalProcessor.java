package care.dovetail.tracker.processing;

import android.util.Pair;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class BandpassSignalProcessor extends SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private static final Pair<Integer, Integer> HALF_GRAPH_HEIGHT = new Pair<>(2000, 12000);

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 5000;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.0625 / ((double) Config.SAMPLING_FREQ), 1.0 / ((double) Config.SAMPLING_FREQ)));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.0625 / ((double) Config.SAMPLING_FREQ), 1.0 / ((double) Config.SAMPLING_FREQ)));

    protected int maxHHalfGraphHeight = minGraphHeight();
    protected int maxVHalfGraphHeight = minGraphHeight();

    public BandpassSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d\n%d", hHalfGraphHeight, vHalfGraphHeight,
                goodSignalMillis / 1000);
    }

    @Override
    protected int processHorizontal(int value) {
        return  (int) hFilter.step(value);
    }

    @Override
    protected int processVertical(int value) {
        return (int) vFilter.step(value);
    }

    protected void resetCalibration() {
        super.resetCalibration();
        maxHHalfGraphHeight = minGraphHeight();
        maxVHalfGraphHeight = minGraphHeight();
    }

    protected void maybeUpdateHorizontalHeight() {
        if (stableHorizontalMillis % WAIT_TIME_FOR_STABILITY_MILLIS == 0) {
            maxHHalfGraphHeight -= maxHHalfGraphHeight * 10 / 100;
        }
        int max = hStats.percentile95;
        int min = hStats.percentile5;
        int newHHalfGraphHeight = Math.min(maxGraphHeight(),
                Math.max(minGraphHeight(), (max - min) / 2));
        if (stableHorizontalMillis > WAIT_TIME_FOR_STABILITY_MILLIS
                && newHHalfGraphHeight > maxHHalfGraphHeight) {
            hHalfGraphHeight = newHHalfGraphHeight;
            maxHHalfGraphHeight = newHHalfGraphHeight;
            horizontalBase = (min + max) / 2;
        }
    }

    protected void maybeUpdateVerticalHeight() {
        if (stableVerticalMillis % WAIT_TIME_FOR_STABILITY_MILLIS == 0) {
            maxVHalfGraphHeight -= maxVHalfGraphHeight * 10 / 100;
        }
        int max = vStats.percentile95;
        int min = vStats.percentile5;
        int newVHalfGraphHeight = Math.min(maxGraphHeight(),
                Math.max(minGraphHeight(), (max - min) / 2));
        if (stableVerticalMillis > WAIT_TIME_FOR_STABILITY_MILLIS
                && newVHalfGraphHeight > maxVHalfGraphHeight) {
            vHalfGraphHeight = newVHalfGraphHeight;
            maxVHalfGraphHeight = newVHalfGraphHeight;
            verticalBase = (min + max) / 2;
        }
    }

    @Override
    protected int minGraphHeight() {
        return HALF_GRAPH_HEIGHT.first;
    }

    @Override
    protected int maxGraphHeight() {
        return HALF_GRAPH_HEIGHT.second;
    }
}
