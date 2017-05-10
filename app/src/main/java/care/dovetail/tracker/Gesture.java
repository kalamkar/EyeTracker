package care.dovetail.tracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by abhi on 5/10/17.
 */

public class Gesture extends EyeEvent.AnyCriteria implements EyeEvent.Observer {

    private final Set<Observer> observers = new HashSet<>();
    public final String name;

    private final List<EyeEvent> triggeredEvents = new ArrayList<>();

    public interface Observer {
        Set<Gesture> getGestures();
        void onGesture(Gesture gesture);
    }

    public Gesture(String name) {
        this.name = name;
    }

    public Gesture addObserver(Observer observer) {
        this.observers.add(observer);
        return this;
    }

    @Override
    public Gesture add(EyeEvent.Criterion criterion) {
        return (Gesture) super.add(criterion);
    }

    @Override
    public EyeEvent.Criteria getCriteria() {
        return this;
    }

    @Override
    public void onEyeEvent(EyeEvent event) {
        triggeredEvents.add(event);
        if (criteria.size() < triggeredEvents.size()) {
            triggeredEvents.remove(0);
        } else if (criteria.size() > triggeredEvents.size()) {
            return;
        }
        // Size of events matches size of criteria, compare them for gesture match.
        boolean matched = true;
        for (int i = 0; i < criteria.size(); i++) {
            matched = matched && criteria.get(i).isMatching(triggeredEvents.get(i));
        }
        if (matched) {
            for (Observer observer : observers) {
                observer.onGesture(this);
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
