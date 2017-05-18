package care.dovetail.ojo.calibration;

import care.dovetail.ojo.filters.Filter;

/**
 * Created by abhi on 4/13/17.
 */

public interface Calibration extends Filter {
    int min();

    int max();

    int level();
}
