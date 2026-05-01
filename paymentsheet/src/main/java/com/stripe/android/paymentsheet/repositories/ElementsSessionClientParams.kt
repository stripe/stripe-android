package com.stripe.android.paymentsheet.repositories

import java.util.Locale

internal class ElementsSessionClientParams(
    val mobileAppId: String,
    private val mobileSessionIdProvider: () -> String,
) {
    val locale: String get() = Locale.getDefault().toLanguageTag()
    val mobileSessionId: String get() = mobileSessionIdProvider()

    /**
     * For checkout session init — values to nest under `elements_session_client`.
     */
    fun toCheckoutSessionMap(): Map<String, String> = mapOf(
        "is_aggregation_expected" to "true",
        "locale" to locale,
        "mobile_session_id" to mobileSessionId,
        "mobile_app_id" to mobileAppId,
    )
}
