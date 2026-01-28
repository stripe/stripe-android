package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Data representing a Link user for registration.
 *
 * @property email The user's email to be used for signup.
 * @property phone The phone number of the user. Phone number must be in E.164 format (e.g., +12125551234), otherwise
 *   an error will be thrown.
 * @property country The two-letter country code of the user (ISO 3166-1 alpha-2).
 * @property fullName The full name of the user. A name should be collected if the user is located outside of the US,
 *   otherwise it is optional.
 */
@Parcelize
@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkUserInfo(
    val email: String,
    val fullName: String?,
    val phone: String,
    val country: String,
) : Parcelable
