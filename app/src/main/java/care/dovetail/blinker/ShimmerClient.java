package care.dovetail.blinker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class ShimmerClient {
	private static final String TAG = "ShimmerClient";

	private final BluetoothDeviceListener listener;

	private final BluetoothAdapter adapter;
	private ShimmerConnection connection;

    public interface BluetoothDeviceListener {
    	public void onScanStart();
    	public void onScanResult(String deviceAddress);
    	public void onScanEnd();
    	public void onConnect(String address);
    	public void onDisconnect(String address);
    	public void onNewValues(int values[]);
    }

	public ShimmerClient(Context context, BluetoothDeviceListener listener) {
		this.listener = listener;

		BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		adapter = bluetoothManager.getAdapter();
	}

	public void startScan() {
		Log.i(TAG, "Starting scan for Shimmer device.");
		listener.onScanStart();
		Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
		for (BluetoothDevice device : pairedDevices) {
			String name = device.getName();
			if (name != null && name.startsWith(Config.BT_DEVICE_NAME_PREFIX)) {
	            Log.i(TAG, String.format("Found device %s", name));
				listener.onScanResult(device.getAddress());
			}
		}

	}

	public void stopScan() {
		Log.i(TAG, "Stopping scan for Shimmer device.");
		listener.onScanEnd();
	}

	public void connect(String address) {
		if (address == null || address.isEmpty()) {
			Log.e(TAG, "No Shimmer device given to connect.");
			return;
		}
		if (adapter == null || !adapter.isEnabled()) {
			Log.e(TAG, "Bluetooth adapter is null or disabled.");
			return;
		}
		final BluetoothDevice device = adapter.getRemoteDevice(address);

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
		            BluetoothSocket socket =
		            		device.createInsecureRfcommSocketToServiceRecord(Config.SHIMMER_UUID);
		            if (socket == null) {
						Log.e(TAG, String.format("Could not connect to %s.", device.getName()));
						return null;
					}
		            socket.connect();
		            connection = new ShimmerConnection(socket);
		        } catch (IOException e) {
		        	Log.e(TAG, String.format("Could not connect to %s.", device.getName()), e);
		        }
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (connection == null) {
					Log.e(TAG, String.format("Could not connect to %s.", device.getName()));
					return;
				}

				listener.onConnect(device.getAddress());
				connection.startStreaming();
			}
		}.execute();
	}

	public boolean isConnected() {
		return connection != null;
	}

	public void close() {
		if (connection != null) {
			Log.i(TAG, "Closing connection to Shimmer device.");
			connection.close();
			connection = null;
		}
	}

	private void onDisconnect(BluetoothDevice device) {
		listener.onDisconnect(device.getAddress());
	}

	public static void maybeEnableBluetooth(Activity activity) {
		BluetoothManager bluetoothManager =
				(BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetooth = bluetoothManager.getAdapter();

		if (bluetooth == null || !bluetooth.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			activity.startActivityForResult(enableBtIntent, Config.BLUETOOTH_ENABLE_REQUEST);
		}
	}

	private class ShimmerConnection extends Thread {
	    private final BluetoothSocket socket;
	    private InputStream inStream;
	    private OutputStream outStream;

	    public ShimmerConnection(BluetoothSocket socket) {
	        this.socket = socket;
	        try {
	            inStream = socket.getInputStream();
	            outStream = socket.getOutputStream();
	        } catch (IOException e) {
	        	Log.e(TAG, "Could not get I/O streams.", e);
	        }
	    }

	    @Override
		public void run() {
	        byte[] buffer = new byte[1024];  // buffer store for the stream
	        int numBytes = -1;

	        do {
	            try {
	                numBytes = inStream.read(buffer);
	                Log.v(TAG, String.format("Num bytes %d, first %d", numBytes, buffer[0]));
	                int values[] = new int[numBytes];
	                for (int i = 0; i < numBytes / 2 ; i += 2) {
	                	values[i] = buffer[i + 1] << 8 & buffer[i];
	                }
	                // listener.onNewValues(values);
	            } catch (IOException e) {
	            	Log.e(TAG, "Exception while reading BT Socket.", e);
	            	break;
	            }
	        } while (numBytes >= 0);
	        onDisconnect(socket.getRemoteDevice());
	    }

	    public void startStreaming() {
	    	if (write(new byte[] {(byte) 0x07})) {
	    		this.start();
	    	}
	    }

	    private boolean write(byte[] bytes) {
	        try {
	            outStream.write(bytes);
	        } catch (IOException e) {
	        	Log.e(TAG, "Could not write to output stream.", e);
	        	return false;
	        }
	        return true;
	    }

	    public void close() {
	        try {
	            socket.close();
	        } catch (IOException e) {
	        	Log.e(TAG, "Could not close socket.", e);
	        }
	    }
	}
}
