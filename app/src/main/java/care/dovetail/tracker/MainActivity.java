package care.dovetail.tracker;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
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
import care.dovetail.tracker.eog.GestureObserver;
import care.dovetail.tracker.eog.GestureRecognizer;
import care.dovetail.tracker.eog.HybridEogProcessor;
import care.dovetail.tracker.processing.AccelerationProcessor;
import care.dovetail.tracker.processing.BandpassBlinkDetector;
import care.dovetail.tracker.processing.BandpassSignalProcessor;
import care.dovetail.tracker.processing.BlinkDetector;
import care.dovetail.tracker.processing.CurveFitSignalProcessor;
import care.dovetail.tracker.processing.EOGProcessor;
import care.dovetail.tracker.processing.Feature;
import care.dovetail.tracker.processing.MedianDiffDiffEOGProcessor;
import care.dovetail.tracker.ui.ChartFragment;
import care.dovetail.tracker.ui.GestureBookPagerAdapter;
import care.dovetail.tracker.ui.GridView;
import care.dovetail.tracker.ui.SettingsActivity;

public class MainActivity extends FragmentActivity implements BluetoothDeviceListener,
        Feature.FeatureObserver, AccelerationProcessor.ShakingObserver, GestureObserver {
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

    private Ringtone ringtone;

    private Pair<Integer, Integer> moleSector = Pair.create(-1, -1);
    private static final int MOLE_NUM_STEPS = 5;

    private GridView leftGrid;
    private GridView rightGrid;
    private GridView leftMoleGrid;
    private GridView rightMoleGrid;
    private ViewPager leftPager;
    private ViewPager rightPager;

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

        leftGrid = (GridView) findViewById(R.id.leftGrid);
        rightGrid = (GridView) findViewById(R.id.rightGrid);
        leftMoleGrid = (GridView) findViewById(R.id.leftMoleGrid);
        rightMoleGrid = (GridView) findViewById(R.id.rightMoleGrid);
        leftPager = (ViewPager) findViewById(R.id.leftPager);
        rightPager = (ViewPager) findViewById(R.id.rightPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBars();
        updateUIFromSettings();
        startBluetooth();
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
            for (int id : new int[]{R.id.leftDebug, R.id.leftTracking, R.id.leftGestures}){
                findViewById(id).setPadding(
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_left),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            }
            for (int id : new int[]{R.id.rightDebug, R.id.rightTracking, R.id.rightGestures}) {
                findViewById(id).setPadding(
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_middle),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_top),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_right),
                        getResources().getDimensionPixelOffset(R.dimen.daydream_padding_bottom));
            }
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

        if (settings.getAlgorithm() == 2) { // Gesture Recognizer
            findViewById(R.id.gestures).setVisibility(View.VISIBLE);
            findViewById(R.id.eye_tracking).setVisibility(View.INVISIBLE);
            leftPager.setAdapter(new GestureBookPagerAdapter(this, getSupportFragmentManager()));
            rightPager.setAdapter(new GestureBookPagerAdapter(this, getSupportFragmentManager()));
        } else {
            findViewById(R.id.gestures).setVisibility(View.INVISIBLE);
            findViewById(R.id.eye_tracking).setVisibility(View.VISIBLE);
        }
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Pair<Integer, Integer> sector = signals.isGoodSignal()
                            ? signals.getSector() : Pair.create(-1, -1);
                    leftGrid.highlight(sector.first, sector.second);
                    rightGrid.highlight(sector.first, sector.second);

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

    @Override
    public void onFeature(final Feature feature) {
//        Log.d(TAG, String.format("Got feature %s", feature.type.toString()));
        if (Feature.Type.BLINK == feature.type) {
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
        } else if (Feature.Type.BAD_CONTACT == feature.type && patchClient.isConnected()) {
            stopBluetooth();
            startBluetooth();
        } else if (Feature.Type.BAD_SIGNAL == feature.type) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(200);
            showQualityProgress();
        } else if (Feature.Type.GOOD_SIGNAL == feature.type) {
            showDebugNumbers();
        }
    }

    @Override
    public void onGesture(final Direction direction, final int amplitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (direction == Direction.LEFT) {
                    leftPager.setCurrentItem(leftPager.getCurrentItem() + 1);
                    rightPager.setCurrentItem(rightPager.getCurrentItem() + 1);
                } else {
                    leftPager.setCurrentItem(leftPager.getCurrentItem() - 1);
                    rightPager.setCurrentItem(rightPager.getCurrentItem() - 1);
                }
            }
        });
        MediaPlayer.create(this, R.raw.page_flip).start();
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
        blinks.addFeatureObserver(this);
        switch (settings.getAlgorithm()) {
            default:
            case 0:
                signals = new BandpassSignalProcessor(
                        settings.getNumSteps(), settings.getGraphHeight());
                blinks.addFeatureObserver((Feature.FeatureObserver) signals);
                break;
            case 1:
                signals = new CurveFitSignalProcessor(
                        settings.getNumSteps(), settings.getGraphHeight());
                blinks.addFeatureObserver((Feature.FeatureObserver) signals);
                break;
            case 2:
                signals = new GestureRecognizer(this);
                break;
            case 3:
                signals = new MedianDiffDiffEOGProcessor(settings.getNumSteps());
                break;
            case 4:
                signals = new HybridEogProcessor(settings.getNumSteps());
                break;
        }
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
        updateStatusUI(View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
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
