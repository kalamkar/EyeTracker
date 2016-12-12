package care.dovetail.blinker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by abhi on 11/16/16.
 */

public class AccelerationProcessor implements SensorEventListener {

    private static final int SAMPLING_PERIOD_MICROS = (1000 / 200) * 1000;

    public interface ShakingObserver {
        void onShakingChange(boolean isShaking);
    }

    private final ShakingObserver observer;

    private final SensorManager sensorManager;
    private final Sensor acceleration;
    private final int accelX[] = new int[Config.GRAPH_LENGTH];
    private final int accelY[] = new int[Config.GRAPH_LENGTH];
    private final int accelZ[] = new int[Config.GRAPH_LENGTH];
    private boolean isShaking = false;

    public AccelerationProcessor(SensorManager sensorManager, ShakingObserver observer) {
        this.observer = observer;
        this.sensorManager = sensorManager;
        acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void start() {
        sensorManager.registerListener(this, acceleration, SAMPLING_PERIOD_MICROS);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public boolean isShaking() {
        return isShaking;
    }

    public int[] getX() {
        return accelX;
    }

    public int[] getY() {
        return accelY;
    }

    public int[] getZ() {
        return accelZ;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        System.arraycopy(accelX, 1, accelX, 0, accelX.length - 1);
        accelX[accelX.length -1] = (int) (event.values[0] * 100);

        System.arraycopy(accelY, 1, accelY, 0, accelY.length - 1);
        accelY[accelY.length -1] = (int) (event.values[1] * 100);

        System.arraycopy(accelZ, 1, accelZ, 0, accelZ.length - 1);
        accelZ[accelZ.length -1] = (int) (event.values[2] * 100);

        boolean isShaking = calculateShaking(accelX) > Config.SHAKING_THRESHOLD
                || calculateShaking(accelY) > Config.SHAKING_THRESHOLD
                || calculateShaking(accelZ) > Config.SHAKING_THRESHOLD;
        if (this.isShaking != isShaking) {
            observer.onShakingChange(isShaking);
        }
    }

    private static int calculateShaking(int accel[]) {
        int prevValue = accel[accel.length/4*3];
        int changes = 0;
        for (int i=accel.length/4*3; i < accel.length -1; i++) {
            changes += Math.abs(accel[i] - prevValue);
            prevValue = accel[i];
        }
        return changes;
    }
}
