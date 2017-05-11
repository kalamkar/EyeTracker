package care.dovetail.tracker.eog.calibration;

import care.dovetail.tracker.eog.filters.Filter;

/**
 * Created by abhi on 4/13/17.
 */

public interface Calibration extends Filter {
    int min();

    int max();

    int level();
}
