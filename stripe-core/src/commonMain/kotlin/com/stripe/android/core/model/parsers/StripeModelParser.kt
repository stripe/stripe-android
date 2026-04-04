package com.stripe.android.core.model.parsers

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel

/**
 * Shared parsing contract that operates on raw response strings instead of Android's
 * `org.json` types. Android can bridge legacy parsers through an adapter while new KMP-safe
 * parsers can implement this directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeModelParser<out ModelType : StripeModel> {
    fun parse(jsonString: String): ModelType?
}
