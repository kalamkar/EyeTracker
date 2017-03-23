package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 3/23/17.
 */

public class LongMemoryCalibrator implements Calibrator {

    private static final int WAIT_TIME_FOR_STABILITY_MILLIS = 5000;

    private static final float HORIZONTAL_FOV_FACTOR = 1.0f;
    private static final float VERTICAL_FOV_FACTOR = 1.0f;

    private final int minGraphHeight;
    private final int maxGraphHeight;

    private int hHalfGraphHeight;
    private int vHalfGraphHeight;

    private int horizontalBase = 0;
    private int verticalBase = 0;

    private int maxHHalfGraphHeight;
    private int maxVHalfGraphHeight;

    private long stableHorizontalMillis;
    private long stableVerticalMillis;

    private Stats hStats;
    private Stats vStats;

    public LongMemoryCalibrator(int minGraphHeight, int maxGraphHeight) {
        this.minGraphHeight = minGraphHeight;
        this.maxGraphHeight = maxGraphHeight;
    }

    public void reset() {
        hHalfGraphHeight = minGraphHeight;
        vHalfGraphHeight = minGraphHeight;
        maxHHalfGraphHeight = minGraphHeight;
        maxVHalfGraphHeight = minGraphHeight;

        stableHorizontalMillis = 0;
        stableVerticalMillis = 0;
    }

    @Override
    public void setHorizontalStats(Stats hStats) {
        this.hStats = hStats;
    }

    @Override
    public void setVerticalStats(Stats vStats) {
        this.vStats = vStats;
    }

    @Override
    public void setStableHorizontalMillis(long stableHorizontalMillis) {
        this.stableHorizontalMillis = stableHorizontalMillis;
    }

    @Override
    public void setStableVerticalMillis(long stableVerticalMillis) {
        this.stableVerticalMillis = stableVerticalMillis;
    }

    @Override
    public long getStableHorizontalMillis() {
        return this.stableHorizontalMillis;
    }

    @Override
    public long getStableVerticalMillis(){
        return this.stableVerticalMillis;
    }

    @Override
    public int horizontalBase() {
        return horizontalBase;
    }

    @Override
    public int verticalBase() {
        return verticalBase;
    }

    @Override
    public int horizontalGraphHeight() {
        return (int) (hHalfGraphHeight * HORIZONTAL_FOV_FACTOR);
    }

    @Override
    public int verticalGraphHeight() {
        return (int) (vHalfGraphHeight * VERTICAL_FOV_FACTOR);
    }

    public void update() {
        maybeUpdateHorizontalHeight();
        maybeUpdateVerticalHeight();
    }

    protected void maybeUpdateHorizontalHeight() {
        if (stableHorizontalMillis % WAIT_TIME_FOR_STABILITY_MILLIS == 0) {
            maxHHalfGraphHeight -= maxHHalfGraphHeight * 10 / 100;
        }
        int max = hStats.percentile95;
        int min = hStats.percentile5;
        int newHHalfGraphHeight = Math.min(maxGraphHeight,
                Math.max(minGraphHeight, (max - min) / 2));
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
        int newVHalfGraphHeight = Math.min(maxGraphHeight,
                Math.max(minGraphHeight, (max - min) / 2));
        if (stableVerticalMillis > WAIT_TIME_FOR_STABILITY_MILLIS
                && newVHalfGraphHeight > maxVHalfGraphHeight) {
            vHalfGraphHeight = newVHalfGraphHeight;
            maxVHalfGraphHeight = newVHalfGraphHeight;
            verticalBase = (min + max) / 2;
        }
    }
}
