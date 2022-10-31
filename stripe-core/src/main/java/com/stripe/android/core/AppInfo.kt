package com.stripe.android.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data for identifying your plug-in or library.
 *
 * See [Setting the API version](https://stripe.com/docs/building-plugins#setappinfo).
 *
 * @param name Name of your application (e.g. "MyAwesomeApp")
 * @param version Version of your application (e.g. "1.2.34")
 * @param url Website for your application (e.g. "https://myawesomeapp.info")
 * @param partnerId Your Stripe Partner ID (e.g. "pp_partner_1234")
 */
@Parcelize
data class AppInfo internal constructor(
    private val name: String,
    private val version: String?,
    private val url: String?,
    private val partnerId: String?
) : Parcelable {

    internal fun toUserAgent(): String {
        return listOfNotNull(
            name,
            version?.let { "/$it" },
            url?.let { " ($it)" }
        ).joinToString("")
    }

    internal fun createClientHeaders(): Map<String, Map<String, String?>> {
        return mapOf("application" to toParamMap())
    }

    internal fun toParamMap() = mapOf(
        "name" to name,
        "version" to version,
        "url" to url,
        "partner_id" to partnerId
    )

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            name: String,
            version: String? = null,
            url: String? = null,
            partnerId: String? = null
        ): AppInfo {
            return AppInfo(name, version, url, partnerId)
        }
    }
}
