package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.KycRetrieveResponseProtocol
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal API request model for KYC refreshing.
 * This represents the exact structure expected by the Stripe API.
 */
@Serializable
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class KycRetrieveResponse(
    @SerialName("first_name")
    override val firstName: String,
    @SerialName("last_name")
    override val lastName: String,
    @SerialName("id_number_last4")
    override val idNumberLastFour: String?,
    @SerialName("id_type")
    override val idType: String?,
    @SerialName("dob")
    @Serializable(with = DateOfBirthSerializer::class)
    override val dateOfBirth: DateOfBirth,

    @SerialName("address")
    @Serializable(with = PaymentSheetAddressSerializer::class)
    override val address: PaymentSheet.Address
) : Parcelable, KycRetrieveResponseProtocol
