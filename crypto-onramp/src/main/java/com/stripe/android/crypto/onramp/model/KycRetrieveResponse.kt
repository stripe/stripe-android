package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal API request model for KYC refreshing.
 * This represents the exact structure expected by the Stripe API.
 */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycRetrieveResponse(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("id_number_last4")
    val idNumberLastFour: String?,
    @SerialName("id_type")
    val idType: String?,
    @SerialName("dob")
    val dateOfBirth: DateOfBirth,

    @SerialName("address")
    @Serializable(with = PaymentSheetAddressSerializer::class)
    val address: PaymentSheet.Address
)
