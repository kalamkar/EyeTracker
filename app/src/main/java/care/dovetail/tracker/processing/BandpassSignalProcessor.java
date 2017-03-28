package care.dovetail.tracker.processing;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.tracker.Config;

public class BandpassSignalProcessor extends SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private static final int MIN_HALF_GRAPH_HEIGHT = 2000;
    private static final int MAX_HALF_GRAPH_HEIGHT = 12000;

    private final IirFilter hFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.0625 / Config.SAMPLING_FREQ, 1.0 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignFisher.design(
            FilterPassType.bandpass, FilterCharacteristicsType.butterworth, 2 /* order */, 0,
            0.0625 / Config.SAMPLING_FREQ, 1.0 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(int numSteps, int graphHeight) {
        // super(numSteps, new LongMemoryCalibrator(MIN_HALF_GRAPH_HEIGHT, MAX_HALF_GRAPH_HEIGHT));
        super(numSteps, new StaticCalibrator(graphHeight));
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d\n%d", calibrator.horizontalGraphHeight(),
                calibrator.verticalGraphHeight(), goodSignalMillis / 1000);
    }

    @Override
    protected int processHorizontal(int value) {
        return  (int) hFilter.step(value);
    }

    @Override
    protected int processVertical(int value) {
        return (int) vFilter.step(value);
    }
}
