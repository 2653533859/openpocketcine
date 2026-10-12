package com.opencapture.openpocketcine.multiview

import com.opencapture.opencine.vocabulary.CameraStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiviewTileReadoutsTest {
    @Test fun unknownTelemetryNeverLooksLikeStandbyOrFullStorage() {
        val readouts = readouts(CameraStatus(), observed = null, available = false)
        assertEquals("—", readouts.timecode)
        assertEquals("—", readouts.battery)
        assertEquals("—", readouts.storage)
        assertEquals("REC —", readouts.recording)
        assertFalse(readouts.isRecording)
        assertTrue(multiviewExposureReadouts(CameraStatus()).all { it.second == "—" })
    }

    @Test fun reportedZeroBatteryAndFullCardRemainKnownValues() {
        val readouts = readouts(CameraStatus(batteryPercent = 0, sdTotalMb = 1024, sdFreeMb = 0), false, true)
        assertEquals("0%", readouts.battery)
        assertEquals("0 GB", readouts.storage)
        assertEquals("STBY", readouts.recording)
    }

    @Test fun eachCameraKeepsItsOwnTimecodeAndRecordingConfirmation() {
        val one = readouts(CameraStatus(recordElapsedSec = 84), true, true, timecode = "01:02:03")
        val two = readouts(CameraStatus(), false, true, timecode = "10:20:30")
        assertEquals("01:02:03", one.timecode)
        assertEquals("REC 1:24", one.recording)
        assertEquals("10:20:30", two.timecode)
        assertEquals("STBY", two.recording)
        val stale = readouts(CameraStatus(recordElapsedSec = 84), true, false)
        assertEquals("REC —", stale.recording)
    }

    @Test fun compactReadoutsRetainIdentityAndUnknownFields() {
        val readouts = readouts(CameraStatus(batteryPercent = 73, storageTotalMb = 16384, storageFreeMb = 8192), false, true)
        assertEquals("B", readouts.letter)
        assertEquals("Side angle", readouts.name)
        assertEquals("Osmo Pocket 4", readouts.model)
        assertEquals("73%", readouts.battery)
        assertEquals("8 GB", readouts.storage)
    }

    /** [timecode] is the tile's clock as the shared status formats it (`MultiviewSession.Tile.timecodeReadout`). */
    private fun readouts(settings: CameraStatus, observed: Boolean?, available: Boolean, timecode: String? = null) = multiviewTileReadouts(
        1, "Side angle", "Osmo Pocket 4", settings, timecode, observed, available,
        recordingBusy = false, recovering = false, hasPicture = true, failure = null,
    )
}
