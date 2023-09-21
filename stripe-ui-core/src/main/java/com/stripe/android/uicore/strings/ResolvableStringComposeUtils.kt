package com.stripe.android.uicore.strings

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@ReadOnlyComposable
fun resolvableStringResource(resolvableString: ResolvableString): String {
    return resolvableString.resolve(LocalContext.current)
}
