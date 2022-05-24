package com.stripe.android.ui.core.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StringRepository @Inject constructor(
    val resources: Resources
) {
    fun get(translationId: TranslationId) =
        resources.getString(translationId.resourceId)

    @Serializable
    enum class TranslationId(val resourceId: Int) {
        // https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/master/src/lib/inner/messages/en.json
        @SerialName("upe.labels.ideal.bank")
        IdealBank(R.string.ideal_bank),

        @SerialName("upe.labels.p24.bank")
        P24Bank(R.string.p24_bank),

        @SerialName("upe.labels.eps.bank")
        EpsBank(R.string.eps_bank),

        @SerialName("address.label.name")
        AddressName(R.string.address_label_name),

        @SerialName("upe.labels.name.onAccount")
        AuBecsAccountName(R.string.au_becs_account_name),

        @SerialName("upe.labels.sepa_debit.billing")
        SepaDebitBilling(R.string.billing_details),
    }
}
