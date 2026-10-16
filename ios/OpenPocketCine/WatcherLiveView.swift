import OpenCineAppKitApple
import MonitorPresentation
import OpenCineCameraControls
import OpenPocketViewCore
import SwiftUI

/// Pocket monitor tools around a remote source (OpenCineCameraControls).
typealias WatcherLiveView = MonitorOperatorWatcherLiveView<AppModel>

extension AppModel: MonitorWatcherLiveHost {
    typealias Picture = PocketLivePicture
    static var assistStores: AssistPanelStores { .app }
    static var falseColorLooks: FalseColorLooks { .app }
    static var operatorHaptics: OperatorHaptics { .app }
    static var watcherBrand: MonitorWatcherLiveBrand {
        MonitorWatcherLiveBrand(lookProfile: { state in
            DJICameraLUTProfile.make(
                family: state.isNano == true ? .nano : .pocket, cameraName: state.cameraModel)
        })
    }
    func inspectorPreview(tool: MonitorAssistSelection) -> some View { AssistInspectorPreview(tool: tool) }
    func lookMenu() -> some View { LUTAssist.longPressMenu(assist: assist) }
}

extension HevcDecoder: MonitorOperatorLiveDecoder {}
