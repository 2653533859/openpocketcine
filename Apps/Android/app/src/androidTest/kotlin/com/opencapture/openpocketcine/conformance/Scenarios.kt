package com.opencapture.openpocketcine.conformance

import com.opencapture.monitorui.MonitorCapabilities
import com.opencapture.opencine.appuitesting.ConformanceApp
import com.opencapture.opencine.appuitesting.ConformanceProfile
import com.opencapture.openpocketcine.MainActivity

/** SIMULATED A7 increment 4 scenarios only: the real launcher over the staged review hosts. */
class ShareRoundTrip : com.opencapture.opencine.appuitesting.ShareRoundTrip(PocketReview)

class ConfirmationCancel : com.opencapture.opencine.appuitesting.ConfirmationCancel(PocketReview)

class ChromeEditReturn : com.opencapture.opencine.appuitesting.ChromeEditReturn(PocketReview)

private object PocketReview : ConformanceApp {
    override val activity = MainActivity::class.java
    // A local review has no connected camera or optional camera controls.
    override val profile = ConformanceProfile("PocketCine", MonitorCapabilities())
}
