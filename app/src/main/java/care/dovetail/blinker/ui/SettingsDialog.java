package care.dovetail.blinker.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import care.dovetail.blinker.Config;
import care.dovetail.blinker.MainActivity;
import care.dovetail.blinker.R;

/**
 * Created by abhi on 2/6/17.
 */

public class SettingsDialog extends DialogFragment {
    private final static String TAG = "SettingsDialog";

    private ToggleButton showChart;
    private SeekBar numSteps;
    private TextView numStepsValue;
    private SeekBar blinkToGaze;
    private TextView blinkToGazeValue;
    private SeekBar verticalToHorizontal;
    private TextView vToHValue;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_settings, container);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs =
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0);

        showChart = (ToggleButton) view.findViewById(R.id.showChart);
        numSteps = (SeekBar) view.findViewById(R.id.num_steps);
        numStepsValue = (TextView)  view.findViewById(R.id.num_steps_value);
        blinkToGaze = (SeekBar) view.findViewById(R.id.blink_to_gaze);
        blinkToGazeValue = (TextView)  view.findViewById(R.id.blink_to_gaze_value);
        verticalToHorizontal = (SeekBar) view.findViewById(R.id.v_to_h);
        vToHValue = (TextView)  view.findViewById(R.id.v_to_h_value);

        showChart.setChecked(prefs.getBoolean(Config.SHOW_CHART, true));
        numSteps.setProgress(prefs.getInt(Config.PREF_NUM_STEPS, 5));
        numStepsValue.setText(Integer.toString(prefs.getInt(Config.PREF_NUM_STEPS, 5)));
        blinkToGaze.setProgress((int) (prefs.getFloat(Config.PREF_BLINK_TO_GAZE, 0.6f) * 10));
        blinkToGazeValue.setText(Float.toString(prefs.getFloat(Config.PREF_BLINK_TO_GAZE, 0.6f)));
        verticalToHorizontal.setProgress((int) (prefs.getFloat(Config.PREF_V_TO_H, 0.7f) * 10));
        vToHValue.setText(Float.toString(prefs.getFloat(Config.PREF_V_TO_H, 0.7f)));

        showChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                        .edit().putBoolean(Config.SHOW_CHART, isChecked).apply();
            }
        });

        numSteps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numStepsValue.setText(Integer.toString(progress));
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                        .edit().putInt(Config.PREF_NUM_STEPS, progress).apply();
            }

            @Override  public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override  public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blinkToGaze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blinkToGazeValue.setText(Float.toString((float) progress / 10));
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                        .edit().putFloat(Config.PREF_BLINK_TO_GAZE, (float) progress / 10).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        verticalToHorizontal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vToHValue.setText(Float.toString((float) progress / 10));
                getActivity().getSharedPreferences(getActivity().getPackageName(), 0)
                        .edit().putFloat(Config.PREF_V_TO_H, (float) progress / 10).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ((MainActivity) getActivity()).startBluetooth();
        getActivity().findViewById(R.id.binocular).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        super.onDismiss(dialog);
    }
}
