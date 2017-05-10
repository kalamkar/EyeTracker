package care.dovetail.tracker.eog;

import care.dovetail.tracker.Config;

/**
 * Created by abhi on 5/10/17.
 */
public class SignalChecker {
    private int countSinceHSignalLoss = 0;
    private int countSinceVSignalLoss = 0;

    public void update(int hValue, int vValue) {
        countSinceHSignalLoss = hValue == 0 ? countSinceHSignalLoss + 1 : 0;
        countSinceVSignalLoss = vValue == 0 ? countSinceVSignalLoss + 1 : 0;
    }

    public long getSignalLossDurationMillis() {
        int maxSignalLossCount = Math.max(countSinceHSignalLoss, countSinceVSignalLoss);
        return (long) (maxSignalLossCount * 1000 / Config.SAMPLING_FREQ);
    }
}
