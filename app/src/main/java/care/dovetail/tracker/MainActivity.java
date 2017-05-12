package care.dovetail.tracker;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import care.dovetail.tracker.bluetooth.ShimmerClient;
import care.dovetail.tracker.bluetooth.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.tracker.eog.CombinedEogProcessor;
import care.dovetail.tracker.eog.EOGProcessor;
import care.dovetail.tracker.eog.GestureEogProcessor;
import care.dovetail.tracker.ui.DebugBinocularFragment;
import care.dovetail.tracker.ui.DebugFragment;
import care.dovetail.tracker.ui.DebugUi;
import care.dovetail.tracker.ui.FruitFragment;
import care.dovetail.tracker.ui.GestureFragment;
import care.dovetail.tracker.ui.PositionFragment;
import care.dovetail.tracker.ui.SettingsActivity;
import care.dovetail.tracker.ui.SpectaclesFragment;

public class MainActivity extends FragmentActivity implements BluetoothDeviceListener,
        AccelerationProcessor.ShakingObserver, EyeEvent.Observer {
    private static final String TAG = "MainActivity";

    private final Settings settings = new Settings(this);

    private final ShimmerClient patchClient = new ShimmerClient(this, this);
    private EOGProcessor eog;
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Fragment demo;
    private DebugUi debug;

    private EyeEvent latestPosition;

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
        startBluetooth();
        accelerometer.start();
    }

    @Override
    protected void onStop() {
        stopBluetooth();
        accelerometer.stop();
        super.onStop();
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

    @Override
    public EyeEvent.Criteria getCriteria() {
        return new EyeEvent.AnyCriteria()
                .add(EyeEvent.Criterion.position(settings.getNumSteps(), settings.getNumSteps()))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SIGNAL_QUALITY))
                .add(EyeEvent.Criterion.badContact(5000));
    }

    @Override
    public void onEyeEvent(EyeEvent event) {
        switch (event.type) {
            case BAD_CONTACT:
                if (patchClient.isConnected()) {
                    stopBluetooth();
                    startBluetooth();
                }
                break;
            case POSITION:
                latestPosition = event;
                if (demo instanceof EyeEvent.Observer && eog.isGoodSignal()
                        && ((EyeEvent.Observer) demo).getCriteria().isMatching(event)) {
                    ((EyeEvent.Observer) demo).onEyeEvent(event);
                }
                break;
            case SIGNAL_QUALITY:
                if (eog.isGoodSignal()) {
                    showDebugNumbers();
                } else {
                    showQualityProgress();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debug.setProgress(eog.getSignalQuality());
                        debug.showWarning(
                                (!eog.isStableHorizontal()) || (!eog.isStableVertical()));
                    }
                });
        }

    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        eog.update(channel1, channel2);
        if (writer != null) {
            int column = latestPosition != null ? latestPosition.column : -1;
            int row = latestPosition != null ? latestPosition.row : -1;
            int filtered1 = eog.horizontal()[Config.GRAPH_LENGTH-1];
            int filtered2 = eog.vertical()[Config.GRAPH_LENGTH-1];
            int moleCol = demo instanceof PositionFragment
                    ? ((PositionFragment) demo).getMoleColumn() : -1;
            int moleRow = demo instanceof PositionFragment
                    ? ((PositionFragment) demo).getMoleRow() : -1;
            writer.write(channel1, channel2, filtered1, filtered2, column, row, moleCol, moleRow);
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
        if (settings.getDemo() == 0) { // Gestures
            demo = new GestureFragment();
            eog = new GestureEogProcessor();
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 1) { // Fruit
            demo = new FruitFragment();
            eog = new GestureEogProcessor();
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 2) { // Position
            demo = new PositionFragment();
//            eog = new PositionEogProcessor(settings.getNumSteps(), settings.getThreshold());
            eog = new CombinedEogProcessor(settings.getNumSteps());
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 3) { // Spectacles
            demo = new SpectaclesFragment();
            eog = new GestureEogProcessor();
            debug = new DebugFragment();
        }

        if (demo instanceof EyeEvent.Observer) {
            eog.addObserver((EyeEvent.Observer) demo);
        } else if (demo instanceof Gesture.Observer) {
            for (Gesture gesture : ((Gesture.Observer) demo).getGestures()) {
                eog.addObserver(gesture);
            }
        }
        eog.addObserver(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.demo, demo).commit();

        debug.setDataSource(eog);
        getSupportFragmentManager()
                .beginTransaction().replace(R.id.debug, (Fragment) debug).commit();

        patchClient.connect();
        lookupStartTimeMillis = System.currentTimeMillis();
        showBluetoothSpinner();
    }

    public void stopBluetooth() {
        patchClient.close();
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
         debug.updateStatusUI(View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
    }

    private void showDebugNumbers() {
        debug.updateStatusUI(View.INVISIBLE, View.INVISIBLE,
                settings.shouldShowNumbers() ? View.VISIBLE : View.INVISIBLE);
    }

    private void showBluetoothSpinner() {
        debug.updateStatusUI(View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
    }

    private void hideAll() {
        debug.updateStatusUI(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
    }
}
