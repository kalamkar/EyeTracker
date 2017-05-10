package care.dovetail.tracker.eog;

import java.util.Set;

import care.dovetail.tracker.EyeEvent;

/**
 * Created by abhi on 4/25/17.
 */

public interface EyeEventRecognizer {
    void update(int horizontal, int vertical);
    boolean hasEyeEvent();
    Set<EyeEvent> getEyeEvents();
}
