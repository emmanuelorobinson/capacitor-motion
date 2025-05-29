package dev.emmanuelrobinson.capacitormotion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.getcapacitor.Plugin;

public class Motion implements SensorEventListener, LocationListener {
    private MotionPlugin motionPlugin;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private Sensor headingSensor;
    private Sensor rotationVectorSensor;
    private boolean isAccelActive = false;
    private boolean isOrientationActive = false;
    private boolean isHeadingActive = false;

    // Location for true north calculation
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private double currentAltitude = 0.0;

    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];
    private float[] magnetometerValues = new float[3];
    private float[] headingValues = new float[1];
    private float[] rotationVectorValues = new float[5];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float[] inclinationMatrix = new float[9];
    private GeomagneticField geomagneticField;

    // Magnetic field strength monitoring for interference detection
    private float[] lastMagneticFieldStrength = new float[3];
    private boolean useRotationVectorForHeading = false;

    public Motion(MotionPlugin plugin) {
        this.motionPlugin = plugin;
        this.sensorManager = (SensorManager) plugin.getContext().getSystemService(Context.SENSOR_SERVICE);
        this.locationManager = (LocationManager) plugin.getContext().getSystemService(Context.LOCATION_SERVICE);

        if (this.sensorManager != null) {
            this.accelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            this.gyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            this.magnetometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            this.headingSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_HEADING);
            this.rotationVectorSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            // Prefer rotation vector sensor for heading as it's less susceptible to
            // magnetic interference
            useRotationVectorForHeading = (rotationVectorSensor != null);

            // Log sensor availability for debugging
            System.out.println("Motion: Sensor availability - Accelerometer: " + (accelerometer != null) +
                    ", Gyroscope: " + (gyroscope != null) +
                    ", Magnetometer: " + (magnetometer != null) +
                    ", Heading: " + (headingSensor != null) +
                    ", RotationVector: " + (rotationVectorSensor != null) +
                    ", Using RotationVector for heading: " + useRotationVectorForHeading);
        }

        // Try to get last known location for geomagnetic field calculation
        if (locationManager != null) {
            try {
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (lastLocation != null) {
                    currentLatitude = lastLocation.getLatitude();
                    currentLongitude = lastLocation.getLongitude();
                    currentAltitude = lastLocation.getAltitude();
                    System.out.println("Motion: Using last known location for true north: " + currentLatitude + ", "
                            + currentLongitude);
                }
            } catch (SecurityException e) {
                System.out.println(
                        "Motion: Location permission not granted, using default location for geomagnetic field");
                // Use a default location (e.g., San Francisco) if no location access
                currentLatitude = 37.7749;
                currentLongitude = -122.4194;
                currentAltitude = 0.0;
            }
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
        if (isAccelActive || isOrientationActive || isHeadingActive) {
            sensorManager.unregisterListener(this);
            if (locationManager != null && isHeadingActive) {
                try {
                    locationManager.removeUpdates(this);
                    System.out.println("Motion: Stopped location updates");
                } catch (SecurityException e) {
                    System.out.println("Motion: Error stopping location updates: " + e.getMessage());
                }
            }
            isAccelActive = false;
            isOrientationActive = false;
            isHeadingActive = false;
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
        } else if ("heading".equals(eventName)) {
            startHeadingUpdates();
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
        if (locationManager != null && isHeadingActive) {
            try {
                locationManager.removeUpdates(this);
                System.out.println("Motion: Stopped location updates");
            } catch (SecurityException e) {
                System.out.println("Motion: Error stopping location updates: " + e.getMessage());
            }
        }
        isAccelActive = false;
        isOrientationActive = false;
        isHeadingActive = false;
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

    public void startHeadingUpdates() {
        if (isHeadingActive) {
            System.out.println("Motion: Heading updates already active.");
            return;
        }

        // Priority order: 1) Rotation Vector (most stable), 2) Dedicated heading
        // sensor, 3) Calculated from mag+accel
        if (useRotationVectorForHeading && rotationVectorSensor != null) {
            System.out.println("Motion: Starting heading updates with Rotation Vector sensor (interference-resistant)");
            isHeadingActive = true;
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME);
        } else if (headingSensor != null) {
            System.out.println("Motion: Starting heading updates with dedicated heading sensor");
            isHeadingActive = true;
            sensorManager.registerListener(this, headingSensor, SensorManager.SENSOR_DELAY_GAME);
        } else if (magnetometer != null && accelerometer != null) {
            // Fallback to calculated heading from magnetometer and accelerometer
            System.out.println(
                    "Motion: Starting heading updates with calculated true north heading (magnetometer + accelerometer)");
            isHeadingActive = true;
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

            // Optionally request location updates for more accurate true north
            if (locationManager != null) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100, this);
                    System.out.println("Motion: Requested location updates for true north accuracy");
                } catch (SecurityException e) {
                    System.out.println("Motion: Location permission not granted, using last known/default location");
                }
            }
        } else {
            System.out.println("Motion: No sensors available for heading calculation.");
        }
    }

    private void calculateHeadingFromRotationVector() {
        if (rotationVectorValues.length < 4)
            return;

        // Get rotation matrix from rotation vector
        float[] rotationMatrixFromVector = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrixFromVector, rotationVectorValues);

        // Get orientation from rotation matrix
        float[] orientationFromVector = new float[3];
        SensorManager.getOrientation(rotationMatrixFromVector, orientationFromVector);

        // Get heading (azimuth) in radians and convert to degrees
        double headingRadians = orientationFromVector[0];
        double headingDegrees = Math.toDegrees(headingRadians);

        // Apply magnetic declination to get true north
        try {
            if (geomagneticField == null) {
                geomagneticField = new GeomagneticField(
                        (float) currentLatitude,
                        (float) currentLongitude,
                        (float) currentAltitude,
                        System.currentTimeMillis());
            }

            float declination = geomagneticField.getDeclination();
            headingDegrees += declination;

            System.out.println("Motion: RotationVector heading: " + Math.toDegrees(headingRadians) +
                    "°, Declination: " + declination + "°, True heading: " + headingDegrees + "°");

        } catch (Exception e) {
            System.out.println(
                    "Motion: Error calculating declination for rotation vector, using raw heading: " + e.getMessage());
        }

        // Normalize to 0-360 degrees
        while (headingDegrees < 0)
            headingDegrees += 360;
        while (headingDegrees >= 360)
            headingDegrees -= 360;

        JSObject data = new JSObject();
        data.put("heading", headingDegrees);

        System.out.println(
                "[Motion.java] calculateHeadingFromRotationVector: About to notify listeners for 'heading' with interference-resistant value: "
                        + headingDegrees);
        motionPlugin.bridgeNotifyListeners("heading", data);
        System.out.println(
                "[Motion.java] calculateHeadingFromRotationVector: Finished notifying listeners for 'heading'");
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
                // Calculate heading if we don't have a dedicated heading sensor
                if (isHeadingActive && headingSensor == null && magnetometerValues.length > 0) {
                    calculateHeading();
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
                // Calculate heading if we don't have rotation vector or dedicated heading
                // sensor
                if (isHeadingActive && !useRotationVectorForHeading && headingSensor == null
                        && accelerometerValues.length > 0) {
                    calculateHeading();
                }
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                System.arraycopy(event.values, 0, rotationVectorValues, 0,
                        Math.min(event.values.length, rotationVectorValues.length));
                if (isHeadingActive && useRotationVectorForHeading) {
                    calculateHeadingFromRotationVector();
                }
                break;

            case Sensor.TYPE_HEADING:
                System.arraycopy(event.values, 0, headingValues, 0, event.values.length);
                if (isHeadingActive) {
                    sendDirectHeadingData();
                }
                break;
        }
    }

    private void sendAccelerometerData() {
        if (accelerometerValues.length < 3 || gyroscopeValues.length < 3) {
            System.out
                    .println("[Motion.java] sendAccelerometerData: Insufficient data from accel or gyro. Accel length: "
                            + accelerometerValues.length + ", Gyro length: " + gyroscopeValues.length);
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
        if (accelerometerValues.length < 3 || magnetometerValues.length < 3)
            return;

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

    private void calculateHeading() {
        if (accelerometerValues.length < 3 || magnetometerValues.length < 3)
            return;

        if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerValues,
                magnetometerValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            // Get magnetic heading first (azimuth in radians)
            double magneticHeadingRadians = orientationAngles[0];
            double magneticHeadingDegrees = Math.toDegrees(magneticHeadingRadians);

            // Calculate true north by applying magnetic declination
            double trueHeadingDegrees = magneticHeadingDegrees;

            try {
                // Create GeomagneticField to get magnetic declination
                geomagneticField = new GeomagneticField(
                        (float) currentLatitude,
                        (float) currentLongitude,
                        (float) currentAltitude,
                        System.currentTimeMillis());

                // Get magnetic declination (difference between magnetic north and true north)
                float declination = geomagneticField.getDeclination();

                // Apply declination to get true heading
                trueHeadingDegrees = magneticHeadingDegrees + declination;

                System.out.println("Motion: Magnetic heading: " + magneticHeadingDegrees + "°, Declination: "
                        + declination + "°, True heading: " + trueHeadingDegrees + "°");

            } catch (Exception e) {
                System.out.println(
                        "Motion: Error calculating magnetic declination, using magnetic heading: " + e.getMessage());
            }

            // Normalize to 0-360 degrees
            while (trueHeadingDegrees < 0)
                trueHeadingDegrees += 360;
            while (trueHeadingDegrees >= 360)
                trueHeadingDegrees -= 360;

            // Keep the calculation logic but don't send notifications when using rotation
            // vector
            System.out.println(
                    "[Motion.java] calculateHeading: Calculated true north value: " + trueHeadingDegrees +
                            " (not sending notification - using rotation vector instead)");
        }
    }

    private void sendDirectHeadingData() {
        if (headingValues.length < 1)
            return;

        // The heading sensor typically provides values in degrees (0-360)
        double headingDegrees = headingValues[0];

        // Ensure the value is normalized to 0-360 range
        while (headingDegrees < 0)
            headingDegrees += 360;
        while (headingDegrees >= 360)
            headingDegrees -= 360;

        JSObject data = new JSObject();
        data.put("heading", headingDegrees);

        System.out.println(
                "[Motion.java] sendDirectHeadingData: About to notify listeners for 'heading' with direct sensor value: "
                        + headingDegrees);
        motionPlugin.bridgeNotifyListeners("heading", data);
        System.out.println("[Motion.java] sendDirectHeadingData: Finished notifying listeners for 'heading'");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    // LocationListener implementation
    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        currentAltitude = location.getAltitude();
        System.out.println("Motion: Location updated for true north: " + currentLatitude + ", " + currentLongitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle status changes if needed
    }

    @Override
    public void onProviderEnabled(String provider) {
        System.out.println("Motion: Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        System.out.println("Motion: Location provider disabled: " + provider);
    }
}