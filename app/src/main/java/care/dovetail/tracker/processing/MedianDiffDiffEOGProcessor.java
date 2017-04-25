package care.dovetail.tracker.processing;

import android.util.Pair;

import care.dovetail.tracker.Config;
import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.Stats;

public class MedianDiffDiffEOGProcessor implements EOGProcessor {
    private static final String TAG = "MedianDiffDiffEOGProcessor";

    private final static int THRESH = 3;
    private final static int MU_SCALE = 3;
    private final static int SIGMA_SCALE = 1;

    private final int numSteps;

    private long goodSignalMillis;

    private final int hRaw[] = new int[10];
    private final int vRaw[] = new int[10];

    private final int hBaseline[] = new int[10];
    private final int vBaseline[] = new int[10];

    private final int hShift[] = new int[10];
    private final int vShift[] = new int[10];

    private int lastHorizontalMedian;
    private int lastVerticalMedian;

    private int lastHorizontalDiff;
    private int lastVerticalDiff;

    public MedianDiffDiffEOGProcessor(int numSteps) {
        this.numSteps = numSteps;
    }

    @Override
    public void update(int hValue, int vValue) {
        goodSignalMillis += Config.MILLIS_PER_UPDATE;
        processHorizontal(hValue);
        processVertical(vValue);
    }

    @Override
    public Pair<Integer, Integer> getSector() {
        return null;
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d", goodSignalMillis / 1000);
    }

    protected void processHorizontal(int value) {
        System.arraycopy(hRaw, 1, hRaw, 0, hRaw.length - 1);
        hRaw[hRaw.length - 1] = value;

        int median = new Stats(hRaw).median;
        int medianDiff = median - lastHorizontalMedian;
        int medianDiffDiff = medianDiff - lastHorizontalDiff;
        lastHorizontalMedian = median;
        lastHorizontalDiff = medianDiff;

        Stats baselineStats = new Stats(hBaseline);
        System.arraycopy(hShift, 1, hShift, 0, hShift.length - 1);
        if (Math.abs(medianDiffDiff) > baselineStats.stdDev * THRESH) {
            hShift[hShift.length - 1] = medianDiffDiff;
            if (Math.random() < 0.5) {  // TODO(abhi): Replace 0.5 with normcdf
                System.arraycopy(hBaseline, 1, hBaseline, 0, hBaseline.length - 1);
                hBaseline[hBaseline.length - 1] = medianDiffDiff;
            }
        } else {
            hShift[hShift.length - 1] = 0;
            System.arraycopy(hBaseline, 1, hBaseline, 0, hBaseline.length - 1);
            hBaseline[hBaseline.length - 1] = medianDiffDiff;
        }
        Stats shiftStats = new Stats(hShift);
        // shiftStats.max, shiftStats.maxIndex
        // shiftStats.min, shiftStats.minIndex
        int mu = MU_SCALE * baselineStats.stdDev;
        int sigma = SIGMA_SCALE * baselineStats.stdDev;
    }

    protected void processVertical(int value) {
        System.arraycopy(vRaw, 1, vRaw, 0, vRaw.length - 1);
        vRaw[vRaw.length - 1] = value;

        int median = new Stats(vRaw).median;
        int medianDiff = median - lastVerticalMedian;
        int medianDiffDiff = medianDiff - lastVerticalDiff;
        lastVerticalMedian = median;
        lastVerticalDiff = medianDiff;

        Stats baselineStats = new Stats(vBaseline);
        System.arraycopy(vShift, 1, vShift, 0, vShift.length - 1);
        if (Math.abs(medianDiffDiff) > baselineStats.stdDev * THRESH) {
            vShift[vShift.length - 1] = medianDiffDiff;
            if (Math.random() < 0.5) {  // TODO(abhi): Replace 0.5 with normcdf
                System.arraycopy(vBaseline, 1, vBaseline, 0, vBaseline.length - 1);
                vBaseline[vBaseline.length - 1] = medianDiffDiff;
            }
        } else {
            vShift[vShift.length - 1] = 0;
            System.arraycopy(vBaseline, 1, vBaseline, 0, vBaseline.length - 1);
            vBaseline[vBaseline.length - 1] = medianDiffDiff;
        }
        Stats shiftStats = new Stats(vShift);
        // shiftStats.max, shiftStats.maxIndex
        // shiftStats.min, shiftStats.minIndex
        int mu = MU_SCALE * baselineStats.stdDev;
        int sigma = SIGMA_SCALE * baselineStats.stdDev;
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
    public boolean isStableHorizontal() {
        return false;
    }

    @Override
    public boolean isStableVertical() {
        return false;
    }

    @Override
    public int[] horizontal() {
        return new int[0];
    }

    @Override
    public int[] vertical() {
        return new int[0];
    }

    @Override
    public Pair<Integer, Integer> horizontalRange() {
        return null;
    }

    @Override
    public Pair<Integer, Integer> verticalRange() {
        return null;
    }
}
