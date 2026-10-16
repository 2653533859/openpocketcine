import MonitorUI
import OpenCineAppKit
import OpenCineCore
import OpenPocketViewCore
import SwiftUI

/// Compatibility host for review callers; production slots return the typed flow directly.
struct AddSetupView: View {
    @Environment(AppModel.self) private var model
    let camera: SavedCamera
    let done: (RouteDraft?) -> Void

    var body: some View {
        AddSetupFlowView(
            flow: PocketAddSetup.flow(
                camera: camera, done: done,
                initialSelection: PocketAddSetup.selection(camera: camera, model: model)))
    }
}

/// Host destinations and actions for the shared setup capability chooser.
@MainActor
enum PocketAddSetup {
    static func flow(
        camera: SavedCamera, done: @escaping (RouteDraft?) -> Void, initialSelection: String? = nil
    ) -> AddSetupFlow {
        AddSetupFlow(
            chooser: StationNetworkSetupView.setupChooser(context: .camera(camera)),
            onSelect: { _ in },
            destination: { option in
                AnyView(PocketSetupDestination(camera: camera, kind: option.kind, done: done))
            },
            initialSelection: initialSelection, onCancel: { done(nil) })
    }

    static func selection(camera: SavedCamera, model: AppModel) -> String? {
        CamerasMapping.addSetupSelection(
            model.cameras, camera: camera,
            chooser: StationNetworkSetupView.setupChooser(context: .camera(camera)))
    }
}

/// The integration retains scanning, credential entry, persistence and route provisioning.
private struct PocketSetupDestination: View {
    @Environment(AppModel.self) private var model
    let camera: SavedCamera
    let kind: RouteKind
    let done: (RouteDraft?) -> Void

    var body: some View {
        StationNetworkSetupView(
            context: .camera(camera), scan: model.networkScan(camera),
            save: { setup, ssid, password in
                done(
                    RouteDraft(
                        kind: setup == .phoneHotspot ? .phoneHotspot : .infrastructureNetwork, network: ssid,
                        secret: password))
                return true
            }, close: { done(nil) }, path: path
        )
        .presentationBackground(MonitorTheme.background)
    }

    private var path: [StationNetworkSetupView.Page] {
        if kind == .phoneHotspot { return [.hotspot] }
        if PocketAddSetup.selection(camera: camera, model: model) == "wifi" {
            return [.password(camera.wifiSSID ?? "")]
        }
        return [.networks]
    }
}
