package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Displayable payment details for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class DisplayablePaymentDetails(
    @SerialName("default_card_brand")
    val defaultCardBrand: String? = null,
    @SerialName("default_payment_type")
    val defaultPaymentType: String? = null,
    @SerialName("last_4")
    val last4: String? = null,
    @SerialName("number_of_saved_payment_details")
    val numberOfSavedPaymentDetails: Long? = null,
) : StripeModel

/**
 * The result of a call to retrieve the [ConsumerSession] for a Link user.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsumerSessionLookup(
    @SerialName("exists")
    val exists: Boolean,
    @SerialName("consumer_session")
    val consumerSession: ConsumerSession? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
    @SerialName("publishable_key")
    val publishableKey: String? = null,
    @SerialName("displayable_payment_details")
    val displayablePaymentDetails: DisplayablePaymentDetails? = null,
    @SerialName("consent_ui")
    val consentUi: ConsentUi? = null,
) : StripeModel
