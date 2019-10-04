package com.stripe.android

import java.util.Objects
import org.json.JSONObject

/**
 * Data for identifying your plug-in or library.
 *
 * See [
 * Building Stripe Plug-ins and Libraries - Setting the API version](https://stripe.com/docs/building-plugins#setappinfo).
 *
 * @param name Name of your application (e.g. "MyAwesomeApp")
 * @param version Version of your application (e.g. "1.2.34")
 * @param url Website for your application (e.g. "https://myawesomeapp.info")
 * @param partnerId Your Stripe Partner ID (e.g. "pp_partner_1234")
 */
class AppInfo private constructor(
    private val name: String,
    private val version: String?,
    private val url: String?,
    private val partnerId: String?
) {

    internal fun toUserAgent(): String {
        return listOfNotNull(
            name,
            version?.let { "/$it" },
            url?.let { " ($it)" }
        ).joinToString("")
    }

    internal fun createClientHeaders(): Map<String, String> {
        val appInfo = mapOf(
            "name" to name,
            "version" to version,
            "url" to url,
            "partner_id" to partnerId
        )

        return mapOf("application" to JSONObject(appInfo).toString())
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is AppInfo -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(appInfo: AppInfo): Boolean {
        return name == appInfo.name &&
            version == appInfo.version &&
            url == appInfo.url &&
            partnerId == appInfo.partnerId
    }

    override fun hashCode(): Int {
        return Objects.hash(name, version, url, partnerId)
    }

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
