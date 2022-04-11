package com.stripe.android.connections.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.connections.model.serializer.JsonAsStringSerializer
import com.stripe.android.connections.model.serializer.PaymentAccountSerializer
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param clientSecret
 * @param id
 * @param linkedAccounts
 * @param livemode
 * @param paymentAccount
 * @param returnUrl
 */
@Parcelize
@Serializable
data class LinkAccountSession internal constructor(
    @SerialName("client_secret")
    val clientSecret: String,

    @SerialName("id")
    val id: String,

    @SerialName("linked_accounts")
    val linkedAccounts: LinkedAccountList,

    @SerialName("livemode")
    val livemode: Boolean,

    @SerialName("payment_account")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val paymentAccount: PaymentAccount? = null,

    @SerialName("return_url")
    val returnUrl: String? = null,

    @SerialName("token")
    @Serializable(with = JsonAsStringSerializer::class)
    internal val token: String? = null
) : StripeModel, Parcelable

@Serializable(with = PaymentAccountSerializer::class)
sealed class PaymentAccount : Parcelable
