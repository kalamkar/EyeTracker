package care.dovetail.blinker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.blinker.ShimmerClient.BluetoothDeviceListener;

public class MainActivity extends Activity implements BluetoothDeviceListener {
    private static final String TAG = "MainActivity";

    private ShimmerClient patchClient;
    private final SignalProcessor signals1 = new SignalProcessor();
    private final SignalProcessor signals2 = new SignalProcessor();

    private Timer chartUpdateTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO(abhi): Create patchClient in onActivityResult if BT enable activity started.
        patchClient = new ShimmerClient(this, this);
        patchClient.startScan();
    }

    @Override
    protected void onStop() {
        if (patchClient != null) {
            patchClient.stopScan();
            patchClient.close();
            patchClient = null;
        }
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        super.onStop();
    }

    @Override
    public void onScanStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.status)).setText(R.string.connecting);
            }
        });
    }

    @Override
    public void onScanResult(String deviceAddress) {
        if (patchClient != null) {
            patchClient.stopScan();
            patchClient.connect(deviceAddress);
        }
    }

    @Override
    public void onScanEnd() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onConnect(String address) {
        Log.i(TAG, String.format("Connected to %s", address));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.isDestroyed()) {
                    return;
                }
                ((TextView) findViewById(R.id.status)).setText(R.string.connected);
            }
        });

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(chartUpdater);
            }
        }, 0, Config.GRAPH_UPDATE_MILLIS);
    }

    @Override
    public void onDisconnect(String address) {
        Log.i(TAG, String.format("Disconnected from %s", address));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!MainActivity.this.isDestroyed()) {
                    ((TextView) findViewById(R.id.status)).setText(R.string.disconnected);
                }
            }
        });
        if (chartUpdateTimer != null) {
            chartUpdateTimer.cancel();
        }
        if (patchClient != null) {
            patchClient.startScan();
        }
    }

    private final Runnable chartUpdater = new Runnable() {
        @Override
        public void run() {
            if (MainActivity.this.isDestroyed()
                    || ((ToggleButton) findViewById(R.id.pause)).isChecked()) {
                return;
            }

            boolean filter = ((ToggleButton) findViewById(R.id.filter)).isChecked();
            ChartFragment chart = (ChartFragment) getFragmentManager().findFragmentById(R.id.chart);
            chart.clear();
            chart.update(signals1.getValues(), signals2.getValues(), null,
                    signals1.medianAmplitude);
        }
    };

    @Override
    public void onNewValues(int[] chunk1, int[] chunk2) {
        signals1.update(chunk1);
        signals2.update(chunk2);
    }
}
