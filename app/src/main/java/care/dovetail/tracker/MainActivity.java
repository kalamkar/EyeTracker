package care.dovetail.tracker;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.bluetooth.ShimmerClient;
import care.dovetail.tracker.bluetooth.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.tracker.eog.HybridEogProcessor;
import care.dovetail.tracker.processing.AccelerationProcessor;
import care.dovetail.tracker.processing.BandpassBlinkDetector;
import care.dovetail.tracker.processing.BlinkDetector;
import care.dovetail.tracker.ui.ChartFragment;
import care.dovetail.tracker.ui.FruitFragment;
import care.dovetail.tracker.ui.PositionFragment;
import care.dovetail.tracker.ui.SettingsActivity;

public class MainActivity extends FragmentActivity implements BluetoothDeviceListener,
        AccelerationProcessor.ShakingObserver, EyeEvent.Observer {
    private static final String TAG = "MainActivity";

    private static final int GRAPH_UPDATE_MILLIS = 100;
    private static final int GAZE_UPDATE_MILLIS = 100;
    private static final int MOLE_UPDATE_MILLIS = 2000;

    private final Settings settings = new Settings(this);

    private final ShimmerClient patchClient = new ShimmerClient(this, this);
    private EOGProcessor signals;
    private BlinkDetector blinks;
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Timer chartUpdateTimer;
    private Timer sectorUpdateTimer;
    private Timer moleUpdateTimer;

    private Map<EyeEvent.Type, MediaPlayer> players = new HashMap<>();

    private Pair<Integer, Integer> moleSector = Pair.create(-1, -1);

    private Fragment demo;

    private long lookupStartTimeMillis;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBars();
        updateUIFromSettings();
        startBluetooth();
        players.put(EyeEvent.Type.GESTURE, MediaPlayer.create(this, R.raw.slice));
        players.put(EyeEvent.Type.LARGE_BLINK, MediaPlayer.create(this, R.raw.beep));
        accelerometer.start();
    }

    @Override
    protected void onStop() {
        stopBluetooth();
        for (MediaPlayer player : players.values()) {
            player.release();
        }
        players.clear();
        accelerometer.stop();
        super.onStop();
    }

    private void updateUIFromSettings() {
        if (settings.isDayDream()) {
            findViewById(R.id.leftDebug).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            findViewById(R.id.rightDebug).setPadding(
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                    getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
        }

        findViewById(R.id.leftNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.rightNumber).setVisibility(
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onConnect(String name) {
        Log.i(TAG, String.format("Connected to %s", name));
        writer = new FileDataWriter(this);
        showQualityProgress();
    }

    @Override
    public void onDisconnect(String name) {
        Log.i(TAG, String.format("Disconnected from %s", name));
        writer.close();
        writer = null;
        hideAll();
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
            }

            if (settings.shouldShowBlinks() || !signals.isGoodSignal()) {
                leftChart.updateChannel3(blinks.blinks(), blinks.blinkRange());
                rightChart.updateChannel3(blinks.blinks(), blinks.blinkRange());
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
            Pair<Integer, Integer> sector = signals.getSector();
            onEyeEvent(new EyeEvent(EyeEvent.Type.POSITION, sector.first, sector.second));

            if (signals.isGoodSignal()) {
                showDebugNumbers();
            } else {
//                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
                showQualityProgress();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int quality = signals.getSignalQuality();
                    ((ProgressBar) findViewById(R.id.leftProgress)).setProgress(quality);
                    ((ProgressBar) findViewById(R.id.rightProgress)).setProgress(quality);

                    boolean isStableSignal =
                            signals.isStableHorizontal() && signals.isStableVertical();
                    findViewById(R.id.leftWarning).setVisibility(
                            isStableSignal ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.rightWarning).setVisibility(
                            isStableSignal ?  View.INVISIBLE : View.VISIBLE);
                }
            });
        }
    }

    private class MoleUpdater extends TimerTask {
        @Override
        public void run() {
            // Add some randomness so that its updating every 2 or 4 seconds.
            if (Stats.random(0, 2) != 0) {
                return;
            }
            moleSector = Pair.create(Stats.random(0, Config.MOLE_NUM_STEPS),
                    Stats.random(0, Config.MOLE_NUM_STEPS));
            onEyeEvent(new EyeEvent(
                    EyeEvent.Type.WHACKAMOLE_POSITION, moleSector.first, moleSector.second));
        }
    }

    @Override
    public void onEyeEvent(EyeEvent event) {
        MediaPlayer player = players.get(event.type);
        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.start();
        }
        if (EyeEvent.Type.BAD_CONTACT == event.type && patchClient.isConnected()) {
            stopBluetooth();
            startBluetooth();
        }
        ((EyeEvent.Observer) demo).onEyeEvent(event);
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        blinks.update(channel2);
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
        long millisSinceLookup = System.currentTimeMillis() - lookupStartTimeMillis;
        if (!isShaking || millisSinceLookup < 5000) {
            return;
        }
        if (patchClient.isConnected()) {
            stopBluetooth();
            startBluetooth();
        } else {
            startBluetooth();
        }
    }

    public void startBluetooth() {
        blinks = new BandpassBlinkDetector();
        blinks.addObserver(this);
        if (settings.getDemo() == 0) { // Gestures
            demo = new FruitFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.demo, demo).commit();
        } else if (settings.getDemo() == 1) { // Position
            demo = new PositionFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.demo, demo).commit();
        }
        signals = new HybridEogProcessor(this, settings.getNumSteps(), settings.getThreshold());
        patchClient.connect();
        lookupStartTimeMillis = System.currentTimeMillis();
        showBluetoothSpinner();

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(new SectorUpdater(), 0, GAZE_UPDATE_MILLIS);

        if (settings.shouldWhackAMole()) {
            moleUpdateTimer = new Timer();
            moleUpdateTimer.schedule(new MoleUpdater(), 0, MOLE_UPDATE_MILLIS);
        }
    }

    public void stopBluetooth() {
        patchClient.close();
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        if (sectorUpdateTimer != null) {
            sectorUpdateTimer.cancel();
        }
        if (moleUpdateTimer != null) {
            moleUpdateTimer.cancel();
        }
        moleSector = Pair.create(-1, -1);
    }

    private void hideBars() {
        findViewById(R.id.content).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showQualityProgress() {
         updateStatusUI(View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
    }

    private void showDebugNumbers() {
        updateStatusUI(View.INVISIBLE, View.INVISIBLE,
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
    }

    private void showBluetoothSpinner() {
        updateStatusUI(View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    private void hideAll() {
        updateStatusUI(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    private void updateStatusUI(final int spinner, final int progress, final int numbers) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.leftBlue).setVisibility(spinner);
                findViewById(R.id.rightBlue).setVisibility(spinner);

                findViewById(R.id.leftProgress).setVisibility(progress);
                findViewById(R.id.rightProgress).setVisibility(progress);
                findViewById(R.id.leftProgressLabel).setVisibility(progress);
                findViewById(R.id.rightProgressLabel).setVisibility(progress);

                findViewById(R.id.leftNumber).setVisibility(numbers);
                findViewById(R.id.rightNumber).setVisibility(numbers);
            }
        });
    }
}
