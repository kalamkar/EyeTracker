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
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.tracker.bluetooth.ShimmerClient;
import care.dovetail.tracker.bluetooth.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.tracker.processing.AccelerationProcessor;
import care.dovetail.tracker.processing.Feature;
import care.dovetail.tracker.processing.SignalProcessor;
import care.dovetail.tracker.ui.ChartFragment;
import care.dovetail.tracker.ui.GridView;
import care.dovetail.tracker.ui.SettingsActivity;

public class MainActivity extends Activity implements BluetoothDeviceListener,
        SignalProcessor.FeatureObserver, AccelerationProcessor.ShakingObserver {
    private static final String TAG = "MainActivity";

    private static final int GRAPH_UPDATE_MILLIS = 100;
    private static final int GAZE_UPDATE_MILLIS = 100;
    private static final int MIN_SIGNAL_QUALITY = 50;

    private final Settings settings = new Settings(this);

    private ShimmerClient patchClient;
    private SignalProcessor signals;
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Timer chartUpdateTimer;
    private Timer sectorUpdateTimer;

    private Ringtone ringtone;

    private Pair<Integer, Integer> moleSector = null;
    private int moleChangeCount = 0;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBars();
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
        chartUpdateTimer.cancel();
        sectorUpdateTimer.cancel();
        if (patchClient != null) {
            patchClient.connect();
        }
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
                leftChart.updateChannel1(signals.channel1(), signals.range1());
                leftChart.updateChannel2(signals.channel2(), signals.range2());
                rightChart.updateChannel1(signals.channel1(), signals.range1());
                rightChart.updateChannel2(signals.channel2(), signals.range2());

                leftChart.updateFeature1(signals.feature1(), signals.range2());
                rightChart.updateFeature1(signals.feature1(), signals.range2());

                leftChart.updateFeature2(signals.feature2(), signals.range2());
                rightChart.updateFeature2(signals.feature2(), signals.range2());
            }

            if (settings.shouldShowBlinks()) {
                leftChart.updateChannel3(signals.blinks(), signals.blinkRange());
                rightChart.updateChannel3(signals.blinks(), signals.blinkRange());
            } else if (settings.shouldShowAccel()) {
                leftChart.updateChannel3(accelerometer.getY(), Pair.create(-1000, 1000));
                rightChart.updateChannel3(accelerometer.getY(), Pair.create(-1000, 1000));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!leftChart.isResumed() || !rightChart.isResumed()) {
                        return;
                    }
                    leftChart.updateUI();
                    rightChart.updateUI();
                    String numbers = String.format("%d\n%d,%d", signals.getHalfGraphHeight(),
                            signals.getNumBlinks(), signals.getSignalQuality());
                    ((TextView) findViewById(R.id.leftNumber)).setText(numbers);
                    ((TextView) findViewById(R.id.rightNumber)).setText(numbers);
                }
            });
        }
    }

    private class SectorUpdater extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean isGoodSignal = signals.getSignalQuality() > MIN_SIGNAL_QUALITY;
                    Pair<Integer, Integer> sector = null;
                    if (settings.shouldWhackAMole()) {
                        if (moleChangeCount == 0) {
                            moleSector = Pair.create(Utils.random(0, settings.getNumSteps()),
                                    Utils.random(0, settings.getNumSteps()));
                            moleChangeCount = Utils.random(10, 50); // 1 to 5 seconds
                        } else {
                            moleChangeCount--;
                        }
                        sector = moleSector;
                    } else if (isGoodSignal) {
                        sector = signals.getSector();
                    } else {
                        int numSteps = settings.getNumSteps();
                        sector = Pair.create(numSteps / 2, numSteps / 2);
                    }
                    findViewById(R.id.leftWarning).setVisibility(
                            isGoodSignal ?  View.INVISIBLE : View.VISIBLE);
                    findViewById(R.id.rightWarning).setVisibility(
                            isGoodSignal ?  View.INVISIBLE : View.VISIBLE);
                    GridView leftGrid = (GridView) findViewById(R.id.leftGrid);
                    leftGrid.highlight(sector.first, sector.second);
                    GridView rightGrid = (GridView) findViewById(R.id.rightGrid);
                    rightGrid.highlight(sector.first, sector.second);
                }
            });
        }
    }

    @Override
    public void onFeature(Feature feature) {
        if (Feature.Type.BLINK == feature.type) {
            ringtone.play();
        }
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        signals.update(channel1, channel2);
        if (writer != null) {
            Pair<Integer, Integer> estimate = signals.getSector();
            int filtered1 = signals.channel1()[Config.GRAPH_LENGTH-1];
            int filtered2 = signals.channel2()[Config.GRAPH_LENGTH-1];
            writer.write(channel1, channel2, filtered1, filtered2, estimate.first, estimate.second,
                    moleSector == null ? -1 : moleSector.first,
                    moleSector == null ? -1 : moleSector.second);
        }
    }

    @Override
    public void onShakingChange(final boolean isShaking) {
        if (isShaking) {
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
                int numSteps = settings.getNumSteps();
                ((GridView) findViewById(R.id.leftGrid)).setNumSteps(numSteps);
                ((GridView) findViewById(R.id.rightGrid)).setNumSteps(numSteps);

                findViewById(R.id.leftGrid).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.rightGrid).setVisibility(show ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.leftNumber).setVisibility(
                        show && settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.rightNumber).setVisibility(
                        show && settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
                findViewById(R.id.leftProgress).setVisibility(
                        show ?  View.INVISIBLE : View.VISIBLE);
                findViewById(R.id.rightProgress).setVisibility(
                        show ?  View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    public void startBluetooth() {
        signals = new SignalProcessor(this, settings.getNumSteps(), settings.getBlinkToGaze(),
                settings.getVtoH());

        // TODO(abhi): Create patchClient in onActivityResult if BT enable activity started.
        patchClient = new ShimmerClient(this, this);
        patchClient.connect();

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new ChartUpdater(), 0, GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(new SectorUpdater(), 0, GAZE_UPDATE_MILLIS);
    }

    public void stopBluetooth() {
        if (patchClient != null) {
            patchClient.close();
            patchClient = null;
        }
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        if (sectorUpdateTimer != null) {
            sectorUpdateTimer.cancel();
        }
        moleChangeCount = 0;
        moleSector = null;
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
