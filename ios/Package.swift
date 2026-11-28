// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ProjectApex",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "ProjectApex",
            targets: ["ProjectApex"]
        )
    ],
    targets: [
        .target(
            name: "ProjectApex",
            path: "ProjectApex/Sources",
            exclude: ["ProjectApexApp.swift"]
        ),
        .testTarget(
            name: "ProjectApexTests",
            dependencies: ["ProjectApex"],
            path: "ProjectApex/Tests"
        )
    ]
)
