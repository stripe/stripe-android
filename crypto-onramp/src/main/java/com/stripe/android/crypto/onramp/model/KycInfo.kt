package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class KycInfo(
    val firstName: String,
    val lastName: String,
    val idNumber: String?,
    val dateOfBirth: DateOfBirth,
    val address: PaymentSheet.Address
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class RefreshKycInfo(
    val firstName: String,
    val lastName: String,
    val idNumberLastFour: String?,
    val idType: String?,
    val dateOfBirth: DateOfBirth,
    val address: PaymentSheet.Address
) : Parcelable
