import Foundation
import Capacitor
// CoreMotion is not strictly needed here if all CoreMotion logic is in Motion.swift

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(MotionPlugin)
public class MotionPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "MotionPlugin"
    public let jsName = "Motion" // This should match the plugin name used in JS
    // CAPBridgedPlugin requires pluginMethods to be defined.
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startMotionUpdates", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopMotionUpdates", returnType: CAPPluginReturnPromise)
        // addListener and removeAllListeners are handled by CAPPlugin and overridden below,
        // so they don't need to be listed here for CAPBridgedPlugin purposes.
    ]
    
    // Pass self (plugin instance) to the implementation so it can call notifyListeners
    private lazy var implementation: Motion = Motion(plugin: self)

    override public func load() {
        // Plugin loaded
        print("[MotionPlugin.swift] MotionPlugin (bridged) loaded. Implementation instance: \(implementation)")
    }

    @objc func startMotionUpdates(_ call: CAPPluginCall) {
        implementation.startMotionUpdates(call: call)
    }
    
    @objc func stopMotionUpdates(_ call: CAPPluginCall) {
        implementation.stopMotionUpdates(call: call)
    }
    
    // Override addListener to delegate to our implementation
    @objc override public func addListener(_ call: CAPPluginCall) {
        super.addListener(call) // Allow Capacitor to manage the listener call object and event registration
        implementation.addListener(call: call) // Let our implementation start the sensors
    }

    @objc override public func removeAllListeners(_ call: CAPPluginCall) {
        super.removeAllListeners(call) // Allow Capacitor to clean up its listener store
        implementation.removeAllListeners(call: call) // Tell our implementation to stop sensors
    }
}
