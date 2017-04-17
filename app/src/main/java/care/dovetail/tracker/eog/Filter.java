package care.dovetail.tracker.eog;

/**
 * Created by abhi on 4/13/17.
 */

interface Filter {
    int update(int raw);

    void removeSpike(int size);
}
