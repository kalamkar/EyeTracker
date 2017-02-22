package care.dovetail.tracker.ui;

import android.app.Activity;
import android.content.SharedPreferences;
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

    private ToggleButton dayDream;
    private ToggleButton showGestures;
    private ToggleButton showNumbers;
    private ToggleButton showChart;
    private ToggleButton whackAMole;
    private SeekBar numSteps;
    private TextView numStepsValue;
    private SeekBar minQuality;
    private TextView minQualityValue;
    private Spinner algorithm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences(getPackageName(), 0);

        dayDream = (ToggleButton) findViewById(R.id.dayDream);
        showGestures = (ToggleButton) findViewById(R.id.showGestures);
        showNumbers = (ToggleButton) findViewById(R.id.showNumbers);
        showChart = (ToggleButton) findViewById(R.id.showChart);
        whackAMole = (ToggleButton) findViewById(R.id.whackAMole);
        numSteps = (SeekBar) findViewById(R.id.num_steps);
        numStepsValue = (TextView)  findViewById(R.id.num_steps_value);
        minQuality = (SeekBar) findViewById(R.id.min_quality);
        minQualityValue = (TextView) findViewById(R.id.min_quality_value);
        algorithm = (Spinner) findViewById(R.id.algo);

        dayDream.setChecked(settings.isDayDream());
        showGestures.setChecked(settings.shouldShowGestures());
        showNumbers.setChecked(settings.shouldShowNumbers());
        showChart.setChecked(settings.shouldShowChart());
        whackAMole.setChecked(settings.shouldWhackAMole());
        numSteps.setProgress(settings.getNumSteps());
        numStepsValue.setText(Integer.toString(settings.getNumSteps()));
        minQuality.setProgress(settings.getMinQuality());
        minQualityValue.setText(Integer.toString(settings.getMinQuality()));
        algorithm.setSelection(settings.getAlgorithm());

        dayDream.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setDayDream(isChecked);
            }
        });

        showGestures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setShowGestures(isChecked);
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

        minQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                minQualityValue.setText(Integer.toString(progress));
                settings.setMinQuality(progress);
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
    }
}
