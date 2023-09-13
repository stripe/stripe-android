package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
enum class TranslationId(val resourceId: Int) {
    // https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/master/src/lib/inner/messages/en.json
    @SerialName("upe.labels.ideal.bank")
    IdealBank(R.string.stripe_ideal_bank),

    @SerialName("upe.labels.p24.bank")
    P24Bank(R.string.stripe_p24_bank),

    @SerialName("upe.labels.eps.bank")
    EpsBank(R.string.stripe_eps_bank),

    @SerialName("upe.labels.fpx.bank")
    FpxBank(R.string.stripe_fpx_bank),

    @SerialName("address.label.name")
    AddressName(CoreR.string.stripe_address_label_full_name),

    @SerialName("upe.labels.name.onAccount")
    AuBecsAccountName(StripeR.string.stripe_au_becs_account_name)
}
