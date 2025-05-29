import Foundation
import CoreMotion
import Capacitor

@objc public class Motion: NSObject {
    private let motionManager = CMMotionManager()
    private var isAccelActive = false
    private var isOrientationActive = false
    private var isHeadingActive = false
    private var isDeviceMotionRunning = false
    private weak var plugin: CAPPlugin?

    public init(plugin: CAPPlugin) {
        self.plugin = plugin
        super.init()
        motionManager.accelerometerUpdateInterval = 1.0 / 60.0 
        motionManager.gyroUpdateInterval = 1.0 / 60.0
        motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
        print("[Motion.swift] Motion implementation initialized. Plugin: \(plugin)")
    }

    @objc public func startMotionUpdates(call: CAPPluginCall) {
        print("[Motion.swift] startMotionUpdates called")
        guard motionManager.isDeviceMotionAvailable else {
            print("[Motion.swift] Device motion not available")
            call.reject("Device motion not available")
            return
        }
        call.resolve()
    }

    @objc public func stopMotionUpdates(call: CAPPluginCall) {
        print("[Motion.swift] stopMotionUpdates called. Stopping all motion updates.")
        stopDeviceMotionIfNeeded()
        isAccelActive = false
        isOrientationActive = false
        isHeadingActive = false
        call.resolve()
    }

    @objc public func addListener(call: CAPPluginCall) {
        let eventName = call.getString("eventName") ?? ""
        print("[Motion.swift] addListener called for event: \(eventName)")
        
        switch eventName {
        case "accel":
            self.startAccelerometerUpdates()
            call.resolve() 
        case "orientation":
            self.startOrientationUpdates()
            call.resolve()
        case "heading":
            self.startHeadingUpdates()
            call.resolve()
        default:
            print("[Motion.swift] addListener - Invalid event name: \(eventName)")
            call.reject("Invalid event name: \(eventName)")
            return
        }
    }

    @objc public func removeAllListeners(call: CAPPluginCall) {
        print("[Motion.swift] removeAllListeners called. Removing all listeners and stopping updates.")
        stopDeviceMotionIfNeeded()
        isAccelActive = false
        isOrientationActive = false
        isHeadingActive = false
        call.resolve()
    }
    
    private func startAccelerometerUpdates() {
        print("[Motion.swift] Attempting to start Accelerometer updates. isAccelActive: \(isAccelActive)")
        guard !isAccelActive else {
            print("[Motion.swift] Accelerometer updates already active.")
            return
        }
        
        isAccelActive = true
        startDeviceMotionIfNeeded()
    }
    
    private func startOrientationUpdates() {
        print("[Motion.swift] Attempting to start Orientation updates. isOrientationActive: \(isOrientationActive)")
        guard !isOrientationActive else {
            print("[Motion.swift] Orientation updates already active.")
            return
        }
        
        isOrientationActive = true
        startDeviceMotionIfNeeded()
    }

    private func startHeadingUpdates() {
        print("[Motion.swift] Attempting to start Heading updates. isHeadingActive: \(isHeadingActive)")
        guard !isHeadingActive else {
            print("[Motion.swift] Heading updates already active.")
            return
        }

        isHeadingActive = true
        startDeviceMotionIfNeeded()
    }
    
