package care.dovetail.tracker.processing;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterDesignExstrom;
import care.dovetail.tracker.Config;
import care.dovetail.tracker.Stats;

/**
 * Created by abhi on 3/22/17.
 */

public class BandpassBlinkDetector implements BlinkDetector {

    private static final String TAG = "BandpassBlinkDetector";

    private static final int SMALL_BLINK_HEIGHT = 5000;
    private static final int MIN_BLINK_HEIGHT = 10000;
    private static final int MAX_BLINK_HEIGHT = 30000;

    private static final int QUALITY_WINDOW = 200; // 4 seconds

    private static final int MIN_STD_DEV = 5000;

    private long updateCount = 0;
    private int blinkWindowIndex = 0;
    private Stats blinkStats = new Stats(null);
    private final int blinks[] = new int[Config.GRAPH_LENGTH];

    private final IirFilter blinkFilter = new IirFilter(IirFilterDesignExstrom.design(
            FilterPassType.bandpass, 1, 1.024 / Config.SAMPLING_FREQ, 2.56 / Config.SAMPLING_FREQ));

    private final List<Feature.FeatureObserver> observers = new ArrayList<>();

    @Override
    public void update(int value) {
        updateCount++;
        System.arraycopy(blinks, 1, blinks, 0, blinks.length - 1);
        blinks[blinks.length - 1] = (int) blinkFilter.step(value);
        blinkStats = new Stats(blinks, blinks.length - QUALITY_WINDOW, QUALITY_WINDOW);

        if (updateCount >= blinks.length && blinkStats.stdDev == 0) {
            notifyFeature(new Feature(Feature.Type.BAD_CONTACT, 0, 0, new int[0]));
        } else if (updateCount % QUALITY_WINDOW == 0 && blinkStats.stdDev > MIN_STD_DEV) {
            // Every 4 seconds check signal quality and send notification if signal is bad.
            notifyFeature(new Feature(Feature.Type.BAD_SIGNAL, 0, 0, new int[0]));
        } else if (updateCount % BLINK_WINDOW == 0 && blinkStats.stdDev < MIN_STD_DEV) {
            notifyFeature(maybeGetBlink(blinks, SMALL_BLINK_HEIGHT, MIN_BLINK_HEIGHT,
                    MAX_BLINK_HEIGHT));
        }
    }

    @Override
    public int getQuality() {
        return blinkStats.stdDev;
    }

    @Override
    public final int[] blinks() {
        return blinks;
    }

    @Override
    public Pair<Integer, Integer> blinkRange() {
        return Pair.create(blinkStats.median - MAX_BLINK_HEIGHT,
                blinkStats.median + MAX_BLINK_HEIGHT);
    }

    @Override
    public void addFeatureObserver(Feature.FeatureObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    private void notifyFeature(Feature feature) {
        if (feature == null) {
            return;
        }
        for (Feature.FeatureObserver observer : observers) {
            observer.onFeature(feature);
        }
    }

    private static Feature maybeGetBlink(int values[], int smallBlinkHeight, int minBlinkHeight,
                                         int maxBlinkHeight) {
        int last = values.length - 1;
        int first = Math.max(0, last - (Config.BLINK_WINDOW * 2) + 1);

        int maxIndex = first;
        int minIndex = first;
        for (int i = first; i <= last; i++) {
            if (values[maxIndex] < values[i]) {
                maxIndex = i;
            }
            if (values[minIndex] > values[i]) {
                minIndex = i;
            }
        }

        if (maxIndex == last || minIndex == last || maxIndex == 0 || minIndex == 0) {
            // Ignore edges for blink to detect strict local minima and maxima.
            return null;
        }

        boolean localMaxima = (values[maxIndex - 1] < values[maxIndex])
                && (values[maxIndex] > values[maxIndex + 1]);
        boolean localMinima = (values[minIndex - 1] > values[minIndex])
                && (values[minIndex] < values[minIndex + 1]);

        int height = values[maxIndex] - values[minIndex];
        if (localMaxima && localMinima && maxIndex < minIndex) {
            if (height > smallBlinkHeight && height < minBlinkHeight) {
                return new Feature(Feature.Type.SMALL_BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            } else if (height > minBlinkHeight && height < maxBlinkHeight) {
                return new Feature(Feature.Type.BLINK, Math.min(minIndex, maxIndex),
                        Math.max(minIndex, maxIndex),
                        new int[]{values[maxIndex], values[minIndex]});
            }
        }
        return null;
    }
}
