import Foundation
import MonitorPresentation
import OpenCineCameraVocabulary
import OpenCineCore
import OpenPocketViewCore

/// Capability translation belongs to the Osmo adapter. Native monitor modules
/// never need to identify a camera brand, body name, or wire opcode.
@MainActor
enum OsmoMonitorPresentation {
    static func zoomTapStops(_ session: CameraSession) -> MonitorZoomTapStops {
        // Only the Pro's native capability table includes the 6×/12× range.
        // Other bodies retain their full ordinary tap sequence (including 4×).
        let supportsExtended =
            session.connectedCamera?.model.zoomStops.contains(12)
            ?? session.zoomStops.contains(12)
        return MonitorZoomTapStops(
            supported: session.zoomStops, extended: supportsExtended ? [6, 12] : [])
    }

    static func zoomCaption(_ session: CameraSession) -> String {
        MonitorZoomCaption.label(factor: session.zoomReadout, opticalStops: session.zoomStops)
    }

    /// The one control model of the connected body, as the DJI integration provides it
    /// (`DJICameraSessionAdapter.controlCapabilities`): what the shared capture UI offers.
    static func controls(_ session: CameraSession, codecColor: CaptureCodecColorChoices? = nil)
        -> ControlCapabilities
    {
        session.control.controlCapabilities(codecColor: codecColor)
    }

    /// What the monitor lays out: a view of the control model over the app's base. Only clip
    /// delete, clip star and the internet hop are the app's, until media and connection routes
    /// carry them (SHELL-02). Audio stays on before the body is known, as main offered the AUDIO
    /// tile and Clean view pin on every body (unknown keeps the base).
    static func capabilities(_ session: CameraSession) -> MonitorLayoutCapabilities {
        MonitorLayoutCapabilities(controls: controls(session), base: appBase)
    }

    private static let appBase: MonitorLayoutCapabilities = {
        var base = MonitorLayoutCapabilities()
        base.audio = true
        base.clipDelete = true
        base.clipStar = true
        base.requiresInternetHop = true
        return base
    }()
}
