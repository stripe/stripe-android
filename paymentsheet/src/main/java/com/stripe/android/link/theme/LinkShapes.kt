package com.stripe.android.link.theme

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class LinkShapes(
    val extraSmall: RoundedCornerShape = RoundedCornerShape(4.dp),
    val default: RoundedCornerShape = RoundedCornerShape(12.dp),
    val primaryButton: RoundedCornerShape = RoundedCornerShape(12.dp),
    val primaryButtonHeight: Dp = 56.dp,
)
