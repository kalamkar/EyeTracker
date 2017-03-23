package care.dovetail.tracker.processing;

import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 3/23/17.
 */

public interface Calibrator {
    void reset();

    void update();

    void setHorizontalStats(Stats hStats);

    void setVerticalStats(Stats vStats);

    void setStableHorizontalMillis(long millis);

    void setStableVerticalMillis(long millis);

    long getStableHorizontalMillis();

    long getStableVerticalMillis();

    int horizontalBase();

    int verticalBase();

    int horizontalGraphHeight();

    int verticalGraphHeight();
}
