package com.opencapture.openpocketcine

import com.opencapture.opencine.appkit.ShellModel
import android.content.Context
import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.opencapture.monitorui.ReviewLaunch
import com.opencapture.opencine.core.ConnectedCamera
import com.opencapture.opencine.core.SavedCamera
import com.opencapture.opencine.dji.DjiCameraSessionAdapter
import com.opencapture.opencine.platform.ReviewMediaSeed
import com.opencapture.opencine.vocabulary.MediaFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun Modifier.reviewMetadata(review: ReviewLaunch?): Modifier =
    if (review == null) this else semantics { testTagsAsResourceId = true }

/** SIMULATED review through the production shell, cache, catalog, gallery and player. */
internal object MonitorReview {
    /** The screens this host stages; anything else decodes null and runs the real startup. */
    private val screens = setOf(
        ReviewLaunch.Screen.CAMERAS, ReviewLaunch.Screen.PAIR, ReviewLaunch.Screen.LIVE,
        ReviewLaunch.Screen.MEDIA, ReviewLaunch.Screen.PLAYBACK, ReviewLaunch.Screen.SETTINGS,
        ReviewLaunch.Screen.CHROME_EDIT,
    )

    fun decode(intent: Intent?): ReviewLaunch? = ReviewLaunch.decode { intent?.getStringExtra(it) }
        ?.takeIf { it.source == ReviewLaunch.Source.LOCAL && !it.connected && it.screen in screens }

    /**
     * Runs before the model builds its cameras store: every staged screen shows the two saved
     * cameras, idle, under it (the review `state` names a scripted pipeline, none is that page),
     * so no staged cell starts on the first-run pair wizard.
     */
    fun prepare(context: Context, review: ReviewLaunch) {
        // ReviewCameras' two saved cameras as the shipping app stored them; "" is that default.
        ReviewCameras.seed("", context.getSharedPreferences(AppModel.LEGACY_CAMERAS, Context.MODE_PRIVATE))
    }

    suspend fun start(context: Context, model: AppModel, review: ReviewLaunch) {
        when (review.screen) {
            ReviewLaunch.Screen.PAIR -> model.pairing = true
            // The staged monitor is the real session's control seam, idle: the live chrome renders
            // over it, and no radio, connection or command ever reaches a camera.
            ReviewLaunch.Screen.LIVE, ReviewLaunch.Screen.CHROME_EDIT -> {
                model.shell.present(ConnectedCamera(
                    SavedCamera(STAGED_CAMERA, DjiCameraSessionAdapter.INTEGRATION,
                        advertisedName = "SIMULATED common input", model = "SIMULATED"),
                    model.session.control))
                // The chrome editor over the same staged monitor (iOS `beginChromeEditing`).
                if (review.screen == ReviewLaunch.Screen.CHROME_EDIT) model.beginChromeEditing(model.currentDispMode)
            }
            ReviewLaunch.Screen.MEDIA, ReviewLaunch.Screen.PLAYBACK -> stageMedia(context, model, review)
            ReviewLaunch.Screen.SETTINGS -> {
                review.settingsTab?.let { model.operatorSettingsTab = it }
                model.homePanel = AppPanel.SETTINGS
            }
            else -> Unit // CAMERAS: the seeded saved cameras leave the shell on the cameras page.
        }
    }

    private suspend fun stageMedia(context: Context, model: AppModel, review: ReviewLaunch) {
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
        model.shell.open(ShellModel.Cover.MEDIA)
    }

    private fun file(clip: ReviewMediaSeed.Clip): MediaFile = MediaFile(
        path = "DCIM/SIMULATED/${clip.filename}", thumbPath = "DCIM/SIMULATED/${clip.filename}",
        sizeBytes = clip.byteCount, durationSeconds = clip.durationSeconds,
        resolution = "${clip.width}x${clip.height}", fps = clip.frameRate,
    )

    private const val STAGED_CAMERA = "simulated-common-input"
}
