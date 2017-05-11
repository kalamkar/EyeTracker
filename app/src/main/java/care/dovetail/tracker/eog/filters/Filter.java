package care.dovetail.tracker.eog.filters;

/**
 * Created by abhi on 4/13/17.
 */

public interface Filter {
    int filter(int raw);

    void removeSpike(int size);
}
