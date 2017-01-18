package care.dovetail.blinker.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;

import care.dovetail.blinker.Config;

public class ShimmerClient {
    private static final String TAG = "ShimmerClient";

    public static final UUID SHIMMER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String BT_DEVICE_NAME_PREFIX = "Shimmer3";

    private final BluetoothDeviceListener listener;

    private final BluetoothAdapter adapter;
    private ShimmerConnection connection;
    private String remoteAddress;

    public interface BluetoothDeviceListener {
        void onScanStart();

        void onScanResult(String deviceAddress);

        void onScanEnd();

        void onConnect(String address);

        void onDisconnect(String address);

        void onNewValues(int channel1, int channel2);
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
            if (name != null && name.startsWith(BT_DEVICE_NAME_PREFIX)) {
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

        remoteAddress = address;
        new ConnectionTask(address, this).execute();
    }

    public boolean isConnected() {
        return connection != null;
    }

    public String getAddress() {
        return remoteAddress;
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
        remoteAddress = null;
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

    private static class ConnectionTask extends AsyncTask<Void, Void, ShimmerConnection> {

        private final String address;
        private final WeakReference<ShimmerClient> client;

        private ConnectionTask(String address, ShimmerClient client) {
            this.address = address;
            this.client = new WeakReference<>(client);
        }

        @Override
        protected ShimmerConnection doInBackground(Void... params) {
            try {
                BluetoothDevice device = client.get().adapter.getRemoteDevice(address);
                BluetoothSocket socket =
                        device.createInsecureRfcommSocketToServiceRecord(SHIMMER_UUID);
                if (socket == null) {
                    Log.e(TAG, String.format("Could not connect to %s.", device.getName()));
                    return null;
                }
                socket.connect();
                return new ShimmerConnection(socket, client.get());
            } catch (IOException e) {
                // Log.e(TAG, String.format("Could not connect to %s.", device.getName()), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ShimmerConnection connection) {
            if (connection == null) {
                client.get().connect(address);
                return;
            }

            client.get().listener.onConnect(address);
            if (connection.sendInquiry()) {
                connection.start();
                client.get().connection = connection;
            }
        }
    }

    private static class ShimmerConnection extends Thread {
        private final BluetoothSocket socket;
        private InputStream inStream;
        private OutputStream outStream;

        private final WeakReference<ShimmerClient> client;

        public ShimmerConnection(BluetoothSocket socket, ShimmerClient client) {
            this.socket = socket;
            this.client = new WeakReference<>(client);
            try {
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Could not get I/O streams.", e);
            }
        }

        @Override
        public void run() {
            while (true) {
                int response;
                try {
                    response = inStream.read();
                } catch (IOException e) {
                    Log.e(TAG, "Exception while reading BT Socket.", e);
                    break;
                }
                if (response == 0x00) {     // Data packet
                    byte[] buffer = new byte[10];    // u16, u8, 124r, i24r
                    try {
                        for (int i = 0; i < buffer.length; i++) {
                            buffer[i] = (byte) inStream.read();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while reading Data Packet.", e);
                        break;
                    }
                    client.get().processData(buffer);
                } else if (response == 0x02) {  // Inquiry response
                    byte[] buffer = new byte[128];
                    int numBytes;
                    try {
                        numBytes = inStream.read(buffer, 0, buffer.length);
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while reading inquiry response.", e);
                        break;
                    }
                    // Sampling rate, accel range, config setup byte0, num channels, buffer size
                    // 0x80 0x02, 0x50 0x9b 0x39 0x08, 0x03, 0x01
                    // 0x1d 0x1e 0x1f   Channel IDs
                    Log.i(TAG, String.format("Num bytes %d, Packet %s",
                            numBytes, bytesToString(buffer, numBytes)));
                    if (numBytes >= 11 && buffer[6] == 3 && buffer[7] == 1 &&
                            buffer[8] == 0x1D && buffer[9] == 0x1E && buffer[10] == 0x1F) {
                        startStreaming();
                    } else {
                        Log.w(TAG, String.format("Unknown config %s",
                                bytesToString(buffer, numBytes)));
                    }
                } else if (response == 0xFF) {
                    Log.i(TAG, "Got Ack");
                } else {
                    Log.w(TAG, String.format("Unknown response 0x%02x", response));
                }
            }
            client.get().onDisconnect(socket.getRemoteDevice());
        }

        public boolean startStreaming() {
            Log.i(TAG, "Sending Start Stream command");
            return write(new byte[]{(byte) 0x07});
        }

        public boolean sendInquiry() {
            Log.i(TAG, "Sending Inquiry command");
            return write(new byte[]{(byte) 0x01});
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

    private void processData(byte buffer[]) {
        if (buffer == null || buffer.length != 10) {
            Log.w(TAG, String.format("Invalid buffer for data packet."));
            return;
        }

        int timestamp = parseU24(buffer[0], buffer[1], buffer[2]);  // Timestamp 3 bytes        u24
        int status = buffer[3];                                     // ExG_ADS1292R_1_STATUS    u8
        int channel1 = parseI24R(buffer[4], buffer[5], buffer[6]);  // ExG_ADS1292R_1_CH1_24BIT i24r
        int channel2 = parseI24R(buffer[7], buffer[8], buffer[9]);  // ExG_ADS1292R_1_CH2_24BIT i24r

        channel1 += Math.pow(2, 24) / 2;
        channel2 += Math.pow(2, 24) / 2;

//        Log.v(TAG, String.format(
//                "Timestamp %d, Status 0x%2x, channel1 %d, channel2 %d",
//                timestamp, status, channel1, channel2));

        listener.onNewValues(channel1, channel2);
    }

    private static int parseU24(byte byte1, byte byte2, byte byte3) {
        int xmsb = (byte3 & 0xFF) << 16;
        int msb = (byte2 & 0xFF) << 8;
        int lsb = (byte1 & 0xFF);
        return xmsb + msb + lsb;
    }

    private static int parseU16(byte byte1, byte byte2) {
        return (byte1 & 0xFF) + ((byte2 & 0xFF) << 8);
    }

    private static int parseI24R(byte byte1, byte byte2, byte byte3) {
        int xmsb = (byte1 & 0xFF) << 16;
        int msb = (byte2 & 0xFF) << 8;
        int lsb = (byte3 & 0xFF);
        return getTwosComplement(xmsb + msb + lsb, 24);
    }

    private static int getTwosComplement(int signedData, int bitLength) {
        int newData = signedData;
        if (signedData >= (1 << (bitLength - 1))) {
            newData = -((signedData ^ (int) (Math.pow(2, bitLength) - 1)) + 1);
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
