package pt.isep.dssmv.projectdroid.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Native accelerometer shake detector satisfying US6 criteria:
 * - AC01: Encapsulates SensorEventListener for TYPE_ACCELEROMETER.
 * - Vectorial acceleration threshold g > threshold (default 3.2g).
 * - Multi-peak gesture detection requiring deliberate back-and-forth movement to eliminate accidental triggers.
 * - Debounce cooldown of 1200ms.
 */
public class ShakeDetector implements SensorEventListener {

    public interface OnShakeListener {
        void onShake();
    }

    public static final float DEFAULT_THRESHOLD_G = 3.2f;
    private static final int SHAKE_SLOP_TIME_MS = 200;
    private static final int SHAKE_RESET_TIME_MS = 900;
    private static final int SHAKE_COOLDOWN_MS = 1200;

    private final OnShakeListener listener;
    private float shakeThresholdG = DEFAULT_THRESHOLD_G;
    private long lastShakePeakTimestamp = 0;
    private long lastShakeTriggerTimestamp = 0;
    private int shakeCount = 0;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    public ShakeDetector(OnShakeListener listener, float thresholdG) {
        this.listener = listener;
        this.shakeThresholdG = thresholdG;
    }

    public void setThreshold(float thresholdG) {
        this.shakeThresholdG = thresholdG;
    }

    public float getThreshold() {
        return shakeThresholdG;
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

        if (gForce > shakeThresholdG) {
            long now = System.currentTimeMillis();

            // Ignore if in cooldown period following a trigger
            if (now - lastShakeTriggerTimestamp < SHAKE_COOLDOWN_MS) {
                return;
            }

            // Ignore peaks occurring too fast (sensor noise/jitter)
            if (now - lastShakePeakTimestamp < SHAKE_SLOP_TIME_MS) {
                return;
            }

            // Reset shake count if too much time elapsed since previous peak
            if (now - lastShakePeakTimestamp > SHAKE_RESET_TIME_MS) {
                shakeCount = 0;
            }

            lastShakePeakTimestamp = now;
            shakeCount++;

            // Require at least 2 distinct shake direction reversals to confirm deliberate shake
            if (shakeCount >= 2) {
                shakeCount = 0;
                lastShakeTriggerTimestamp = now;
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
