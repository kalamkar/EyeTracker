package care.dovetail.blinker;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

public class Config {

	public static final UUID SHIMMER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public static final long DATA_UUID = 0x404846A1;
	public static final String BT_DEVICE_NAME_PREFIX = "Shimmer3";
	public static final int SAMPLE_INTERVAL_MS = 5;
	public static final int SAMPLES_PER_BROADCAST = 20; // Hardcoded in FW

	public static final int GRAPH_LENGTH = 1000;			// 5 seconds at 200Hz

	public static final int SHORT_GRAPH_MIN = 100; 	//   0 for V3,  64 for V2,  64 for V1
	public static final int SHORT_GRAPH_MAX = 160; 	// 255 for V3, 192 for V2, 255 for V1

	public static final int GRAPH_UPDATE_MILLIS = 500;

	public static final int FEATURE_DETECT_SAMPLES = 5 * SAMPLES_PER_BROADCAST;

	public static final int BPM_UPDATE_MILLIS = 3000;
	public static final int MIN_BPM_SAMPLES = 5;
	public static final int MAX_BPM_SAMPLES = 10;

	public static final int FILTER_AMPLITUDE_ADJUST = 125;

	public static final SimpleDateFormat EVENT_TIME_FORMAT =
			new SimpleDateFormat("hh:mm:ssaa, MMM dd yyyy", Locale.US);

	public static final int BLUETOOTH_ENABLE_REQUEST = 0;
}
