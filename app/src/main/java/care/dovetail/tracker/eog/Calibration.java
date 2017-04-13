package care.dovetail.tracker.eog;

/**
 * Created by abhi on 4/13/17.
 */

interface Calibration extends Transformation {
    int min();

    int max();

    int level();
}
