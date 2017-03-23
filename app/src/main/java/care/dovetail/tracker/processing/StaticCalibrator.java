package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 3/23/17.
 */

public class StaticCalibrator implements Calibrator {

    private final int halfGraphHeight;

    private long stableHorizontalMillis;
    private long stableVerticalMillis;

    public StaticCalibrator(int halfGraphHeight) {
        this.halfGraphHeight = halfGraphHeight;
    }

    public void reset() {
        stableHorizontalMillis = 0;
        stableVerticalMillis = 0;
    }

    @Override
    public void setHorizontalStats(Stats hStats) {
    }

    @Override
    public void setVerticalStats(Stats vStats) {
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
        return 0;
    }

    @Override
    public int verticalBase() {
        return 0;
    }

    @Override
    public int horizontalGraphHeight() {
        return halfGraphHeight;
    }

    @Override
    public int verticalGraphHeight() {
        return halfGraphHeight;
    }

    public void update() {
    }
}
