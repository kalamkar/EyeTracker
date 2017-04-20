package care.dovetail.tracker.eog;

/**
 * Created by abhi on 4/19/17.
 */

public interface GestureObserver {
    enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    void onGesture(Direction direction, int amplitude);
}
