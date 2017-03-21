package care.dovetail.tracker.processing;

public class PassThroughSignalProcessor extends SignalProcessor {
    private static final String TAG = "SignalProcessor4";

    public PassThroughSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", hHalfGraphHeight, vHalfGraphHeight);
    }

    @Override
    protected int processHorizontal(int value) {
        return value;
    }

    @Override
    protected int processVertical(int value) {
        return value;
    }

    @Override
    protected int minGraphHeight() {
        return 10000;
    }

    @Override
    protected int maxGraphHeight() {
        return 100000;
    }
}
