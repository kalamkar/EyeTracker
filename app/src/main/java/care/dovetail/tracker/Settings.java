package care.dovetail.tracker;

import android.content.Context;

/**
 * Created by abhi on 2/10/17.
 */

public class Settings {
    private static final String TAG = "Settings";

    private static final String SHOW_CHART = "show_chart";
    private static final String SHOW_BLINKS = "show_blinks";
    private static final String WHACK_A_MOLE = "whack_a_mole";
    private static final String BLINK_TO_GAZE = "blink_to_gaze";
    private static final String V_TO_H = "v_to_h";
    private static final String NUM_STEPS = "num_steps";

    private final Context context;

    public Settings(Context context) {
        this.context = context;
    }

    public boolean shouldShowChart() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_CHART, true);
    }

    public void setShowChart(boolean showChart) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_CHART, showChart).apply();
    }

    public boolean shouldShowBlinks() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getBoolean(SHOW_BLINKS, true);
    }

    public void setShowBlinks(boolean showBlinks) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putBoolean(SHOW_BLINKS, showBlinks).apply();
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

    public float getBlinkToGaze() {
        return context.getSharedPreferences(
                context.getPackageName(), 0).getFloat(BLINK_TO_GAZE, 0.6f);
    }

    public void setBlinkToGaze(float blinkToGaze) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putFloat(BLINK_TO_GAZE, blinkToGaze).apply();
    }

    public float getVtoH() {
        return context.getSharedPreferences(context.getPackageName(), 0).getFloat(V_TO_H, 0.7f);
    }

    public void setvToH(float vToH) {
        context.getSharedPreferences(
                context.getPackageName(), 0).edit().putFloat(V_TO_H, vToH).apply();
    }
}
