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

    private int hLevel;
    private int vLevel;

    public MedianDiffDiffEOGProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d\n%d", hHalfGraphHeight, vHalfGraphHeight,
                goodSignalMillis / 1000);
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
        hLevel += medianDiffDiff;
        return hLevel;
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
        vLevel += medianDiffDiff;
        return vLevel;
    }

    @Override
    protected void onFeature(Feature feature) {
        super.onFeature(feature);
        hLevel = 0;
        vLevel = 0;
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
