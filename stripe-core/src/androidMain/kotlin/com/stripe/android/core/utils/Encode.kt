package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import java.net.URLEncoder

/**
 * URL-encode a string. This is useful for sanitizing untrusted data for use in URLs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
