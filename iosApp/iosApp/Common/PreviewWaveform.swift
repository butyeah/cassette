import SwiftUI

/// How many lines `PreviewWaveform` draws, and so how many colours it can use.
let previewWaveformLines = 3

/// A stack of horizontal wavy lines, one per entry in `colors` (up to `previewWaveformLines`), each
/// as wide as the view. While `playing` the waves ripple; otherwise they ease down to flat, dimmed
/// lines, and the view keeps its size so nothing around it shifts. A port of Android's
/// PreviewWaveform, which draws Material's wavy progress indicator with the same parameters.
///
/// Draws no background of its own: the colours are meant for whatever it sits on (see
/// `waveformColors`, which takes that background).
///
/// Decorative: it isn't driven by the audio itself. Callers just say whether something is playing.
struct PreviewWaveform: View {
    let playing: Bool
    let colors: [Color]
    var strokeWidth: CGFloat = 8
    var lineSpacing: CGFloat = 4

    /// One line of the waveform: each gets its own wave shape and speed, so the set reads as one
    /// waveform.
    private struct Line {
        let wavelength: CGFloat
        let speed: CGFloat
        let amplitude: CGFloat
    }

    private static let lines = [
        Line(wavelength: 40, speed: 40, amplitude: 1),
        Line(wavelength: 28, speed: 64, amplitude: 0.7),
        Line(wavelength: 52, speed: 26, amplitude: 0.85),
    ]
    private static let idleAlpha = 0.35
    /// How much taller than its stroke a line's frame is, leaving room for the wave's swing.
    private static let frameToStroke: CGFloat = 2.75

    var body: some View {
        WaveAnimation(running: playing) { amplitude, time in
            VStack(spacing: lineSpacing) {
                ForEach(Array(zip(colors, Self.lines).enumerated()), id: \.offset) { _, pair in
                    let (color, line) = pair
                    WaveLine(
                        amplitude: amplitude * line.amplitude,
                        phase: wavePhase(time: time, speed: line.speed, wavelength: line.wavelength),
                        wavelength: line.wavelength,
                        strokeWidth: strokeWidth
                    )
                    .stroke(color, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                    .frame(height: strokeWidth * Self.frameToStroke)
                }
            }
        }
        .opacity(playing ? 1 : Self.idleAlpha)
        .animation(.easeInOut(duration: 0.3), value: playing)
        .accessibilityHidden(true)
    }
}

/// One sine line across the frame, centred vertically. `amplitude` is a 0...1 fraction of the room
/// the frame leaves beside the stroke, and animates.
private struct WaveLine: Shape {
    var amplitude: CGFloat
    let phase: CGFloat
    let wavelength: CGFloat
    let strokeWidth: CGFloat

    var animatableData: CGFloat {
        get { amplitude }
        set { amplitude = newValue }
    }

    func path(in rect: CGRect) -> Path {
        // Round caps reach half a stroke past each end, so the line starts and stops that far in.
        let start = rect.minX + strokeWidth / 2
        let end = rect.maxX - strokeWidth / 2
        let swing = max(rect.height - strokeWidth, 0) / 2 * amplitude
        var path = Path()
        var x = start
        path.move(to: point(x, rect: rect, swing: swing))
        while x < end {
            x = min(x + 2, end)
            path.addLine(to: point(x, rect: rect, swing: swing))
        }
        return path
    }

    private func point(_ x: CGFloat, rect: CGRect, swing: CGFloat) -> CGPoint {
        CGPoint(x: x, y: rect.midY + swing * sin(2 * .pi * (x - phase) / wavelength))
    }
}
