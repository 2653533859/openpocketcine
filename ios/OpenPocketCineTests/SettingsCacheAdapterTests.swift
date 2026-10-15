import Foundation
import OpenCineAppKit
import XCTest

@testable import OpenPocketCine

private final class SettingsCacheFileManager: FileManager, @unchecked Sendable {
    let support = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    override func urls(for directory: SearchPathDirectory, in domainMask: SearchPathDomainMask) -> [URL] {
        directory == .applicationSupportDirectory ? [support] : super.urls(for: directory, in: domainMask)
    }
}

/// SIMULATED real cache files through the actual shipping Settings source factory.
@MainActor final class SettingsCacheAdapterTests: XCTestCase {
    func testRetainedSettingsSourceCannotClearAReplacementSession() async throws {
        let firstManager = SettingsCacheFileManager(), nextManager = SettingsCacheFileManager()
        let firstMedia = CameraMedia(fileManager: firstManager)
        let nextMedia = CameraMedia(fileManager: nextManager)
        let first = CameraSession(borrowing: HevcDecoder(), cameraMedia: firstMedia)
        let next = CameraSession(borrowing: HevcDecoder(), cameraMedia: nextMedia)
        let model = AppModel(session: first)
        let cache = MonitorSettingsCacheModel()
        defer {
            cache.retire()
            model.delivery.close()
            first.disconnect(); next.disconnect()
            first.decoder.close(); next.decoder.close()
            try? FileManager.default.removeItem(at: firstManager.support)
            try? FileManager.default.removeItem(at: nextManager.support)
        }
        try firstMedia.cacheDirectory(cameraID: first.mediaCameraID).write(Data(repeating: 1, count: 4096), at: "fixture.bin")
        try nextMedia.cacheDirectory(cameraID: next.mediaCameraID).write(Data(repeating: 2, count: 8192), at: "fixture.bin")
        let retained = model.settingsCacheSource
        model.session = next
        XCTAssertEqual(retained.revision, next.mediaCacheRevision,
            "Coincident content revisions must not admit a replaced session")
        XCTAssertNil(model.shell.monitor)
        cache.refresh(retained)
        cache.requestClear()
        XCTAssertFalse(cache.confirmationPresented)
        cache.clear()
        for _ in 0..<1000 where cache.clearing { await Task.yield() }
        XCTAssertFalse(cache.clearing)
        let firstBytes = await firstMedia.cacheByteCountAllCameras()
        let nextBytes = await nextMedia.cacheByteCountAllCameras()
        XCTAssertEqual(firstBytes, 4096)
        XCTAssertEqual(nextBytes, 8192, "The stale parent must send no Clear to its replacement")
    }
}
