package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Networks(
    /**
     * The customer’s preferred card network for co-branded cards. Supports cartes_bancaires, mastercard, or visa.
     * Selection of a network that does not apply to the card will be stored as invalid_preference on the card.
     *
     * [card.networks.preferred](https://docs.stripe.com/api/tokens/create_card#create_card_token-card-networks-preferred)
     *
     * Optional
     *
     */
    val preferred: String,
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_PREFERRED to preferred,
        )
    }

    private companion object {
        private const val PARAM_PREFERRED = "preferred"
    }
}
