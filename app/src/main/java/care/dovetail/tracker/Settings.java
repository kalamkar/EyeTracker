package care.dovetail.tracker;

import android.content.Context;

/**
 * Created by abhi on 2/10/17.
 */

public class Settings {
    private static final String TAG = "Settings";

    private static final String DAY_DREAM = "DAY_DREAM";
    private static final String SHOW_BLINKMARKS = "show_blinkmarks";
    private static final String SHOW_NUMBERS = "show_numbers";
    private static final String SHOW_CHART = "show_chart";
    private static final String WHACK_A_MOLE = "whack_a_mole";
    private static final String NUM_STEPS = "num_steps";
    private static final String ALGORITHM = "algorithm";
    private static final String CURSOR_STYLE = "cursor_style";
    private static final String GRAPH_HEIGHT = "graph_height";

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

    public boolean shouldShowBlinkmarks() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_BLINKMARKS, false);
    }

    public void setShowBlinkmarks(boolean showBlinkmarks) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_BLINKMARKS, showBlinkmarks).apply();
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
        return false; // shouldShowChart();
    }

    public boolean shouldWhackAMole() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(WHACK_A_MOLE, false);
    }

    public void setWhackAMole(boolean whackAMole) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(WHACK_A_MOLE, whackAMole).apply();
    }

    public int getNumSteps() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(NUM_STEPS, 5);
    }

    public void setNumSteps(int numSteps) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(NUM_STEPS, numSteps).apply();
    }

    public int getGraphHeight() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(GRAPH_HEIGHT, 3000);
    }

    public void setGraphHeight(int graphHeight) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(GRAPH_HEIGHT, graphHeight).apply();
    }

    public int getAlgorithm() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(ALGORITHM, 0);
    }

    public void setAlgorithm(int algorithm) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(ALGORITHM, algorithm).apply();
    }

    public int getCursorStyle() {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(CURSOR_STYLE, 0);
    }

    public void setCursorStyle(int cursor) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putInt(CURSOR_STYLE, cursor).apply();
    }
}