    private func startDeviceMotionIfNeeded() {
        // Only start device motion if it's not already running and we have at least one listener
        guard !isDeviceMotionRunning && (isAccelActive || isOrientationActive || isHeadingActive) else {
            print("[Motion.swift] Device motion already running or no active listeners")
            return
        }
        
        guard motionManager.isDeviceMotionAvailable else {
            print("[Motion.swift] Device motion not available")
            return
        }
        
        print("[Motion.swift] Starting shared DeviceMotion updates.")
        isDeviceMotionRunning = true
        
        motionManager.startDeviceMotionUpdates(using: .xMagneticNorthZVertical, to: OperationQueue.main) { [weak self] (motion, error) in
            guard let strongSelf = self else { 
                print("[Motion.swift] DeviceMotion - self is nil in closure")
                return
            }
            
            // Check if we still have active listeners
            guard strongSelf.isAccelActive || strongSelf.isOrientationActive || strongSelf.isHeadingActive else { 
                print("[Motion.swift] DeviceMotion - no active listeners, stopping")
                strongSelf.stopDeviceMotionIfNeeded()
                return
            }
            
            if let error = error {
                print("[Motion.swift] DeviceMotion - Error: \(error.localizedDescription)")
                if strongSelf.isAccelActive {
                    strongSelf.plugin?.notifyListeners("accelError", data: ["message": error.localizedDescription])
                }
                if strongSelf.isOrientationActive {
                    strongSelf.plugin?.notifyListeners("orientationError", data: ["message": error.localizedDescription])
                }
                return
            }
            
            guard let motion = motion else {
                print("[Motion.swift] DeviceMotion - No motion data in closure")
                return
            }
            
            // Handle accelerometer data if listener is active
            if strongSelf.isAccelActive {
                let acceleration = [
                    "x": motion.userAcceleration.x * 9.81, // Convert to m/s²
                    "y": motion.userAcceleration.y * 9.81,
                    "z": motion.userAcceleration.z * 9.81
                ]
                
                let accelerationIncludingGravity = [
                    "x": (motion.userAcceleration.x + motion.gravity.x) * 9.81,
                    "y": (motion.userAcceleration.y + motion.gravity.y) * 9.81,
                    "z": (motion.userAcceleration.z + motion.gravity.z) * 9.81
                ]
                
                let rotationRate = [
                    "alpha": motion.rotationRate.z * 180.0 / Double.pi, // Convert to degrees/second
                    "beta": motion.rotationRate.x * 180.0 / Double.pi,
                    "gamma": motion.rotationRate.y * 180.0 / Double.pi
                ]
                
                let accelData: [String: Any] = [
                    "acceleration": acceleration,
                    "accelerationIncludingGravity": accelerationIncludingGravity,
                    "rotationRate": rotationRate,
                    "interval": strongSelf.motionManager.deviceMotionUpdateInterval * 1000 // Convert to milliseconds
                ]
                
                print("[Motion.swift] Accel - Notifying listeners with data: \(accelData)")
                strongSelf.plugin?.notifyListeners("accel", data: accelData)
            }
            
            // Handle orientation data if listener is active
            if strongSelf.isOrientationActive {
                let attitude = motion.attitude
                let orientationData: [String: Any] = [
                    "alpha": attitude.yaw * 180.0 / Double.pi, 
                    "beta": attitude.pitch * 180.0 / Double.pi, 
                    "gamma": attitude.roll * 180.0 / Double.pi  
                ]
                
                print("[Motion.swift] Orientation - Notifying listeners with data: \(orientationData)")
                strongSelf.plugin?.notifyListeners("orientation", data: orientationData)
            }

            if strongSelf.isHeadingActive {
                let heading = motion.heading
                let headingData: [String: Any] = [
                    "heading": heading
                ]
                
                print("[Motion.swift] Heading - Notifying listeners with data: \(headingData)")
                strongSelf.plugin?.notifyListeners("heading", data: headingData)
            }
        }
    }
    
    private func stopDeviceMotionIfNeeded() {
        guard isDeviceMotionRunning else {
            print("[Motion.swift] Device motion not running, nothing to stop")
            return
        }
        
        // Only stop if no listeners are active
        guard !isAccelActive && !isOrientationActive && !isHeadingActive else {
            print("[Motion.swift] Still have active listeners, not stopping device motion")
            return
        }
        
        print("[Motion.swift] Stopping shared DeviceMotion updates.")
        motionManager.stopDeviceMotionUpdates()
        isDeviceMotionRunning = false
    }
}