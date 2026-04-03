package com.stripe.android.core.utils

import androidx.annotation.RestrictTo

/**
 * URL-encode a string. This is useful for sanitizing untrusted data for use in URLs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
expect fun urlEncode(value: String): String
