package com.stripe.android.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * Internal copy of [com.stripe.android.AppInfo]. It's a public API object and can't be changed
 * without introducing backward incompatibility.
 * TODO(ccen): Move AppInfo to stripe-core and delete this copy during the next major version bump.
 *
 * Data for identifying your plug-in or library.
 *
 * See [Building Stripe Plug-ins and Libraries - Setting the API version](https://stripe.com/docs/building-plugins#setappinfo).
 *
 * @param name Name of your application (e.g. "MyAwesomeApp")
 * @param version Version of your application (e.g. "1.2.34")
 * @param url Website for your application (e.g. "https://myawesomeapp.info")
 * @param partnerId Your Stripe Partner ID (e.g. "pp_partner_1234")
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class InternalAppInfo internal constructor(
    private val name: String,
    private val version: String?,
    private val url: String?,
    private val partnerId: String?
) : Parcelable {

    fun toUserAgent(): String {
        return listOfNotNull(
            name,
            version?.let { "/$it" },
            url?.let { " ($it)" }
        ).joinToString("")
    }

    fun createClientHeaders(): Map<String, Map<String, String?>> {
        val appInfo = mapOf(
            "name" to name,
            "version" to version,
            "url" to url,
            "partner_id" to partnerId
        )

        return mapOf("application" to appInfo)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            name: String,
            version: String? = null,
            url: String? = null,
            partnerId: String? = null
        ): InternalAppInfo {
            return InternalAppInfo(name, version, url, partnerId)
        }
    }
}
