package care.dovetail.tracker.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import care.dovetail.tracker.R;
import care.dovetail.tracker.Settings;

/**
 * Created by abhi on 2/6/17.
 */

public class SettingsActivity extends Activity {
    private final static String TAG = "SettingsActivity";

    private final Settings settings = new Settings(this);

    private TextView numStepsValue;
    private TextView graphHeightValue;
    private TextView thresholdValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ToggleButton dayDream = (ToggleButton) findViewById(R.id.dayDream);
        ToggleButton showBlinkmarks = (ToggleButton) findViewById(R.id.showBlinkmarks);
        ToggleButton showNumbers = (ToggleButton) findViewById(R.id.showNumbers);
        ToggleButton showChart = (ToggleButton) findViewById(R.id.showChart);
        ToggleButton whackAMole = (ToggleButton) findViewById(R.id.whackAMole);
        Spinner algorithm = (Spinner) findViewById(R.id.algo);
        Spinner cursor = (Spinner) findViewById(R.id.cursor);

        SeekBar numSteps = (SeekBar) findViewById(R.id.num_steps);
        numStepsValue = (TextView)  findViewById(R.id.num_steps_value);

        SeekBar graphHeight = (SeekBar) findViewById(R.id.graph_height);
        graphHeightValue = (TextView)  findViewById(R.id.graph_height_value);

        SeekBar threshold = (SeekBar) findViewById(R.id.threshold);
        thresholdValue = (TextView)  findViewById(R.id.threshold_value);


        dayDream.setChecked(settings.isDayDream());
        showBlinkmarks.setChecked(settings.shouldShowBlinkmarks());
        showNumbers.setChecked(settings.shouldShowNumbers());
        showChart.setChecked(settings.shouldShowChart());
        whackAMole.setChecked(settings.shouldWhackAMole());
        algorithm.setSelection(settings.getAlgorithm());
        cursor.setSelection(settings.getCursorStyle());

        numSteps.setProgress(settings.getNumSteps());
        numStepsValue.setText(Integer.toString(settings.getNumSteps()));

        graphHeight.setProgress(settings.getGraphHeight());
        graphHeightValue.setText(Integer.toString(settings.getGraphHeight()));

        threshold.setProgress(settings.getThreshold());
        thresholdValue.setText(Integer.toString(settings.getThreshold()));

        dayDream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setDayDream(isChecked);
            }
        });

        showBlinkmarks.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowBlinkmarks(isChecked);
            }
        });

        showNumbers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowNumbers(isChecked);
            }
        });

        showChart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowChart(isChecked);
            }
        });

        whackAMole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setWhackAMole(isChecked);
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

        graphHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                graphHeightValue.setText(Integer.toString(progress));
                settings.setGraphHeight(progress);
            }

            @Override  public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override  public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        algorithm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.setAlgorithm(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        cursor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.setCursorStyle(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdValue.setText(Integer.toString(progress));
                settings.setThreshold(progress);
            }

            @Override  public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override  public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
