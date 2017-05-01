package care.dovetail.tracker.ui;

import care.dovetail.tracker.EOGProcessor;
import care.dovetail.tracker.processing.BlinkDetector;

/**
 * Created by abhi on 5/1/17.
 */

public interface DebugUi {
    void setDataSource(EOGProcessor signals, BlinkDetector blinks);
    void updateStatusUI(int spinner, int progress, int numbers);
    void setProgress(int progress);
    void showWarning(boolean show);
}
