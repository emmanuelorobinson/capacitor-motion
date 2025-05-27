import Foundation

@objc public class Motion: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
