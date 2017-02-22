package care.dovetail.tracker;

import android.content.Context;

/**
 * Created by abhi on 2/10/17.
 */

public class Settings {
    private static final String TAG = "Settings";

    private static final String DAY_DREAM = "DAY_DREAM";
    private static final String SHOW_GESTURES = "show_gestures";
    private static final String SHOW_NUMBERS = "show_numbers";
    private static final String SHOW_CHART = "show_chart";
    private static final String WHACK_A_MOLE = "whack_a_mole";
    private static final String NUM_STEPS = "num_steps";
    private static final String MIN_QUALITY = "min_quality";
    private static final String ALGORITHM = "algorithm";

    private final Context context;

    public Settings(Context context) {
        this.context = context;
    }

    public boolean isDayDream() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(DAY_DREAM, true);
    }

    public void setDayDream(boolean dayDream) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(DAY_DREAM, dayDream).apply();
    }

    public boolean shouldShowGestures() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_GESTURES, true);
    }

    public void setShowGestures(boolean showNumbers) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_GESTURES, showNumbers).apply();
    }

    public boolean shouldShowNumbers() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_NUMBERS, true);
    }

    public void setShowNumbers(boolean showNumbers) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_NUMBERS, showNumbers).apply();
    }

    public boolean shouldShowChart() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_CHART, false);
    }

    public void setShowChart(boolean showChart) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_CHART, showChart).apply();
    }

    public boolean shouldShowBlinks() {
        return false;
    }

    public boolean shouldWhackAMole() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(WHACK_A_MOLE, false);
    }

    public void setWhackAMole(boolean whackAMole) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(WHACK_A_MOLE, whackAMole).apply();
    }

    public boolean shouldShowAccel() {
        return false;
    }

    public int getNumSteps() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(NUM_STEPS, 5);
    }

    public void setNumSteps(int numSteps) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(NUM_STEPS, numSteps).apply();
    }

    public int getMinQuality() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(MIN_QUALITY, 50);
    }

    public void setMinQuality(int minQuality) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(MIN_QUALITY, minQuality).apply();
    }

    public int getAlgorithm() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(ALGORITHM, 0);
    }

    public void setAlgorithm(int algorithm) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(ALGORITHM, algorithm).apply();
    }
}
