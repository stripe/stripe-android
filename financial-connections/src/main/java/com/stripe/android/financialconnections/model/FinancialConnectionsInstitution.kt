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

    // TODO@carlosmuvi remove hardcoded images.
    // @SerialName(value = "icon")
    val icon: Image? = Image("https://pbs.twimg.com/profile_images/748885874516094980/ywt_aKRx_400x400.jpg"),

    // TODO@carlosmuvi remove hardcoded images.
    // @SerialName(value = "logo")
    val logo: Image? = Image("https://play-lh.googleusercontent.com/KFPSKyRk3oZQvTPR1BJ2nzGQZFcAfsNWUZ-MQQM_ixEbxs4_-MYHo4cVQOlDU8lrG3BE"),

    @SerialName(value = "featured_order") val featuredOrder: Int? = null,

    @SerialName(value = "url") val url: String? = null

) : Parcelable

@Serializable
@Parcelize
internal data class Image(

    @SerialName(value = "default") val default: String

) : Parcelable
