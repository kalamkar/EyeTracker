package care.dovetail.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import care.dovetail.ojo.CombinedEogProcessor;
import care.dovetail.ojo.EyeController;
import care.dovetail.ojo.EyeEvent;
import care.dovetail.ojo.Gesture;
import care.dovetail.ojo.bluetooth.EogDevice;
import care.dovetail.tracker.ui.DebugBinocularFragment;
import care.dovetail.tracker.ui.DebugFragment;
import care.dovetail.tracker.ui.DebugUi;
import care.dovetail.tracker.ui.FruitFragment;
import care.dovetail.tracker.ui.GestureFragment;
import care.dovetail.tracker.ui.PositionFragment;
import care.dovetail.tracker.ui.SettingsActivity;
import care.dovetail.tracker.ui.SpectaclesFragment;

public class MainActivity extends FragmentActivity implements EogDevice.Observer,
        EyeEvent.Observer {
    private static final String TAG = "MainActivity";

    private final Settings settings = new Settings(this);

    private final EyeController eyeController = new EyeController(this,
            new CombinedEogProcessor(settings.getNumSteps(), settings.shouldShowBandpassChart()));

    private FileDataWriter writer = null;

    private Fragment demo;
    private DebugUi debug;

    private EyeEvent latestPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        findViewById(R.id.settings).setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        eyeController.disconnect();
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        return true;
                    }
                });
        eyeController.device.add(this);
        ((EyeEvent.Source) eyeController.processor).add(this);

        if (settings.getDemo() == 0) { // Gestures
            demo = new GestureFragment();
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 1) { // Fruit
            demo = new FruitFragment();
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 2) { // Position
            demo = new PositionFragment();
            debug = new DebugBinocularFragment();
        } else if (settings.getDemo() == 3) { // Spectacles
            demo = new SpectaclesFragment();
            debug = new DebugFragment();
        }

        if (demo instanceof EyeEvent.Observer) {
            ((EyeEvent.Source) eyeController.processor).add((EyeEvent.Observer) demo);
        } else if (demo instanceof Gesture.Observer) {
            ((Gesture.Observer) demo).setEyeEventSource((EyeEvent.Source) eyeController.processor);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.demo, demo).commit();

        debug.setDataSource(eyeController.processor);
        getSupportFragmentManager()
                .beginTransaction().replace(R.id.debug, (Fragment) debug).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideBars();
        eyeController.connect();
        showBluetoothSpinner();
    }

    @Override
    protected void onStop() {
        eyeController.disconnect();
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
                .add(new EyeEvent.Criterion(EyeEvent.Type.POSITION))
                .add(new EyeEvent.Criterion(EyeEvent.Type.SIGNAL_QUALITY))
                .add(EyeEvent.Criterion.badContact(5000));
    }

    @Override
    public void onEyeEvent(EyeEvent event) {
        switch (event.type) {
            case POSITION:
                latestPosition = event;
                break;
            case SIGNAL_QUALITY:
                if (eyeController.processor.isGoodSignal()) {
                    showDebugNumbers();
                } else {
                    showQualityProgress();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        debug.setProgress(eyeController.processor.getSignalQuality());
                        debug.showWarning(
                                (!eyeController.processor.isStableHorizontal())
                                        || (!eyeController.processor.isStableVertical()));
                    }
                });
        }
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        if (writer != null) {
            int column = latestPosition != null ? latestPosition.column : -1;
            int row = latestPosition != null ? latestPosition.row : -1;
            int filtered1 = eyeController.processor.horizontal()[Config.GRAPH_LENGTH-1];
            int filtered2 = eyeController.processor.vertical()[Config.GRAPH_LENGTH-1];
            int moleCol = demo instanceof PositionFragment
                    ? ((PositionFragment) demo).getMoleColumn() : -1;
            int moleRow = demo instanceof PositionFragment
                    ? ((PositionFragment) demo).getMoleRow() : -1;
            writer.write(channel1, channel2, filtered1, filtered2, column, row, moleCol, moleRow);
        }
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
