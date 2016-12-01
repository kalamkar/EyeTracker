package care.dovetail.blinker;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import care.dovetail.blinker.ShimmerClient.BluetoothDeviceListener;

public class MainActivity extends Activity implements BluetoothDeviceListener,
        SignalProcessor.FeatureObserver, AccelerationProcessor.ShakingObserver {
    private static final String TAG = "MainActivity";

    private ShimmerClient patchClient;
    private SignalProcessor signals = new SignalProcessor(this, true);
    private AccelerationProcessor accelerometer;

    private FileDataWriter writer = null;

    private Timer chartUpdateTimer = null;
    private Timer sectorUpdateTimer = null;

    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        findViewById(R.id.binocular).setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        ((ToggleButton) findViewById(R.id.filter)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        signals = new SignalProcessor(MainActivity.this, isChecked);
                    }
                });

        accelerometer = new AccelerationProcessor(
                (SensorManager) getSystemService(Context.SENSOR_SERVICE), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO(abhi): Create patchClient in onActivityResult if BT enable activity started.
        patchClient = new ShimmerClient(this, this);
        patchClient.startScan();

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);

        chartUpdateTimer = new Timer();
        chartUpdateTimer.schedule(chartUpdater, 0, Config.GRAPH_UPDATE_MILLIS);

        sectorUpdateTimer = new Timer();
        sectorUpdateTimer.schedule(sectorUpdater, 0, Config.GRAPH_UPDATE_MILLIS);

        accelerometer.start();
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
        accelerometer.stop();
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
        writer = new FileDataWriter(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.this.isDestroyed()) {
                    return;
                }
                ((TextView) findViewById(R.id.status)).setText(R.string.connected);
            }
        });

    }

    @Override
    public void onDisconnect(String address) {
        Log.i(TAG, String.format("Disconnected from %s", address));
        writer.close();
        writer = null;
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
            boolean filter = ((ToggleButton) findViewById(R.id.filter)).isChecked();
            final ChartFragment leftChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.leftChart);
            final ChartFragment rightChart =
                    (ChartFragment) getFragmentManager().findFragmentById(R.id.rightChart);
            leftChart.clear();
            rightChart.clear();
            if (filter) {
                leftChart.updateChannel1(signals.positions1(), signals.range1());
                leftChart.updateChannel2(signals.positions2(), signals.range2());
                rightChart.updateChannel1(signals.positions1(), signals.range1());
                rightChart.updateChannel2(signals.positions2(), signals.range2());
            } else {
                leftChart.updateChannel1(signals.channel1(), signals.range1());
                leftChart.updateChannel2(signals.channel2(), signals.range2());
                rightChart.updateChannel1(signals.channel1(), signals.range1());
                rightChart.updateChannel2(signals.channel2(), signals.range2());
            }
//            leftChart.updateChannel3(accelerometer.getY(), Pair.create(-100, 100));
//            rightChart.updateChannel3(accelerometer.getY(), Pair.create(-100, 100));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (leftChart.isResumed() && rightChart.isResumed()) {
                        leftChart.updateUI();
                        rightChart.updateUI();
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
                    Pair<Integer, Integer> sector = signals.getSector();
                    GridView leftGrid = (GridView) findViewById(R.id.leftGrid);
                    leftGrid.highlight(sector.first, sector.second);
                    GridView rightGrid = (GridView) findViewById(R.id.rightGrid);
                    rightGrid.highlight(sector.first, sector.second);
                }
            });
        }
    };

    @Override
    public void onFeature(Feature feature) {
        ringtone.play();
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        if (!accelerometer.isShaking()) {
            signals.update(channel1, channel2);
        }
        if (writer != null) {
            writer.write(channel1, channel2);
        }
    }

    @Override
    public void onShakingChange(final boolean isShaking) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.shaking).setVisibility(isShaking ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }
}
