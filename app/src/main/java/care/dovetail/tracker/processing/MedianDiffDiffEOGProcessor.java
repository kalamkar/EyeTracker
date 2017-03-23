package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

public class MedianDiffDiffEOGProcessor extends SignalProcessor {
    private static final String TAG = "MedianDiffDiffEOGProcessor";

    private final static int THRESH = 3;

    private final int hOriginal[] = new int[10];
    private final int vOriginal[] = new int[10];

    private int lastHorizontalMedian;
    private int lastVerticalMedian;

    private int lastHorizontalDiff;
    private int lastVerticalDiff;

    public MedianDiffDiffEOGProcessor(int numSteps) {
        super(numSteps);
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
        int shift = Math.abs(medianDiffDiff) > hStats.stdDev * THRESH ? medianDiffDiff : 0;
        lastHorizontalMedian = median;
        lastHorizontalDiff = medianDiff;
        return shift;
    }

    @Override
    protected int processVertical(int value) {
        System.arraycopy(vOriginal, 1, vOriginal, 0, vOriginal.length - 1);
        vOriginal[vOriginal.length - 1] = value;
        int median = new Stats(vOriginal).median;
        int medianDiff = median - lastVerticalMedian;
        int medianDiffDiff = medianDiff - lastVerticalDiff;
        int shift = Math.abs(medianDiffDiff) > vStats.stdDev * THRESH ? medianDiffDiff : 0;
        lastVerticalMedian = median;
        lastVerticalDiff = medianDiff;
        return shift;
    }

    protected void maybeUpdateHorizontalHeight() {
        hHalfGraphHeight = (hStats.percentile95 - hStats.percentile5) / 2;
    }

    protected void maybeUpdateVerticalHeight() {
        vHalfGraphHeight = (vStats.percentile95 - vStats.percentile5) / 2;
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
        return 200;
    }

    @Override
    protected int maxGraphHeight() {
        return 5000;
    }
}
