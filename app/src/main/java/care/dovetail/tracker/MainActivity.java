package care.dovetail.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.bluetooth.ShimmerClient;
import care.dovetail.tracker.bluetooth.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.tracker.processing.AccelerationProcessor;
import care.dovetail.tracker.processing.BandpassSignalProcessor;
import care.dovetail.tracker.processing.CurveFitSignalProcessor;
import care.dovetail.tracker.processing.Feature;
import care.dovetail.tracker.processing.SignalProcessor;
import care.dovetail.tracker.processing.SignalProcessor4;
import care.dovetail.tracker.ui.ChartFragment;
import care.dovetail.tracker.ui.GridView;
import care.dovetail.tracker.ui.SettingsActivity;

public class MainActivity extends Activity implements BluetoothDeviceListener,
        SignalProcessor.FeatureObserver, AccelerationProcessor.ShakingObserver {
    private static final String TAG = "MainActivity";

    private static final int GRAPH_UPDATE_MILLIS = 100;
    private static final int GAZE_UPDATE_MILLIS = 100;

    private final Settings settings = new Settings(this);

    private final ShimmerClient patchClient = new ShimmerClient(this, this);
    private SignalProcessor signals;
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Timer chartUpdateTimer;
    private Timer sectorUpdateTimer;

    private Ringtone ringtone;

    private Pair<Integer, Integer> moleSector = Pair.create(-1, -1);
    private static final int MOLE_NUM_STEPS = 5;

    private GridView leftGrid;
    private GridView rightGrid;
    private GridView leftMoleGrid;
    private GridView rightMoleGrid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        findViewById(R.id.settings).setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        stopBluetooth();
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        return true;
                    }
                });

        accelerometer = new AccelerationProcessor(
                (SensorManager) getSystemService(Context.SENSOR_SERVICE), this);

        leftGrid = (GridView) findViewById(R.id.leftGrid);
        rightGrid = (GridView) findViewById(R.id.rightGrid);
        leftMoleGrid = (GridView) findViewById(R.id.leftMoleGrid);
        rightMoleGrid = (GridView) findViewById(R.id.rightMoleGrid);
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBars();
        updateUIFromSettings();
        startBluetooth();
        showDualView(false);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        accelerometer.start();
    }

    @Override
    protected void onStop() {
        stopBluetooth();
        ringtone.stop();
        accelerometer.stop();
        super.onStop();
    }

    private void updateUIFromSettings() {
        if (settings.isDayDream()) {
            findViewById(R.id.left).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            findViewById(R.id.right).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
        }

        int numSteps = settings.getNumSteps();
        ((GridView) findViewById(R.id.leftGrid)).setNumSteps(numSteps);
        ((GridView) findViewById(R.id.rightGrid)).setNumSteps(numSteps);

        findViewById(R.id.leftNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.rightNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);

        if (settings.shouldWhackAMole()) {
            leftGrid.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            rightGrid.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            leftMoleGrid.setCursorStyle(GridView.CursorStyle.RECTANGLE);
            rightMoleGrid.setCursorStyle(GridView.CursorStyle.RECTANGLE);

            leftMoleGrid.setNumSteps(MOLE_NUM_STEPS);
            rightMoleGrid.setNumSteps(MOLE_NUM_STEPS);
        } else {
            leftGrid.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
            rightGrid.setCursorStyle(GridView.CursorStyle.values()[settings.getCursorStyle()]);
        }
    }

    @Override
    public void onConnect(String name) {
        Log.i(TAG, String.format("Connected to %s", name));
        writer = new FileDataWriter(this);
        showDualView(true);
    }

    @Override
    public void onDisconnect(String name) {
        Log.i(TAG, String.format("Disconnected from %s", name));
        writer.close();
        writer = null;
        showDualView(false);
    }

    private class ChartUpdater extends TimerTask {
        @Override
        public void run() {
            final ChartFragment leftChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.leftChart);
            final ChartFragment rightChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.rightChart);
            leftChart.clear();
            rightChart.clear();

            if (settings.shouldShowChart()) {
                leftChart.updateChannel1(signals.horizontal(), signals.horizontalRange());
                leftChart.updateChannel2(signals.vertical(), signals.verticalRange());
                rightChart.updateChannel1(signals.horizontal(), signals.horizontalRange());
                rightChart.updateChannel2(signals.vertical(), signals.verticalRange());

                leftChart.updateFeature1(signals.feature1(), signals.horizontalRange());
                rightChart.updateFeature1(signals.feature1(), signals.horizontalRange());

                leftChart.updateFeature2(signals.feature2(), signals.verticalRange());
                rightChart.updateFeature2(signals.feature2(), signals.verticalRange());
            }

            if (settings.shouldShowBlinks() || !signals.isGoodSignal()) {
                leftChart.updateChannel3(signals.blinks(), signals.blinkRange());
                rightChart.updateChannel3(signals.blinks(), signals.blinkRange());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!leftChart.isResumed() || !rightChart.isResumed()) {
                        return;
                    }
                    leftChart.updateUI();
                    rightChart.updateUI();
                    String numbers = signals.getDebugNumbers();
                    ((TextView) findViewById(R.id.leftNumber)).setText(numbers);
                    ((TextView) findViewById(R.id.rightNumber)).setText(numbers);
                }
            });
        }
    }

    private class SectorUpdater extends TimerTask {
        @Override
        public void run() {
            if (signals.isBadContact() && patchClient.isConnected()) {
                stopBluetooth();
                startBluetooth();
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int quality = signals.getSignalQuality();
                    boolean isGoodSignal = signals.isGoodSignal();
                    Pair<Integer, Integer> sector = Pair.create(-1, -1);
                    if (isGoodSignal) {
                        sector = signals.getSector();
                    }

                    boolean showProgress = isGoodSignal || !patchClient.isConnected();

                    findViewById(R.id.leftProgress).setVisibility(
                            showProgress ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.rightProgress).setVisibility(
                            showProgress ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.leftProgressLabel).setVisibility(
                            showProgress ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.rightProgressLabel).setVisibility(
                            showProgress ?  View.INVISIBLE : View.VISIBLE);

                    findViewById(R.id.leftNumber).setVisibility(
                            showProgress ?  View.VISIBLE : View.INVISIBLE);
                    findViewById(R.id.rightNumber).setVisibility(
                            showProgress ?  View.VISIBLE : View.INVISIBLE);

                    boolean isStableSignal =
                            signals.isStableHorizontal() && signals.isStableVertical();
                    findViewById(R.id.leftWarning).setVisibility(
                            isStableSignal ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.rightWarning).setVisibility(
                            isStableSignal ?  View.INVISIBLE : View.VISIBLE);

                    ((ProgressBar) findViewById(R.id.leftProgress)).setProgress(quality);
                    ((ProgressBar) findViewById(R.id.rightProgress)).setProgress(quality);

                    leftGrid.highlight(sector.first, sector.second);
                    rightGrid.highlight(sector.first, sector.second);
                }
            });
        }
    }

    @Override
    public void onFeature(final Feature feature) {
        if (Feature.Type.BLINK == feature.type && signals.isGoodSignal()) {
            ringtone.play();
            if (settings.shouldShowBlinkmarks()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        leftGrid.mark(feature.sector.first, feature.sector.second);
                        rightGrid.mark(feature.sector.first, feature.sector.second);
                    }
                });
            }
            if (settings.shouldWhackAMole()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        moleSector = Pair.create(Stats.random(0, MOLE_NUM_STEPS),
                                Stats.random(0, MOLE_NUM_STEPS));
                        leftMoleGrid.highlight(moleSector.first, moleSector.second);
                        rightMoleGrid.highlight(moleSector.first, moleSector.second);
                    }
                });
            }
        }
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        signals.update(channel1, channel2);
        if (writer != null) {
            Pair<Integer, Integer> estimate = signals.getSector();
            int filtered1 = signals.horizontal()[Config.GRAPH_LENGTH-1];
            int filtered2 = signals.vertical()[Config.GRAPH_LENGTH-1];
            writer.write(channel1, channel2, filtered1, filtered2, estimate.first, estimate.second,
                    moleSector.first, moleSector.second);
        }
    }

    @Override
    public void onShakingChange(final boolean isShaking) {
        if (isShaking && patchClient.isConnected()) {
            stopBluetooth();
            startBluetooth();
        }
    }

    private void showDualView(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.isDestroyed()) {
                    return;
                }
                leftGrid.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                rightGrid.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                leftMoleGrid.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                rightMoleGrid.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.leftBlue).setVisibility(
                        show ?  View.INVISIBLE : View.VISIBLE);
                findViewById(R.id.rightBlue).setVisibility(
                        show ?  View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    public void startBluetooth() {
        switch (settings.getAlgorithm()) {
            default:
            case 0:
                signals = new BandpassSignalProcessor(this, settings.getNumSteps());
                break;
            case 1:
                signals = new CurveFitSignalProcessor(this, settings.getNumSteps());
                break;
            case 2:
                signals = new SignalProcessor4(this, settings.getNumSteps());
                break;
        }
        patchClient.connect();

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(new SectorUpdater(), 0, GAZE_UPDATE_MILLIS);
    }

    public void stopBluetooth() {
        patchClient.close();
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        if (sectorUpdateTimer != null) {
            sectorUpdateTimer.cancel();
        }
        moleSector = Pair.create(-1, -1);
    }

    private void hideBars() {
        findViewById(R.id.binocular).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
