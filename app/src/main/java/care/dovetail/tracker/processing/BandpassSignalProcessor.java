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

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 10000;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.25 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter hAggressiveFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            2.0 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    private final IirFilter vAggressiveFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            2.0 / Config.SAMPLING_FREQ, 4.0 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    protected int processHorizontal(int value) {
        int normalValue = (int) hFilter.step(value);
        int initialValue = (int) hAggressiveFilter.step(value);
        return goodSignalMillis < waitMillisForStability() ? initialValue : normalValue;
    }

    @Override
    protected int processVertical(int value) {
        int normalValue = (int) vFilter.step(value);
        int initialValue = (int) vAggressiveFilter.step(value);
        return goodSignalMillis < waitMillisForStability() ? initialValue : normalValue;
    }

    @Override
    protected int minGraphHeight() {
        return MIN_HALF_GRAPH_HEIGHT;
    }

    @Override
    protected int maxGraphHeight() {
        return MAX_HALF_GRAPH_HEIGHT;
    }

    @Override
    protected int waitMillisForStability() {
        return WAIT_TIME_FOR_STABILITY_MILLIS;
    }
}
