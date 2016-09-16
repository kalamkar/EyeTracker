package care.dovetail.blinker;

import java.util.Arrays;

/**
 * Created by abhi on 9/16/16.
 */
public class Utils {

    public static int getMedian(int values[]) {
        int copyOfValues[] = values.clone();
        Arrays.sort(copyOfValues);
        return copyOfValues[copyOfValues.length / 2];
    }
}
