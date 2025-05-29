package dev.emmanuelrobinson.capacitormotion;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

@CapacitorPlugin(name = "Motion")
public class MotionPlugin extends Plugin {
    private Motion implementation;

    @Override
    public void load() {
        implementation = new Motion(this);
        System.out.println("MotionPlugin loaded and implementation initialized.");
    }

    @PluginMethod
    public void startMotionUpdates(PluginCall call) {
        if (implementation == null) {
            call.reject("Implementation not initialized");
            return;
        }
        implementation.startMotionUpdates(call);
    }

    @PluginMethod
    public void stopMotionUpdates(PluginCall call) {
        if (implementation == null) {
            call.reject("Implementation not initialized");
            return;
        }
        implementation.stopMotionUpdates(call);
    }

    @Override
    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void addListener(PluginCall call) {
        if (implementation == null) {
            System.err.println("MotionPlugin: addListener called before implementation was initialized!");
            call.reject("Implementation not initialized. Cannot add listener.");
            return;
        }
        
        String eventName = call.getString("eventName");
        if (eventName == null || eventName.isEmpty()) {
            System.err.println("MotionPlugin: addListener called without or with empty eventName string");
            call.reject("eventName string is required and cannot be empty.");
            return;
        }
        
        System.out.println("MotionPlugin: Adding listener for event: " + eventName + " (Callback ID: " + call.getCallbackId() + ")");
        
        // Call super.addListener first to register the listener on the JS side
        super.addListener(call);
        
        // Then start the appropriate sensor updates
        if ("accel".equals(eventName)) {
            implementation.startAccelerometerUpdates();
        } else if ("orientation".equals(eventName)) {
            implementation.startOrientationUpdates();
        } else if ("heading".equals(eventName)) {
            implementation.startHeadingUpdates();
        } else {
            System.out.println("MotionPlugin: Listener registered for unknown event type: " + eventName);
        }
        
        System.out.println("MotionPlugin: Listener processing completed for event: " + eventName);
    }

    @Override
    @PluginMethod
    public void removeAllListeners(PluginCall call) {
        if (implementation == null) {
            System.err.println("MotionPlugin: removeAllListeners called before implementation was initialized!");
            call.reject("Implementation not initialized. Cannot remove listeners.");
            return;
        }
        
        // Stop all sensor updates first
        implementation.removeAllListeners(call);
        
        // Then call super to clean up JS listeners
        super.removeAllListeners(call);
    }

    /**
     * Helper method to allow the Motion implementation class to call notifyListeners.
     */
    public void bridgeNotifyListeners(String eventNameParam, JSObject data) {
        final String localEventName = eventNameParam;

        System.out.println("MotionPlugin: bridgeNotifyListeners received call for event: " + localEventName);
        if (data != null) {
            System.out.println("MotionPlugin: data for " + localEventName + " = " + data.toString());
        } else {
            System.out.println("MotionPlugin: data for " + localEventName + " is null");
        }
        
        try {
            // Call notifyListeners directly using the local final variable and set retainUntilConsumed to true
            notifyListeners(localEventName, data, true);
            System.out.println("MotionPlugin: Successfully called Capacitor's notifyListeners for " + localEventName + " with retainUntilConsumed=true");
        } catch (Exception e) {
            System.err.println("MotionPlugin: Error in Capacitor's notifyListeners for " + localEventName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}