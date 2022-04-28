package com.stripe.android.financialconnections.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.financialconnections.model.serializer.JsonAsStringSerializer
import com.stripe.android.financialconnections.model.serializer.PaymentAccountSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param clientSecret
 * @param id
 * @param accounts
 * @param livemode
 * @param paymentAccount
 * @param returnUrl
 */
@Parcelize
@Serializable
data class FinancialConnectionsSession internal constructor(
    @SerialName("client_secret")
    val clientSecret: String,

    @SerialName("id")
    val id: String,

    @SerialName("linked_accounts")
    val accounts: FinancialConnectionsAccountList,

    @SerialName("livemode")
    val livemode: Boolean,

    @SerialName("payment_account")
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val paymentAccount: PaymentAccount? = null,

    @SerialName("return_url")
    val returnUrl: String? = null,

    @SerialName("bank_account_token")
    @Serializable(with = JsonAsStringSerializer::class)
    internal val bankAccountToken: String? = null
) : StripeModel, Parcelable

@Serializable(with = PaymentAccountSerializer::class)
sealed class PaymentAccount : Parcelable
