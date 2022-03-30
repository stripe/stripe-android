package com.stripe.android.connections.model

import android.os.Parcelable
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
    val paymentAccount: PaymentAccount? = null,

    @SerialName("return_url")
    val returnUrl: String? = null
) : StripeModel, Parcelable

@Serializable(with = PaymentAccountSerializer::class)
sealed class PaymentAccount : Parcelable
