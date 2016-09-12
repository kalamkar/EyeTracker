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
    	void onScanStart();
    	void onScanResult(String deviceAddress);
    	void onScanEnd();
    	void onConnect(String address);
    	void onDisconnect(String address);
    	void onNewValues(int values[]);
    }

    private static class DataPacket {
        private final long timestamp;
        private final byte status;
        private final long ch1;
        private final long ch2;

        DataPacket(long timestamp, byte status, long ch1, long ch2) {
            this.timestamp = timestamp;
            this.status = status;
            this.ch1 = ch1;
            this.ch2 = ch2;
        }
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
				if (connection.sendInquiry()) {
                    connection.start();
                }
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
                    switch(buffer[0] & 0xFF) {
                        case 0x00:
                            // Data packet
                            DataPacket data = parseDataPacket(buffer);
                            if (data != null) {
                                Log.v(TAG, String.format("Timestamp %d, Status 0x%02x, ch1 %d, ch2 %d",
                                        data.timestamp, data.status, data.ch1, data.ch2));
	                            listener.onNewValues(new int[] {(int) data.ch1});
                            } else {
                                Log.w(TAG, String.format("Unknown data %s",
                                        bytesToString(buffer, numBytes)));
                            }
                            break;
                        case 0xFF:
                            // command ack
                            Log.v(TAG, String.format("Ack %s", bytesToString(buffer, numBytes)));
                            if ((buffer[1] & 0xFF) == 0x02) {
                                // Inquiry Ack and response
                                // 0xff 0x02
                                // Sampling rate, accel range, config setup byte0, num chans, buffer size
                                // 0x80 0x02 0x50 0x9b 0x39 0x08 0x03 0x01
                                // 0x1d 0x1e 0x1f   Channel IDs
                                startStreaming();
                            }
                            break;
                        default:
                            Log.v(TAG, String.format("Num bytes %d, Packet %s",
                                    numBytes, bytesToString(buffer, numBytes)));
                            break;
                    }
	            } catch (IOException e) {
	            	Log.e(TAG, "Exception while reading BT Socket.", e);
	            	break;
	            }
	        } while (numBytes >= 0);
	        onDisconnect(socket.getRemoteDevice());
	    }

	    public boolean startStreaming() {
	    	return write(new byte[] {(byte) 0x07});
	    }

        public boolean sendInquiry() {
            return write(new byte[] {(byte) 0x01});
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

    private static DataPacket parseDataPacket(byte packet[]) {
        if (packet != null && packet.length >= 11) {
            return new DataPacket(parseU24(packet[1], packet[2], packet[3]), packet[4],
                    parseI24R(packet[5], packet[6], packet[7]),
                    parseI24R(packet[8], packet[9], packet[10]));
        }
        return null;
    }

    private static long parseU24(byte byte1, byte byte2, byte byte3) {
        long xmsb =((long)(byte3 & 0xFF) << 16);
        long msb =((long)(byte2 & 0xFF) << 8);
        long lsb =((long)(byte1 & 0xFF));
        return xmsb + msb + lsb;
    }

    private static long parseU16(byte byte1, byte byte2) {
        return (byte1 & 0xFF) + ((byte2 & 0xFF) << 8);
    }

    private static long parseI24R(byte byte1, byte byte2, byte byte3) {
        long xmsb = ((long)(byte1 & 0xFF) << 16);
        long msb = ((long)(byte2 & 0xFF) << 8);
        long lsb = ((long)(byte3 & 0xFF));
        return getTwosComplement((int)(xmsb + msb + lsb), 24);
    }

	private static long getTwosComplement(int signedData, int bitLength) {
		int newData=signedData;
		if (signedData >= (1 << (bitLength-1))) {
			newData = -((signedData ^ (int)(Math.pow(2, bitLength) - 1)) + 1);
		}
		return newData;
	}

    private static String bytesToString(byte buffer[], int numBytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numBytes; i++) {
            builder.append(String.format("0x%02x ", buffer[i]));
        }
        return builder.toString();
    }
}
