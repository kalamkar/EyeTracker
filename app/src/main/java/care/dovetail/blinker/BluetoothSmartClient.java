package care.dovetail.blinker;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BluetoothSmartClient extends BluetoothGattCallback {
	private static final String TAG = "BluetoothSmartClient";

	private final BluetoothDeviceListener listener;
	private final Context context;

	private final BluetoothAdapter adapter;
	private final BluetoothLeScanner scanner;

	private BluetoothGatt gatt;
	private BluetoothGattCharacteristic sensorData;

	private int state = BluetoothProfile.STATE_DISCONNECTED;

    public interface BluetoothDeviceListener {
    	public void onScanStart();
    	public void onScanResult(String deviceAddress);
    	public void onScanEnd();
    	public void onConnect(String address);
    	public void onDisconnect(String address);
    	public void onNewValues(int values[]);
    }

	public BluetoothSmartClient(Context context, BluetoothDeviceListener listener) {
		this.listener = listener;
		this.context = context;

		BluetoothManager bluetoothManager =
				(BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		adapter = bluetoothManager.getAdapter();
		scanner = adapter.getBluetoothLeScanner();
	}

	private ScanCallback callback = new ScanCallback() {
		@Override
		public void onScanFailed(int errorCode) {
			Toast.makeText(context,
					String.format("Bluetooth LE scan failed with error %d", errorCode),
					Toast.LENGTH_LONG).show();
			super.onScanFailed(errorCode);
		}

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			String name = result.getDevice().getName();
			name = name == null ? result.getScanRecord().getDeviceName() : name;
			if (name != null && name.startsWith(Config.BT_DEVICE_NAME_PREFIX)) {
				Log.i(TAG, String.format("Found device %s", name));
				listener.onScanResult(result.getDevice().getAddress());
			}
			super.onScanResult(callbackType, result);
		}
	};

	public void startScan() {
		Log.i(TAG, "Starting scan for BTLE patch.");
		ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_BALANCED).setReportDelay(0).build();
		scanner.startScan(null, settings, callback);
		listener.onScanStart();
	}

	public void stopScan() {
		Log.i(TAG, "Stopping scan for BTLE patch.");
		scanner.stopScan(callback);
		listener.onScanEnd();
	}

	public void connect(String address) {
		if (address == null || address.isEmpty()) {
			Log.e(TAG, "No BluetoothLE device given to connect.");
			return;
		}
		if (adapter == null || !adapter.isEnabled()) {
			Log.e(TAG, "Bluetooth adapther is null or disabled.");
			return;
		}
		adapter.getRemoteDevice(address).connectGatt(context, false /* auto connect */, this);
	}

	@Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        this.gatt = gatt;
        this.state = newState;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
        	gatt.discoverServices();
        	listener.onConnect(gatt.getDevice().getAddress());
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        	listener.onDisconnect(gatt.getDevice().getAddress());
        	close();
        } else {
        	Log.e(TAG, String.format("GATT server %s changed to unknown new state %d and status %d",
        			gatt.getDevice().getAddress(), newState, status));
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
        	Log.e(TAG, "onServicesDiscovered received error code: " + status);
        	return;
        }
    	for (BluetoothGattService service : gatt.getServices()) {
    		for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
    			long uuid = characteristic.getUuid().getMostSignificantBits() >> 32;
    			if (uuid == Config.DATA_UUID) {
    				sensorData = characteristic;
    			}
    		}
    	}

    	if (sensorData != null) {
        	Log.d(TAG, String.format("Found data UUID %s",
        			Long.toHexString(sensorData.getUuid().getMostSignificantBits())));
        	enableNotifications();
    	} else {
    		Log.e(TAG, "Could not find Sensor Data characteristic.");
    	}
    }

	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {
        // gatt.readCharacteristic(sensorData);
		readCharacteristic(characteristic);
		super.onCharacteristicChanged(gatt, characteristic);
	}

	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
			int status) {
		readCharacteristic(characteristic);
		super.onCharacteristicRead(gatt, characteristic, status);
	}

	private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		byte[] values = characteristic.getValue();
		int intValues[] = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			intValues[i] = values[i] & 0xFF;
		}

		listener.onNewValues(intValues);
	}

	public boolean isConnected() {
		return state == BluetoothProfile.STATE_CONNECTED;
	}

	public String getDevice() {
		return gatt == null ? null : gatt.getDevice().getAddress();
	}

	public void close() {
		if (gatt == null) {
	        return;
	    }
//		if (isConnected()) {
//			gatt.disconnect();
//		}
		gatt.close();
		gatt = null;
	}

	private boolean enableNotifications() {
		if (sensorData == null) {
			return false;
		}
		boolean success = true;
		success = success && gatt.setCharacteristicNotification(sensorData, true);
		for (BluetoothGattDescriptor descriptor : sensorData.getDescriptors()) {
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			success = success && gatt.writeDescriptor(descriptor);
		}
		return success;
	}

	@SuppressWarnings("unused")
	private boolean disableNotifications() {
		if (sensorData == null) {
			return false;
		}
		boolean success = true;
		success = success && gatt.setCharacteristicNotification(sensorData, false);
		for (BluetoothGattDescriptor descriptor : sensorData.getDescriptors()) {
			descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
			success = success && gatt.writeDescriptor(descriptor);
		}
		return success;
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
}
