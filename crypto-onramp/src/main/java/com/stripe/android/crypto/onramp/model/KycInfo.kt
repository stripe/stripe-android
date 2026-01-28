package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Represents the full list of KYC information to be collected.
 *
 * @property firstName The user’s given name as it appears on official documents.
 * @property lastName The user’s family name as it appears on official documents.
 * @property idNumber The full identification number.
 * @property dateOfBirth The user’s date of birth.
 * @property address The user’s billing address.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class KycInfo(
    val firstName: String,
    val lastName: String,
    val idNumber: String?,
    val dateOfBirth: DateOfBirth,
    val address: PaymentSheet.Address
)

/**
 * Represents a set of KYC information used when refreshing or revalidating
 * an existing user’s identity.
 *
 * @property firstName The user’s given name.
 * @property lastName The user’s family name.
 * @property idNumberLastFour The last four digits of the user’s identification number.
 * @property idType The type of government-issued identification.
 * Currently only social_security_number is accepted.
 * @property dateOfBirth The user’s date of birth.
 * @property address The user’s billing address.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Poko
internal class RefreshKycInfo(
    val firstName: String,
    val lastName: String,
    val idNumberLastFour: String?,
    val idType: String?,
    val dateOfBirth: DateOfBirth,
    val address: PaymentSheet.Address
) : Parcelable
