package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

public class SlopeRemovalSignalProcessor extends SignalProcessor {
    private static final String TAG = "DriftRemovalSignalProcessor";

    private static final int DRIFT_WINDOW_SIZE = 100;
    private static final int DRIFT_UPDATE_INTERVAL = 100;

    protected final int hRaw[] = new int[DRIFT_WINDOW_SIZE];
    protected final int vRaw[] = new int[DRIFT_WINDOW_SIZE];

    private float hCurrentDrift = 0;
    private float vCurrentDrift = 0;
    private int hAdjustment = 0;
    private int vAdjustment = 0;
    private int hCountSinceDriftUpdate = 0;
    private int vCountSinceDriftUpdate = 0;

    public SlopeRemovalSignalProcessor(int numSteps, int graphHeight) {
        super(numSteps, new StaticCalibrator(graphHeight));
    }

    @Override
    public String getDebugNumbers() {
        return String.format("%d\n%d", goodSignalMillis / 1000, calibrator.horizontalGraphHeight());
    }

    @Override
    protected int processHorizontal(int value) {
        System.arraycopy(hRaw, 1, hRaw, 0, hRaw.length - 1);
        hRaw[hRaw.length - 1] = value;

        if (updateCount != 0 && updateCount % DRIFT_UPDATE_INTERVAL == 0) {
            float previousDrift = hCurrentDrift;
            hCurrentDrift = Stats.calculateSlope(hRaw);
            hAdjustment -= DRIFT_UPDATE_INTERVAL * previousDrift;

            hCountSinceDriftUpdate = 0;
        } else {
            hCountSinceDriftUpdate++;
        }

        return value - (int) (hCountSinceDriftUpdate * hCurrentDrift) + hAdjustment;
    }

    @Override
    protected int processVertical(int value) {
        System.arraycopy(vRaw, 1, vRaw, 0, vRaw.length - 1);
        vRaw[vRaw.length - 1] = value;

        if (updateCount != 0 && updateCount % DRIFT_UPDATE_INTERVAL == 0) {
            float previousDrift = vCurrentDrift;
            vCurrentDrift = Stats.calculateSlope(vRaw);
            vAdjustment -= DRIFT_UPDATE_INTERVAL * previousDrift;

            vCountSinceDriftUpdate = 0;
        } else {
            vCountSinceDriftUpdate++;
        }
        return value - (int) (vCountSinceDriftUpdate * vCurrentDrift) + vAdjustment;
    }
}
