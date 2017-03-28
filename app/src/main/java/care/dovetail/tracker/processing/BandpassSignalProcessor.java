package care.dovetail.tracker.processing;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import care.dovetail.tracker.Config;

public class BandpassSignalProcessor extends SignalProcessor {
    private static final String TAG = "BandpassSignalProcessor";

    private final IirFilter hFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 2, 0.0625 / Config.SAMPLING_FREQ, 1.024 / Config.SAMPLING_FREQ));

    private final IirFilter vFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 2, 0.0625 / Config.SAMPLING_FREQ, 1.024 / Config.SAMPLING_FREQ));

    public BandpassSignalProcessor(int numSteps, int graphHeight) {
        super(numSteps, new StaticCalibrator(graphHeight));
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d,%d", calibrator.horizontalGraphHeight(),
                sumProcessingMillis / updateCount, goodSignalMillis / 1000);
    }

    @Override
    protected int processHorizontal(int value) {
        return (int) hFilter.step(value);
    }

    @Override
    protected int processVertical(int value) {
        return (int) vFilter.step(value);
    }
}
