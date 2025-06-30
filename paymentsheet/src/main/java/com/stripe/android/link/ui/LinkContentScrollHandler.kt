package com.stripe.android.link.ui

import androidx.compose.runtime.compositionLocalOf

internal val LocalLinkContentScrollHandler = compositionLocalOf<((Boolean) -> Unit)?> { null }
