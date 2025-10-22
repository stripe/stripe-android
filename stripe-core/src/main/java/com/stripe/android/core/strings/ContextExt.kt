@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.core.strings

import android.content.Context
import android.content.res.Resources
import android.icu.text.MessageFormat
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes

/**
 * Get a formatted string from resources using ICU MessageFormat on API 24+ or
 * standard Android format for older versions.
 *
 * String resources should use ICU format: "Hello {0}" instead of "Hello %s"
 *
 * @param resId the string resource identifier
 * @param formatArgs format arguments for the string
 * @return the formatted string
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.getStringIcu(
    @StringRes resId: Int,
    vararg formatArgs: Any?
): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // Use ICU MessageFormat for API 24+
        // Pattern: "Hello {0}" with args
        MessageFormat.format(getString(resId), *formatArgs)
    } else {
        // Fall back to standard Android format for older APIs
        // This branch handles legacy string resources with %s, %1$s, etc.
        // In production, all strings should use ICU format
        @Suppress("SpreadOperator")
        getString(resId, *formatArgs)
    }
}

/**
 * Get a formatted string from resources using ICU MessageFormat on API 24+ or
 * standard Android format for older versions.
 *
 * String resources should use ICU format: "Hello {0}" instead of "Hello %s"
 *
 * @param resId the string resource identifier
 * @param formatArgs format arguments for the string
 * @return the formatted string
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Resources.getStringIcu(
    @StringRes resId: Int,
    vararg formatArgs: Any?
): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // Use ICU MessageFormat for API 24+
        // Pattern: "Hello {0}" with args
        MessageFormat.format(getString(resId), *formatArgs)
    } else {
        // Fall back to standard Android format for older APIs
        // This branch handles legacy string resources with %s, %1$s, etc.
        // In production, all strings should use ICU format
        @Suppress("SpreadOperator")
        getString(resId, *formatArgs)
    }
}
