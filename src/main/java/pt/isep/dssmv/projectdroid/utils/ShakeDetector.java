package pt.isep.dssmv.projectdroid.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Native accelerometer shake detector satisfying US6 criteria:
 * - AC01: Encapsulates SensorEventListener for TYPE_ACCELEROMETER.
 * - AC02: Vectorial acceleration threshold g > 2.7g.
 * - AC03: Debounce cooldown of 1000ms.
 */
public class ShakeDetector implements SensorEventListener {

    public interface OnShakeListener {
        void onShake();
    }

    private static final float SHAKE_THRESHOLD_G = 2.7f;
    private static final int SHAKE_COOLDOWN_MS = 1000;

    private final OnShakeListener listener;
    private long lastShakeTimestamp = 0;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTimestamp >= SHAKE_COOLDOWN_MS) {
                lastShakeTimestamp = now;
                if (listener != null) {
                    listener.onShake();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
