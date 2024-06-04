package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param featured
 * @param id
 * @param mobileHandoffCapable
 * @param name
 * @param featuredOrder
 * @param url
 */
@Serializable
@Parcelize
internal data class FinancialConnectionsInstitution(

    @SerialName(value = "featured") val featured: Boolean,

    @SerialName(value = "id") val id: String,

    @SerialName(value = "mobile_handoff_capable") val mobileHandoffCapable: Boolean,

    @SerialName(value = "name") val name: String,

    @SerialName(value = "icon")
    val icon: Image? = null,

    @SerialName(value = "logo")
    val logo: Image? = null,

    @SerialName(value = "featured_order") val featuredOrder: Int? = null,

    @SerialName(value = "url") val url: String? = null

) : Parcelable, java.io.Serializable {

    /**
     * This returns a cleaned up url containing only the root domain.
     * Works for up to two-part tlds, but falls apart for longer ones e.g. hello.al.sp.gov.br
     * Once the URLs are cleaned up on the backend, won't need this anymore.
     */
    val formattedUrl: String
        get() = runCatching {
            // This match would still have subdomains
            val matchResult = Regex("""^(?:https?://)?(?:www\.|[^@\n]+@)?([^:/\n]+)""")
                .find(url ?: return "")
            val rootUrl = matchResult?.groups?.get(1)?.value ?: return ""
            val parts = rootUrl.split('.')
            val len = parts.size
            return if (len > 2 && parts[len - 2].length <= 3 && parts[len - 1].length <= 2) {
                "${parts[len - 3]}.${parts[len - 2]}.${parts[len - 1]}"
            } else {
                "${parts[len - 2]}.${parts[len - 1]}"
            }
        }.getOrDefault(url ?: "")
}
