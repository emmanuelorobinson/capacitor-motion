// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorMotion",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorMotion",
            targets: ["MotionPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "MotionPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/MotionPlugin"),
        .testTarget(
            name: "MotionPluginTests",
            dependencies: ["MotionPlugin"],
            path: "ios/Tests/MotionPluginTests")
    ]
)
