package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import java.net.URLEncoder

/**
 * Android actual uses the JDK form-encoding implementation so behavior stays
 * identical to the pre-KMP version.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
actual fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
