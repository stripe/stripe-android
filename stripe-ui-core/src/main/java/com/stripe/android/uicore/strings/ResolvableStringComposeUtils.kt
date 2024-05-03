@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.strings

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.core.strings.ResolvableString

/**
 * Resolves a string value from a [ResolvableString] instance.
 *
 * @return a string value
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@ReadOnlyComposable
fun ResolvableString.resolve(): String {
    return resolve(LocalContext.current)
}
