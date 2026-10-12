package com.opencapture.openpocketcine

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opencapture.monitorui.MonitorRect

val ChromeShape = RoundedCornerShape(LiveDesign.CORNER_RADIUS_DP.dp)

fun Modifier.liveModuleFrame(rect: MonitorRect): Modifier =
    offset(rect.x.dp, rect.y.dp).size(rect.width.dp, rect.height.dp)
