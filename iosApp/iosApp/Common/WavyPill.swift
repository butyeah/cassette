import SwiftUI

// A port of Android's WavyPill.kt (cover/.../cover/components), which WavyPillTest covers there and
// WavyPillTests here.

/// Wave shape and speed for `WavyPill`, matching `PreviewWaveform`'s leading line.
let wavyPillWavelength: CGFloat = 40
let wavyPillWaveSpeed: CGFloat = 40

/// How far `WavyPill`'s edges swing at full amplitude. The pill is inset by this much.
let wavyPillMaxAmplitude: CGFloat = 3

/// Distance between the points a wavy edge is sampled at, in points.
private let sampleStep: CGFloat = 2

/// How far a wavy edge is displaced at `x`, for a straight run from `start` to `end` (the pill's
/// flat stretch between its round caps). A sine of `wavelength`, shifted by `phase`, scaled by
/// `amplitude`, and tapered to 0 over half a wavelength at either end so the wave meets the caps
/// without a kink. 0 outside the run.
func wavyEdgeOffset(x: CGFloat, start: CGFloat, end: CGFloat, amplitude: CGFloat, wavelength: CGFloat, phase: CGFloat) -> CGFloat {
    if x <= start || x >= end || amplitude == 0 { return 0 }
    let taper = min(wavelength / 2, (end - start) / 2)
    let fromEdge = min(x - start, end - x)
    let envelope = fromEdge >= taper ? 1 : smoothstep(fromEdge / taper)
    return amplitude * envelope * sin(2 * .pi * (x - phase) / wavelength)
}

private func smoothstep(_ t: CGFloat) -> CGFloat { t * t * (3 - 2 * t) }

/// A pill whose top and bottom edges are travelling waves: M3 Expressive's wavy look, as Android's
/// `wavyPillBackground` draws it. It's inset top and bottom by `maxAmplitude` so the waves never
/// leave its frame; its ends stay round.
///
/// `amplitude` is a 0...1 fraction of `maxAmplitude` (0 draws a plain pill) and animates; `phase` is
/// how far the wave has travelled, in points.
struct WavyPill: Shape {
    var amplitude: CGFloat
    var phase: CGFloat
    var wavelength: CGFloat = wavyPillWavelength
    var maxAmplitude: CGFloat = wavyPillMaxAmplitude

    var animatableData: CGFloat {
        get { amplitude }
        set { amplitude = newValue }
    }

    func path(in rect: CGRect) -> Path {
        let inset = maxAmplitude
        let top = rect.minY + inset
        let bottom = rect.maxY - inset
        let radius = max((bottom - top) / 2, 0)
        let start = rect.minX + radius
        let end = max(rect.maxX - radius, start)
        let swing = min(max(amplitude, 0), 1) * inset

        func offset(_ x: CGFloat) -> CGFloat {
            wavyEdgeOffset(x: x - rect.minX, start: radius, end: end - rect.minX, amplitude: swing, wavelength: wavelength, phase: phase)
        }

        // Top edge, left to right, then the right cap; bottom edge back, then the left cap. The
        // bottom mirrors the top, so the pill swells and pinches rather than bending.
        var path = Path()
        path.move(to: CGPoint(x: start, y: top))
        var x = start
        while x < end {
            x = min(x + sampleStep, end)
            path.addLine(to: CGPoint(x: x, y: top - offset(x)))
        }
        path.addArc(center: CGPoint(x: end, y: top + radius), radius: radius,
                    startAngle: .degrees(-90), endAngle: .degrees(90), clockwise: false)
        while x > start {
            x = max(x - sampleStep, start)
            path.addLine(to: CGPoint(x: x, y: bottom + offset(x)))
        }
        path.addArc(center: CGPoint(x: start, y: top + radius), radius: radius,
                    startAngle: .degrees(90), endAngle: .degrees(270), clockwise: false)
        path.closeSubpath()
        return path
    }
}

/// Drives a wave for `content`: an amplitude that eases between 0 and 1 as `running` changes, and a
/// clock, in seconds, for its phase. The clock runs while `running`, and keeps going while the
/// amplitude eases back to 0, so the wave settles flat while moving instead of freezing
/// mid-ripple. Idle, it asks for no frames. With Reduce Motion on, the clock stands still.
struct WaveAnimation<Content: View>: View {
    let running: Bool
    var animation: Animation = .spring(duration: 0.5, bounce: 0.2)
    @ViewBuilder let content: (_ amplitude: CGFloat, _ time: Double) -> Content

    @State private var amplitude: CGFloat = 0
    @State private var settling = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: reduceMotion || (!running && !settling))) { timeline in
            content(amplitude, timeline.date.timeIntervalSinceReferenceDate)
        }
        .onAppear { amplitude = running ? 1 : 0 }
        .onChange(of: running) { _, running in
            settling = !running
            withAnimation(animation, completionCriteria: .logicallyComplete) {
                amplitude = running ? 1 : 0
            } completion: {
                settling = false
            }
        }
    }
}

/// How far a wave travelling at `speed` points per second has moved by `time`, within one `wavelength`.
func wavePhase(time: Double, speed: CGFloat, wavelength: CGFloat) -> CGFloat {
    CGFloat((time * Double(speed)).truncatingRemainder(dividingBy: Double(wavelength)))
}
