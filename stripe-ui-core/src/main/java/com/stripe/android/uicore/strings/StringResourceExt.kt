@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.strings

import android.icu.text.MessageFormat
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Load a string resource using ICU MessageFormat on API 24+ or standard Android format
 * for older versions.
 *
 * String resources should use ICU format: "Hello {0}" instead of "Hello %s"
 *
 * @param id the resource identifier
 * @param formatArgs optional format arguments for the string
 * @return the string data associated with the resource
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@ReadOnlyComposable
fun stringResourceIcu(
    @StringRes id: Int,
    vararg formatArgs: Any
): String {
    val context = LocalContext.current
    val rawString = context.getString(id)

    return if (formatArgs.isEmpty()) {
        rawString
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use ICU MessageFormat for API 24+
            // Pattern: "Hello {0}" with args
            MessageFormat.format(rawString, *formatArgs)
        } else {
            // Fall back to standard Android format for older APIs
            // This branch handles legacy string resources with %s, %1$s, etc.
            // In production, all strings should use ICU format
            @Suppress("SpreadOperator")
            context.getString(id, *formatArgs)
        }
    }
}
