import Foundation
import AVFoundation
import AppKit

let input = URL(fileURLWithPath: CommandLine.arguments[1])
let output = URL(fileURLWithPath: CommandLine.arguments[2], isDirectory: true)
let asset = AVURLAsset(url: input)
let duration = CMTimeGetSeconds(asset.duration)
precondition(duration > 0)
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = CGSize(width: 540, height: 1200)
generator.requestedTimeToleranceBefore = .zero
generator.requestedTimeToleranceAfter = .zero
for (index, seconds) in [0.0, duration / 2, max(0, duration - 1)].enumerated() {
    let frame = try generator.copyCGImage(at: CMTime(seconds: seconds, preferredTimescale: 600), actualTime: nil)
    let bitmap = NSBitmapImageRep(cgImage: frame)
    let data = bitmap.representation(using: .png, properties: [:])!
    try data.write(to: output.appendingPathComponent("candidate-video-frame-\(index).png"))
}
let summary: [String: Any] = ["duration_seconds": duration, "decoded_frames": 3, "file": input.lastPathComponent]
try JSONSerialization.data(withJSONObject: summary, options: [.prettyPrinted, .sortedKeys])
    .write(to: output.appendingPathComponent("video-validation.json"))
print("Decoded three frames; duration \(duration) seconds")
