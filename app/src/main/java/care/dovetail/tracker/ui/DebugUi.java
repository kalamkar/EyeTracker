package care.dovetail.tracker.ui;

import care.dovetail.ojo.EogProcessor;

/**
 * Created by abhi on 5/1/17.
 */

public interface DebugUi {
    void setDataSource(EogProcessor eog);
    void updateStatusUI(int spinner, int progress, int numbers);
    void setProgress(int progress);
    void showWarning(boolean show);
}
