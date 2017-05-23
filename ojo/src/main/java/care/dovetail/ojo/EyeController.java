package care.dovetail.ojo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import care.dovetail.ojo.bluetooth.EogDevice;
import care.dovetail.ojo.bluetooth.ShimmerClient;

/**
 * Created by abhi on 5/23/17.
 */

public class EyeController implements EyeEvent.Observer, EogDevice.Observer {

    public final EogDevice device;
    public final EogProcessor processor;

    private final Context context;

    private AccelerationProcessor accelerometer;
    private long lookupStartTimeMillis;

    public EyeController(Context context) {
        this.context = context;
        device = new ShimmerClient(context);
        device.add(this);
        processor = new GestureEogProcessor(); // new CombinedEogProcessor(3, true);
        ((EyeEvent.Source) processor).add(this);
    }

    public void connect() {
        device.connect();
        accelerometer = new AccelerationProcessor(
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
        accelerometer.start();
        lookupStartTimeMillis = System.currentTimeMillis();
    }

    public void disconnect() {
        device.close();
        accelerometer.stop();
    }

    @Override
    public EyeEvent.Criteria getCriteria() {
        return new EyeEvent.AnyCriteria()
                .add(new EyeEvent.Criterion(EyeEvent.Type.SIGNAL_QUALITY))
                .add(EyeEvent.Criterion.badContact(5000));
    }

    @Override
    public void onEyeEvent(EyeEvent event) {
        switch (event.type) {
            case BAD_CONTACT:
                if (device.isConnected()) {
                    disconnect();
                    connect();
                }
                break;
            case SIGNAL_QUALITY:
        }
    }

    @Override
    public void onNewValues(int channel1, int channel2) {
        processor.update(channel1, channel2);
    }

    @Override
    public void onConnect(String name) {
    }

    @Override
    public void onDisconnect(String name) {
    }

    private void onShakingChange(final boolean isShaking) {
        long millisSinceLookup = System.currentTimeMillis() - lookupStartTimeMillis;
        if (!isShaking || millisSinceLookup < 5000) {
            return;
        }
        if (device.isConnected()) {
            disconnect();
            connect();
        } else {
            connect();
        }
    }

    private class AccelerationProcessor implements SensorEventListener {

        private static final int SHAKING_THRESHOLD = 10000;

        private static final int SAMPLING_PERIOD_MICROS = (1000 / 200) * 1000;

        private final SensorManager sensorManager;
        private final Sensor acceleration;
        private final int accelX[] = new int[Config.GRAPH_LENGTH];
        private final int accelY[] = new int[Config.GRAPH_LENGTH];
        private final int accelZ[] = new int[Config.GRAPH_LENGTH];
        private boolean isShaking = false;

        public AccelerationProcessor(SensorManager sensorManager) {
            this.sensorManager = sensorManager;
            acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        public void start() {
            sensorManager.registerListener(this, acceleration, SAMPLING_PERIOD_MICROS);
        }

        public void stop() {
            sensorManager.unregisterListener(this);
        }

        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            System.arraycopy(accelX, 1, accelX, 0, accelX.length - 1);
            accelX[accelX.length - 1] = (int) (event.values[0] * 100);

            System.arraycopy(accelY, 1, accelY, 0, accelY.length - 1);
            accelY[accelY.length - 1] = (int) (event.values[1] * 100);

            System.arraycopy(accelZ, 1, accelZ, 0, accelZ.length - 1);
            accelZ[accelZ.length - 1] = (int) (event.values[2] * 100);

            isShaking = calculateShaking(accelX) > SHAKING_THRESHOLD
                    || calculateShaking(accelY) > SHAKING_THRESHOLD
                    || calculateShaking(accelZ) > SHAKING_THRESHOLD;
            onShakingChange(isShaking);
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
