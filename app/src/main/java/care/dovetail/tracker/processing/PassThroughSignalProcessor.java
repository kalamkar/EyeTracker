package care.dovetail.tracker.processing;

public class PassThroughSignalProcessor extends SignalProcessor {
    private static final String TAG = "SignalProcessor4";

    private long sumTimeBetweenUpdateMillis = 0;
    private long lastUpdateMillis = 0;
    private long updateCount = 1;

    public PassThroughSignalProcessor(FeatureObserver observer, int numSteps) {
        super(observer, numSteps);
        lastUpdateMillis = System.currentTimeMillis();
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", goodSignalMillis / 1000,
                sumTimeBetweenUpdateMillis / updateCount);
    }

    @Override
    protected int processHorizontal(int value) {
        updateCount++;
        long currentTime = System.currentTimeMillis();
        sumTimeBetweenUpdateMillis += (currentTime - lastUpdateMillis);
        lastUpdateMillis = currentTime;
        return value;
    }

    @Override
    protected int processVertical(int value) {
        return value;
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
        return 10000;
    }

    @Override
    protected int maxGraphHeight() {
        return 100000;
    }
}
