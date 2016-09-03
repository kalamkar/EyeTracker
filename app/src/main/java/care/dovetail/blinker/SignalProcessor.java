package care.dovetail.blinker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;
import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignFisher;
import care.dovetail.blinker.SignalProcessor.Feature.Type;

public class SignalProcessor {
	private static final String TAG = "SignalProcessor";

	private static final int WINDOW_SIZE = 2000 / Config.SAMPLE_INTERVAL_MS; // 2secs max 3 blink time
	private static final int MIN_BLINK_HEIGHT = 5;

	private final List<Feature> features = new ArrayList<Feature>();

	private int updateCount = 0;
	private final int values[] = new int[Config.GRAPH_LENGTH];
	private final int displayValues[] = new int[Config.GRAPH_LENGTH];
	private final int filtered[] = new int[Config.GRAPH_LENGTH];

	public int medianAmplitude;

	private final IirFilter eogFilter;

	private enum Direction {
		NEXT,
		PREVIOUS
	}

	public static class Feature {
		public enum Type {
			TRIPLE_BLINK,
			BROW_FLICK,
			SLOPE,
			PEAK,
			VALLEY
		}

		public final Type type;
		public final int start;
		public final int end;
		public final int[] data;

		public final int index;

		public int min;
		public int max;
		public int height;

		public Feature(Type type, int start, int end, int[] data) {
			this.type = type;
			this.start = start;
			this.end = end;
			this.data = data;

			index = start + (data.length / 2);
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof Feature)) {
				return false;
			}
			Feature otherPt = (Feature) other;
			return type == otherPt.type && start == otherPt.start && end == otherPt.end;
		}
	}

	public SignalProcessor() {
		// Frequency values relative to sampling rate.
		// 30bpm = 0.0025 = 0.5Hz / 200Hz
		// 60bpm = 0.005 = 1Hz / 200Hz
		// 840bpm = 0.07 = 14Hz / 200Hz

		// highpass filter
		eogFilter = new IirFilter(IirFilterDesignFisher.design(
				FilterPassType.bandpass, FilterCharacteristicsType.bessel, 4, 0, 0.005, 0.015));
	}

	public synchronized void update(int[] chunk) {
		updateCount += chunk.length;

		System.arraycopy(values, chunk.length, values, 0, values.length - chunk.length);
		System.arraycopy(chunk, 0, values, values.length - chunk.length, chunk.length);

		System.arraycopy(filtered, chunk.length, filtered, 0, filtered.length - chunk.length);
		int start = filtered.length - chunk.length;
		for (int i = 0; i < chunk.length; i++) {
			filtered[start + i] = (int) eogFilter.step(chunk[i])
					+ (chunk[i] != 0 ? Config.FILTER_AMPLITUDE_ADJUST : 0);
		}

		if (updateCount >= Config.FEATURE_DETECT_SAMPLES) {
			updateCount = 0;
			processFeatures();
		}
	}

	public synchronized List<Feature> getFeatures(Type type) {
		List<Feature> subset = new ArrayList<Feature>();
		for (Feature fp : features) {
			if (type != null && type.equals(fp.type)) {
				subset.add(fp);
			}
		}
		return subset;
	}

	public synchronized int[] getValues() {
		return displayValues;
	}

	public synchronized int[] getFilteredValues() {
		updateFiltered();
		return filtered;
	}

	private void updateFiltered() {
		// Pass through a bandpass filter of 30bpm to 840bpm
		for (int i = 0; i < values.length; i++) {
			filtered[i] = (int) eogFilter.step(values[i])
					+ (values[i] != 0 ? Config.FILTER_AMPLITUDE_ADJUST : 0);
		}
	}

	private synchronized void processFeatures() {
		features.clear();
		findFeatures();
		updateStats();
		System.arraycopy(values, 0, displayValues, 0, displayValues.length);
	}

	private void findFeatures() {
		int i = 0;
		while (i < filtered.length) {
			Feature slope1 = findNextSlope(i);
			if (slope1 == null) {
				break;
			}
			features.add(slope1);
//			Feature slope2 = findNextSlope(slope1.end);
//			if (slope2 == null) {
//				break;
//			}
//			features.add(slope2);
//			Feature slope3 = findNextSlope(slope2.end);
//			if (slope3 == null) {
//				break;
//			}
//			features.add(slope3);
//			if (slope3.index - slope1.index < WINDOW_SIZE) {
//				Feature feature = new Feature(Feature.Type.TRIPLE_BLINK, slope1.start, slope3.end,
//						Arrays.copyOfRange(filtered, slope1.start, slope3.end));
//				features.add(feature);
//			}
			i = slope1.end;
		}
	}

	private void updateStats() {
		int copyOfValues[] = filtered.clone();
		Arrays.sort(copyOfValues);
		medianAmplitude = copyOfValues[copyOfValues.length / 2];
	}

	private Feature findNextSlope(int index) {
		for (int i = index; i < filtered.length - 1; i++) {
			int peakIndex = find(i, Direction.NEXT, Feature.Type.PEAK);
			if (peakIndex < 0) {
				return null;
			}
			int valleyIndex = find(peakIndex, Direction.NEXT, Feature.Type.VALLEY);
			if (valleyIndex < 0) {
				return null;
			}
			// TODO(abhi): Also compare indices if they are far too apart
			if (filtered[peakIndex] - filtered[valleyIndex] > MIN_BLINK_HEIGHT) {
				Feature feature = new Feature(Feature.Type.SLOPE, peakIndex, valleyIndex,
						Arrays.copyOfRange(filtered, peakIndex, valleyIndex));
				feature.min = filtered[valleyIndex];
				feature.max = filtered[peakIndex];
				feature.height = filtered[peakIndex] - filtered[valleyIndex];
				return feature;
			}
		}
		return null;
	}

	private int find(int index, Direction direction, Feature.Type feature) {
		int i = index;
		do {
			i = direction == Direction.NEXT ?  i + 1 : i - 1;
			boolean result = feature == Feature.Type.PEAK ? isPeak(i) : isValley(i);
			if (result) {
				return i;
			}
		} while (i < filtered.length - 1 && i > 0);
		Log.w(TAG, String.format("No %s found from %d to %d", feature, index, i));
		return -1;
	}

	private boolean isPeak(int index) {
		if (index - 1 < 0 || index + 1 >= filtered.length) {
			return false;
		}
		if ((filtered[index - 1] == filtered[index]) && (filtered[index] == filtered[index + 1])) {
			return false;
		}
		return (filtered[index - 1] < filtered[index]) && (filtered[index] > filtered[index + 1]);
	}

	private boolean isValley(int index) {
		if (index - 1 < 0 || index + 1 >= filtered.length) {
			return false;
		}
		if ((filtered[index - 1] == filtered[index]) && (filtered[index] == filtered[index + 1])) {
			return false;
		}
		return (filtered[index - 1] > filtered[index]) && (filtered[index] < filtered[index + 1]);
	}
}
