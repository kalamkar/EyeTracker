package care.dovetail.tracker;

import java.util.ArrayList;
import java.util.List;

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
        FIXATION,
        POSITION,
        SIGNAL_QUALITY,
        BAD_CONTACT
    }

    public interface Observer {
        Criteria getCriteria();
        void onEyeEvent(EyeEvent event);
    }

    public abstract static class Criteria {
        protected final List<Criterion> criteria = new ArrayList<>();
        public Criteria add(Criterion criterion) {
            criteria.add(criterion);
            return this;
        }

        public abstract boolean isMatching(EyeEvent event);
    }

    public static class AnyCriteria extends Criteria {
        public boolean isMatching(EyeEvent event) {
            for (Criterion criterion : criteria) {
                if (criterion != null && criterion.isMatching(event)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class AllCriteria extends Criteria {
        public boolean isMatching(EyeEvent event) {
            for (Criterion criterion : criteria) {
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

        public final int maxColumn;
        public final int maxRow;

        public Criterion(Type type) {
            this(type, NONE, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,
                    -1, -1);
        }

        private Criterion(Type type, Direction direction, int minAmplitude, int maxAmplitude,
                          long minDurationMillis, long maxDurationMillis,
                          int maxColumn, int maxRow) {
            this.type = type;
            this.direction = direction;

            this.minAmplitude = minAmplitude;
            this.maxAmplitude = maxAmplitude;

            this.minDurationMillis = minDurationMillis;
            this.maxDurationMillis = maxDurationMillis;

            this.maxColumn = maxColumn;
            this.maxRow = maxRow;
        }

        public static Criterion position(int maxColumn, int maxRow) {
            return new Criterion(Type.POSITION, NONE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    Long.MIN_VALUE, Long.MAX_VALUE, maxColumn, maxRow);
        }

        public static Criterion fixation(long minDurationMillis) {
            return new Criterion(Type.FIXATION, NONE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    minDurationMillis, Long.MAX_VALUE, -1, -1);
        }

        public static Criterion saccade(Direction direction, int minAmplitude) {
            return saccade(direction, minAmplitude, Integer.MAX_VALUE);
        }

        public static Criterion saccade(Direction direction, int minAmplitude, int maxAmplitude) {
            return new Criterion(Type.SACCADE, direction, minAmplitude, maxAmplitude,
                    Long.MIN_VALUE, Long.MAX_VALUE, -1, -1);
        }

        public static Criterion badContact(long minDurationMillis) {
            return new Criterion(Type.POSITION, NONE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    minDurationMillis, Long.MAX_VALUE, -1, -1);
        }

        public boolean isMatching(EyeEvent event) {
            if (event == null || type != event.type) {
                return false;
            }
            if (direction != NONE && direction != event.direction) {
                return false;
            }
            if (Math.abs(event.amplitude) < minAmplitude
                    || Math.abs(event.amplitude) > maxAmplitude) {
                return false;
            }
            return !(event.durationMillis < minDurationMillis
                    || event.durationMillis > maxDurationMillis);
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

    public EyeEvent(Type type, Direction direction) {
        this(type, direction, 0, 0, -1, -1);
    }

    public EyeEvent(Type type, int amplitude, long durationMillis) {
        this(type, NONE, amplitude, durationMillis, -1, -1);
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
