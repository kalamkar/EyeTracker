package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

public class MedianDiffDiffEOGProcessor extends SignalProcessor {
    private static final String TAG = "MedianDiffDiffEOGProcessor";

    private final int hOriginal[] = new int[50];
    private final int vOriginal[] = new int[50];

    private int lastHorizontalMedian;
    private int lastVerticalMedian;

    private int lastHorizontalDiff;
    private int lastVerticalDiff;

    public MedianDiffDiffEOGProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d", goodSignalMillis / 1000);
    }

    @Override
    protected int processHorizontal(int value) {
        System.arraycopy(hOriginal, 1, hOriginal, 0, hOriginal.length - 1);
        hOriginal[hOriginal.length - 1] = value;
        int median = new Stats(hOriginal).median;
        int medianDiff = median - lastHorizontalMedian;
        int medianDiffDiff = medianDiff - lastHorizontalDiff;
        lastHorizontalMedian = median;
        lastHorizontalDiff = medianDiff;
        return medianDiffDiff;
    }

    @Override
    protected int processVertical(int value) {
        System.arraycopy(vOriginal, 1, vOriginal, 0, vOriginal.length - 1);
        vOriginal[vOriginal.length - 1] = value;
        int median = new Stats(vOriginal).median;
        int medianDiff = median - lastVerticalMedian;
        int medianDiffDiff = medianDiff - lastVerticalDiff;
        lastVerticalMedian = median;
        lastVerticalDiff = medianDiff;
        return medianDiffDiff;
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
    protected int minGraphHeight() {
        return 2000;
    }

    @Override
    protected int maxGraphHeight() {
        return 8000;
    }
}
