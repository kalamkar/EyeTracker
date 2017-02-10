package care.dovetail.tracker.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
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

import care.dovetail.tracker.MainActivity;
import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 2/6/17.
 */

public class SettingsDialog extends DialogFragment {
    private final static String TAG = "SettingsDialog";

    private Settings settings;

    private ToggleButton showChart;
    private ToggleButton showBlinks;
    private SeekBar numSteps;
    private TextView numStepsValue;
    private SeekBar blinkToGaze;
    private TextView blinkToGazeValue;
    private SeekBar verticalToHorizontal;
    private TextView vToHValue;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        settings = new Settings(context);
    }

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
        showBlinks = (ToggleButton) view.findViewById(R.id.showBlinks);
        numSteps = (SeekBar) view.findViewById(R.id.num_steps);
        numStepsValue = (TextView)  view.findViewById(R.id.num_steps_value);
        blinkToGaze = (SeekBar) view.findViewById(R.id.blink_to_gaze);
        blinkToGazeValue = (TextView)  view.findViewById(R.id.blink_to_gaze_value);
        verticalToHorizontal = (SeekBar) view.findViewById(R.id.v_to_h);
        vToHValue = (TextView)  view.findViewById(R.id.v_to_h_value);

        showChart.setChecked(settings.shouldShowChart());
        showBlinks.setChecked(settings.shouldShowBlinks());
        numSteps.setProgress(settings.getNumSteps());
        numStepsValue.setText(Integer.toString(settings.getNumSteps()));
        blinkToGaze.setProgress((int) (settings.getBlinkToGaze() * 10));
        blinkToGazeValue.setText(Float.toString(settings.getBlinkToGaze()));
        verticalToHorizontal.setProgress((int) (settings.getVtoH() * 10));
        vToHValue.setText(Float.toString(settings.getVtoH()));

        showChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowChart(isChecked);
            }
        });

        showBlinks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowBlinks(isChecked);
            }
        });

        numSteps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                numStepsValue.setText(Integer.toString(progress));
                settings.setNumSteps(progress);
            }

            @Override  public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override  public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blinkToGaze.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                blinkToGazeValue.setText(Float.toString((float) progress / 10));
                settings.setBlinkToGaze((float) progress / 10);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        verticalToHorizontal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vToHValue.setText(Float.toString((float) progress / 10));
                settings.setvToH((float) progress / 10);
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
