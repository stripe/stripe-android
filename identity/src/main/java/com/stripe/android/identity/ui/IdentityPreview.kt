package com.stripe.android.identity.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
internal fun IdentityPreview(
    content: @Composable () -> Unit
) {
    AdoptForStripeTheme(
        hostingAppColors = MaterialTheme.colors,
        hostingAppTypography = MaterialTheme.typography,
        hostingAppShapes = MaterialTheme.shapes,
        inspectionMode = LocalInspectionMode.current,
        content = content
    )
}
