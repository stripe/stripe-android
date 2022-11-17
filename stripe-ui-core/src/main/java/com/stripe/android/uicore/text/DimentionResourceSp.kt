package com.stripe.android.uicore.text

import androidx.annotation.DimenRes
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.TextUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@ReadOnlyComposable
fun dimensionResourceSp(@DimenRes id: Int): TextUnit {
    with(LocalDensity.current) {
        return dimensionResource(id = id).toSp()
    }
}
