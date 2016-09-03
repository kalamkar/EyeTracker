package care.dovetail.blinker;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import care.dovetail.blinker.ShimmerClient.BluetoothDeviceListener;
import care.dovetail.blinker.SignalProcessor.Feature;

public class MainActivity extends Activity implements BluetoothDeviceListener {
	private static final String TAG = "MainActivity";

	private ShimmerClient patchClient;
	private final SignalProcessor signals = new SignalProcessor();

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
		patchClient.stopScan();
		patchClient.connect(deviceAddress);
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
			chart.update(filter ? signals.getFilteredValues() : signals.getValues(),
					signals.getFeatures(Feature.Type.SLOPE), signals.medianAmplitude);
		}
	};

	@Override
	public void onNewValues(int[] chunk) {
		signals.update(chunk);
	}
}
