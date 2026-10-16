package com.opencapture.openpocketcine

import android.content.Context
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.opencapture.monitorui.ReviewLaunch
import com.opencapture.monitorui.ReviewLiveValues
import com.opencapture.opencine.core.CameraTimecode
import com.opencapture.opencine.core.ConnectedCamera
import com.opencapture.opencine.core.SavedCamera
import com.opencapture.opencine.dji.DjiCameraSessionAdapter
import com.opencapture.opencine.platform.ReviewMediaSeed
import com.opencapture.opencine.vocabulary.CameraCommands
import com.opencapture.opencine.vocabulary.CameraModel
import com.opencapture.opencine.vocabulary.CameraStatus
import com.opencapture.opencine.vocabulary.FoundCamera
import com.opencapture.opencine.vocabulary.MediaFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Modifier.reviewMetadata(review: ReviewLaunch?): Modifier =
    if (review == null) this else semantics { testTagsAsResourceId = true }

/**
 * SIMULATED reviews: the local media review through the production cache, catalog, gallery and
 * player, and the live review (`ReviewLaunch` LIVE) through the production DJI session projection,
 * monitor and capture popups, with no radio, link or decoder.
 */
internal object MonitorReview {
    fun decode(intent: Intent?): ReviewLaunch? = ReviewLaunch.decode { intent?.getStringExtra(it) }
        ?.takeIf {
            it.screen == ReviewLaunch.Screen.LIVE ||
                (it.screen == ReviewLaunch.Screen.MEDIA && it.source == ReviewLaunch.Source.LOCAL && !it.connected)
        }

    /** The review body, a Pocket 4 Pro as the iOS live review stages it. */
    val liveCamera = FoundCamera("simulated-dji", "SIMULATED", "SIMULATED Pocket 4 Pro",
        CameraModel(name = "Osmo Pocket 4 Pro", family = "pocket", verified = true), null)

    /** The shared LIVE review values as this camera reports them, in its own colour (D-Log2). */
    fun liveStatus(values: ReviewLiveValues = ReviewLiveValues.SIMULATED): CameraStatus =
        CameraStatus(batteryPercent = values.batteryPercent, storageTotalMb = values.storageTotalMebibytes.toInt(),
            storageFreeMb = values.storageFreeMebibytes.toInt(), shootingMode = 1, iso = values.iso,
            isoIndex = 7, shutterDenom = values.shutterDenominator, expoMode = CameraCommands.EXPO_MANUAL,
            colorMode = CameraCommands.COLOR_DLOG2, resolutionCode = CameraCommands.RES_4K, fps = values.framesPerSecond,
            fpsIndex = 2, wbMode = 6, wbKelvin = values.whiteBalanceKelvin, focusMode = 2, focusTrack = 0, audioChannel = 2)
            .withTimecodeValue(CameraTimecode(values.timecodeHours, values.timecodeMinutes, values.timecodeSeconds, frames = 0))

    suspend fun start(context: Context, model: AppModel, review: ReviewLaunch) {
        if (review.screen == ReviewLaunch.Screen.LIVE) {
            model.session.stageSimulatedLive(liveCamera, liveStatus())
            model.shell.present(ConnectedCamera(SavedCamera(id = liveCamera.id,
                integration = DjiCameraSessionAdapter.INTEGRATION, advertisedName = liveCamera.name,
                model = liveCamera.model.name), model.session.control))
            return
        }
        val namespace = "SIMULATED-S1"
        withContext(Dispatchers.IO) {
            val cache = model.mediaCache
            val clips = ReviewMediaSeed.write(File(context.cacheDir, "SIMULATED-S1"), review.mediaCount) { clip ->
                val file = file(clip)
                ReviewMediaSeed.Destination(cache.fileCache(namespace, file), cache.thumbnailFile(namespace, file))
            }
            cache.persistCatalog(clips.map(::file), namespace)
            cache.rememberCameraId(namespace)
        }
        model.homePanel = AppPanel.MEDIA
    }

    private fun file(clip: ReviewMediaSeed.Clip): MediaFile = MediaFile(
        path = "DCIM/SIMULATED/${clip.filename}", thumbPath = "DCIM/SIMULATED/${clip.filename}",
        sizeBytes = clip.byteCount, durationSeconds = clip.durationSeconds,
        resolution = "${clip.width}x${clip.height}", fps = clip.frameRate,
    )
}
