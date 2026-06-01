package com.stripe.link.core.data.network

/**
 * Constructs [ApiRequest] instances for each API endpoint.
 *
 * @param requestSurface identifies the calling platform (e.g. "android", "ios").
 */
class ApiRequestFactory(
    private val requestSurface: String,
) {

    fun emailClick(ref: String, eid: String, link: String): ApiRequest {
        return ApiRequest(
            url = "https://app.link.com/api/email_click",
            params = mapOf(
                "ref" to ref,
                "eid" to eid,
                "link" to link,
                "request_surface" to requestSurface,
            )
        )
    }

    private fun buildAddressParams(
        line1: String?,
        line2: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        country: String?,
    ): Map<String, String> = buildMap {
        line1?.let { put("line1", it) }
        line2?.let { put("line2", it) }
        city?.let { put("city", it) }
        state?.let { put("state", it) }
        postalCode?.let { put("postal_code", it) }
        country?.let { put("country", it) }
    }
}
