package care.dovetail.blinker;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.blinker.ShimmerClient.BluetoothDeviceListener;

public class MainActivity extends Activity implements BluetoothDeviceListener,
        SignalProcessor.FeatureObserver {
    private static final String TAG = "MainActivity";

    private static final int SECTOR_IDS[] = new int[] {R.id.num1, R.id.num2, R.id.num3, R.id.num4,
            R.id.num5, R.id.num6, R.id.num7, R.id.num8, R.id.num9};

    private ShimmerClient patchClient;
    private final SignalProcessor signals = new SignalProcessor(this);

    private Timer chartUpdateTimer = null;
    private Timer sectorUpdateTimer = null;

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO(abhi): Create patchClient in onActivityResult if BT enable activity started.
        patchClient = new ShimmerClient(this, this);
        patchClient.startScan();

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
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
        if (sectorUpdateTimer != null) {
            sectorUpdateTimer.cancel();
        }
        ringtone.stop();
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
        chartUpdateTimer.schedule(chartUpdater, 0, Config.GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(sectorUpdater, 0, 100);
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

    private final TimerTask chartUpdater = new TimerTask() {
        @Override
        public void run() {
            if (MainActivity.this.isDestroyed()
                    || ((ToggleButton) findViewById(R.id.pause)).isChecked()) {
                return;
            }

            boolean filter = ((ToggleButton) findViewById(R.id.filter)).isChecked();
            final ChartFragment chart = (ChartFragment) getFragmentManager().findFragmentById(R.id.chart);
            chart.clear();
            if (filter) {
                chart.updateChannel1(signals.positions1(), signals.range1());
                chart.updateChannel2(signals.positions2(), signals.range2());
            } else {
                chart.updateChannel1(signals.channel1(), signals.range1());
                chart.updateChannel2(signals.channel2(), signals.range2());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (chart.isResumed()) {
                        chart.updateUI();
                    }
                }
            });
        }
    };

    private final TimerTask sectorUpdater = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int highlightSector = signals.getSector();
                    for (int i = 0; i < SECTOR_IDS.length; i++) {
                        if (i == highlightSector) {
                            findViewById(SECTOR_IDS[i]).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(SECTOR_IDS[i]).setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        }
    };

    @Override
    public void onFeature(Feature feature) {
        Log.i(TAG, "Found blink");
        ringtone.play();
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        signals.update(channel1, channel2);
    }
}
