package care.dovetail.tracker.processing;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class BandpassSignalProcessor extends SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private static final int MIN_HALF_GRAPH_HEIGHT = 2000;
    private static final int MAX_HALF_GRAPH_HEIGHT = 8000;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d, %d\n%d, %d", hHalfGraphHeight, hStats.median,
                vHalfGraphHeight, vStats.median);
    }

    @Override
    protected int processHorizontal(int value) {
        return (int) hFilter.step(value);
    }

    @Override
    protected int processVertical(int value) {
        return (int) vFilter.step(value);
    }

    @Override
    protected int minGraphHeight() {
        return MIN_HALF_GRAPH_HEIGHT;
    }

    @Override
    protected int maxGraphHeight() {
        return MAX_HALF_GRAPH_HEIGHT;
    }
}
