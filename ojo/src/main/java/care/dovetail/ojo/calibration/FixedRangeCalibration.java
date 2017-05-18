package care.dovetail.ojo.calibration;

/**
 * Created by abhi on 4/10/17.
 */

public class FixedRangeCalibration implements Calibration {
    private static final String TAG = "FixedRangeCalibration";

    private final int numSteps;

    private final int min;
    private final int max;
    private int level = 0;

    public FixedRangeCalibration(int numSteps, int range) {
        this.numSteps = numSteps;
        this.min = -(range / 2);
        this.max = range / 2;
    }

    @Override
    public int min() {
        return min;
    }

    @Override
    public int max() {
        return max;
    }

    @Override
    public int level() {
        return level;
    }

    @Override
    public int filter(int value) {
        level = DriftingMedianCalibration.getLevel(value, min, max, numSteps);
        return level;
    }

    @Override
    public void removeSpike(int size) {
    }
}
