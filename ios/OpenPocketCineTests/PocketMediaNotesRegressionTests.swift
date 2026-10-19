import Foundation
import OpenCineAppKit
import OpenPocketViewCore
import XCTest

@testable import OpenPocketCine

private final class NotesFileManager: FileManager, @unchecked Sendable {
    let support = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    override func urls(for directory: FileManager.SearchPathDirectory, in domainMask: FileManager.SearchPathDomainMask) -> [URL] {
        directory == .applicationSupportDirectory ? [support] : super.urls(for: directory, in: domainMask)
    }
}

/// Behavior-audit regressions: the shared catalog shows only the library's note line, so the
/// app's media notes and copy have to reach it (PIB-082, PIB-083).
@MainActor final class PocketMediaNotesRegressionTests: XCTestCase {
    func testHostNoteReachesTheSharedGalleryNoteLine() {
        let manager = NotesFileManager(), media = CameraMedia(fileManager: manager)
        let session = CameraSession(borrowing: HevcDecoder(), cameraMedia: media)
        defer { session.disconnect(); session.decoder.close(); try? FileManager.default.removeItem(at: manager.support) }
        let library = session.mediaLibrary
        session.mediaNote = MediaOperatorCopy.deleteFailed
        XCTAssertEqual(library.note, MediaOperatorCopy.deleteFailed)
    }

    func testGalleryCopyNamesPlaybackAndTheCameraGallery() {
        XCTAssertEqual(CameraSession.mediaLibraryCopy.openFailed, MediaOperatorCopy.playbackFailed)
        XCTAssertEqual(CameraSession.mediaLibraryCopy.sourceHeld, MediaOperatorCopy.browsingOnCamera)
        XCTAssertEqual(CameraSession.mediaLibraryCopy.empty, MediaOperatorCopy.noClips)
    }
}
