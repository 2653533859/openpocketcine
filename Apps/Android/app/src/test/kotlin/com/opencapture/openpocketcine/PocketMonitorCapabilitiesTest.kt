package com.opencapture.openpocketcine

import com.opencapture.opencine.core.CapabilitySnapshot
import com.opencapture.opencine.core.MonitorLayoutCapabilities
import com.opencapture.opencine.dji.DjiCameraSessionAdapter
import com.opencapture.opencine.vocabulary.CameraModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pocket main laid out the timecode and audio on every body (`PocketMonitorCapabilities` `timecode = true`, `audio = true`). */
class PocketMonitorCapabilitiesTest {
    private fun declared(model: CameraModel?) = MonitorLayoutCapabilities(
        CapabilitySnapshot(
            timecodeCapability = DjiCameraSessionAdapter.timecodeCapability(model),
            audioLevels = DjiCameraSessionAdapter.audioLevelsSupport(model),
        ),
        appLayoutBase,
    )

    @Test fun theLayoutIsTheDeclarationWithNoAppTimecodeOverride() {
        val nano = declared(CameraModel(name = "Osmo Nano", family = "nano", verified = true))
        // Multi View reads the declaration and hides the Nano clock, as main did. The single-view
        // clock slot is the shared DISP rule (MonitorOperatorVocabularySurface), not an app flag.
        assertFalse(nano.timecode)
        assertTrue(declared(CameraModel(name = "Osmo Pocket 3", verified = true)).timecode)
    }

    @Test fun audioPinAndTileStayOfferedBeforeTheBodyIsKnown() {
        assertTrue(declared(null).audio)
    }
}
