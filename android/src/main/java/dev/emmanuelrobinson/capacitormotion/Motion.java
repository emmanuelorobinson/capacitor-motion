package dev.emmanuelrobinson.capacitormotion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.getcapacitor.Plugin;

public class Motion implements SensorEventListener {
    private MotionPlugin motionPlugin;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private boolean isAccelActive = false;
    private boolean isOrientationActive = false;

    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];
    private float[] magnetometerValues = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    public Motion(MotionPlugin plugin) {
        this.motionPlugin = plugin;
        this.sensorManager = (SensorManager) plugin.getContext().getSystemService(Context.SENSOR_SERVICE);
        if (this.sensorManager != null) {
            this.accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.gyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            this.magnetometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    public void startMotionUpdates(PluginCall call) {
        if (accelerometer == null && gyroscope == null && magnetometer == null) {
            call.reject("Motion sensors not available on this device.");
            return;
        }
        call.resolve();
    }

    public void stopMotionUpdates(PluginCall call) {
        if (isAccelActive || isOrientationActive) {
            sensorManager.unregisterListener(this);
            isAccelActive = false;
            isOrientationActive = false;
        }
        call.resolve();
    }

    public void addListener(PluginCall call) {
        String eventName = call.getString("eventName");
        if (eventName == null) {
            call.reject("eventName is required.");
            return;
        }

        System.out.println("Motion: addListener called for event: " + eventName);

        if ("accel".equals(eventName)) {
            startAccelerometerUpdates();
            call.resolve(); // This was missing!
        } else if ("orientation".equals(eventName)) {
            startOrientationUpdates();
            call.resolve(); // This was missing!
        } else {
            call.reject("Invalid event name: " + eventName);
        }
    }

    public void removeAllListeners(PluginCall call) {
        System.out.println("Motion: removeAllListeners called");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        isAccelActive = false;
        isOrientationActive = false;
        call.resolve();
    }

    public void startAccelerometerUpdates() {
        if (!isAccelActive && accelerometer != null && gyroscope != null) {
            System.out.println("Motion: Starting accelerometer updates");
            isAccelActive = true;
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        } else {
            if (isAccelActive) {
                System.out.println("Motion: Accelerometer updates already active.");
            } else {
                System.out.println("Motion: Accelerometer or Gyroscope sensor not available.");
            }
        }
    }

    public void startOrientationUpdates() {
        if (!isOrientationActive && accelerometer != null && magnetometer != null) {
            System.out.println("Motion: Starting orientation updates");
            isOrientationActive = true;
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            if (isOrientationActive) {
                System.out.println("Motion: Orientation updates already active.");
            } else {
                System.out.println("Motion: Accelerometer or Magnetometer sensor not available for orientation.");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.length);
                if (isAccelActive) {
                    sendAccelerometerData();
                }
                if (isOrientationActive && magnetometerValues.length > 0) { 
                    calculateOrientation();
                }
                break;

            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroscopeValues, 0, event.values.length);
                if (isAccelActive) { 
                    sendAccelerometerData();
                }
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnetometerValues, 0, event.values.length);
                if (isOrientationActive && accelerometerValues.length > 0) { 
                    calculateOrientation();
                }
                break;
        }
    }

    private void sendAccelerometerData() {
        if (accelerometerValues.length < 3 || gyroscopeValues.length < 3) {
            System.out.println("[Motion.java] sendAccelerometerData: Insufficient data from accel or gyro. Accel length: " + accelerometerValues.length + ", Gyro length: " + gyroscopeValues.length);
            return;
        }

        JSObject acceleration = new JSObject();
        acceleration.put("x", accelerometerValues[0]);
        acceleration.put("y", accelerometerValues[1]);
        acceleration.put("z", accelerometerValues[2]);

        JSObject accelerationIncludingGravity = new JSObject();
        accelerationIncludingGravity.put("x", accelerometerValues[0]);
        accelerationIncludingGravity.put("y", accelerometerValues[1]);
        accelerationIncludingGravity.put("z", accelerometerValues[2]);

        JSObject rotationRate = new JSObject();
        rotationRate.put("alpha", Math.toDegrees(gyroscopeValues[2])); 
        rotationRate.put("beta", Math.toDegrees(gyroscopeValues[0]));  
        rotationRate.put("gamma", Math.toDegrees(gyroscopeValues[1])); 

        JSObject data = new JSObject();
        data.put("acceleration", acceleration);
        data.put("accelerationIncludingGravity", accelerationIncludingGravity);
        data.put("rotationRate", rotationRate);
        data.put("interval", 16.67); 

        System.out.println("[Motion.java] sendAccelerometerData: About to notify listeners for 'accel'");
        motionPlugin.bridgeNotifyListeners("accel", data);
        System.out.println("[Motion.java] sendAccelerometerData: Finished notifying listeners for 'accel'");
    }

    private void calculateOrientation() {
        if (accelerometerValues.length < 3 || magnetometerValues.length < 3) return; 

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            JSObject data = new JSObject();
            data.put("alpha", Math.toDegrees(orientationAngles[0]));
            data.put("beta", Math.toDegrees(orientationAngles[1]));  
            data.put("gamma", Math.toDegrees(orientationAngles[2])); 

            System.out.println("[Motion.java] calculateOrientation: About to notify listeners for 'orientation'");
            motionPlugin.bridgeNotifyListeners("orientation", data);
            System.out.println("[Motion.java] calculateOrientation: Finished notifying listeners for 'orientation'");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }
}