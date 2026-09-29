package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Represents the full list of KYC information to be collected.
 *
 * Constructors that omit [idType] use [IdType.SocialSecurityNumber].
 *
 * @property firstName The user’s given name as it appears on official documents.
 * @property lastName The user’s family name as it appears on official documents.
 * @property idNumber The full identification number.
 * @property idType The type of identification provided by the user.
 * @property dateOfBirth The user’s date of birth.
 * @property address The user’s billing address.
 * @property birthCountry The country where the user was born.
 * @property birthCity The city where the user was born.
 * @property nationalities The user's nationalities.
 */
@ExperimentalCryptoOnramp
@Poko
class KycInfo @JvmOverloads constructor(
    val firstName: String?,
    val lastName: String?,
    val idNumber: String?,
    val idType: IdType,
    val dateOfBirth: DateOfBirth?,
    val address: PaymentSheet.Address?,
    val birthCountry: CountryCode? = null,
    val birthCity: String? = null,
    val nationalities: List<CountryCode>? = null
) {
    constructor(
        firstName: String?,
        lastName: String?,
        idNumber: String?,
        dateOfBirth: DateOfBirth?,
        address: PaymentSheet.Address?,
        birthCountry: CountryCode? = null,
        birthCity: String? = null,
        nationalities: List<CountryCode>? = null
    ) : this(
        firstName = firstName,
        lastName = lastName,
        idNumber = idNumber,
        idType = IdType.SocialSecurityNumber,
        dateOfBirth = dateOfBirth,
        address = address,
        birthCountry = birthCountry,
        birthCity = birthCity,
        nationalities = nationalities
    )

    constructor(
        firstName: String?,
        lastName: String?,
        idNumber: String?,
        dateOfBirth: DateOfBirth?,
        address: PaymentSheet.Address?
    ) : this(
        firstName = firstName,
        lastName = lastName,
        idNumber = idNumber,
        idType = IdType.SocialSecurityNumber,
        dateOfBirth = dateOfBirth,
        address = address,
        birthCountry = null,
        birthCity = null,
        nationalities = null
    )
}

/**
 * Represents a set of KYC information used when refreshing or revalidating
 * an existing user’s identity.
 *
 * @property firstName The user’s given name.
 * @property lastName The user’s family name.
 * @property idNumberLastFour The last four digits of the user’s identification number.
 * @property idType The API value for the type of government-issued identification.
 * @property dateOfBirth The user’s date of birth.
 * @property address The user’s billing address.
 */
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
