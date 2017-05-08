package care.dovetail.tracker;

import java.util.HashSet;
import java.util.Set;

import static care.dovetail.tracker.EyeEvent.Direction.NONE;

/**
 * Created by abhi on 4/24/17.
 */

public class EyeEvent {
    public enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        UP_LEFT,
        UP_RIGHT,
        DOWN_LEFT,
        DOWN_RIGHT,
        CENTER,
        NONE
    }

    public enum Type {
        SACCADE,
        GAZE,
        SMALL_BLINK,
        LARGE_BLINK,
        WHACKAMOLE_POSITION,
        POSITION,
        BAD_CONTACT
    }

    public interface Observer {
        Criteria getCriteria();
        void onEyeEvent(EyeEvent event);
    }

    public abstract static class Criteria {
        protected final Set<Criterion> criterionSet = new HashSet<>();
        public Criteria add(Criterion criterion) {
            criterionSet.add(criterion);
            return this;
        }

        public abstract boolean isMatching(EyeEvent event);
    }

    public static class AnyCriteria extends Criteria {
        public boolean isMatching(EyeEvent event) {
            for (Criterion criterion : criterionSet) {
                if (criterion != null && criterion.isMatching(event)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class AllCriteria extends Criteria {
        public boolean isMatching(EyeEvent event) {
            for (Criterion criterion : criterionSet) {
                if (criterion != null && !criterion.isMatching(event)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class Criterion {
        public final Type type;
        public final Direction direction;

        public final int minAmplitude;
        public final int maxAmplitude;

        public final long minDurationMillis;
        public final long maxDurationMillis;

        public Criterion(Type type, int minAmplitude) {
            this(type, NONE, minAmplitude, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        public Criterion(Type type, Direction direction, int minAmplitude) {
            this(type, direction, minAmplitude, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
        }

        public Criterion(Type type, Direction direction, int minAmplitude, int maxAmplitude,
                         long minDurationMillis, long maxDurationMillis) {
            this.type = type;
            this.direction = direction;

            this.minAmplitude = minAmplitude;
            this.maxAmplitude = maxAmplitude;

            this.minDurationMillis = minDurationMillis;
            this.maxDurationMillis = maxDurationMillis;
        }

        public boolean isMatching(EyeEvent event) {
            if (event == null || type != event.type) {
                return false;
            }
            if (direction != NONE && direction != event.direction) {
                return false;
            }
            if (minAmplitude > event.amplitude || maxAmplitude < event.amplitude) {
                return false;
            }
            return !(minDurationMillis > event.durationMillis
                    || maxDurationMillis < event.durationMillis);
        }
    }

    public final Type type;
    public final Direction direction;
    public final int amplitude;
    public final long durationMillis;
    public final int column;
    public final int row;

    public EyeEvent(Type type) {
        this(type, NONE, 0, 0, -1, -1);
    }

    public EyeEvent(Type type, Direction direction, int amplitude, long durationMillis) {
        this(type, direction, amplitude, durationMillis, -1, -1);
    }

    public EyeEvent(Type type, int column, int row) {
        this(type, NONE, 0, 0, column, row);
    }

    private EyeEvent(Type type, Direction direction, int amplitude, long durationMillis,
                     int column, int row) {
        this.type = type;
        this.direction = direction;
        this.amplitude = amplitude;
        this.durationMillis = durationMillis;
        this.column = column;
        this.row = row;
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, direction);
    }
}
