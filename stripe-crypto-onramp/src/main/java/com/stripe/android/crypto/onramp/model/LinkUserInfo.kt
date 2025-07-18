package com.stripe.android.crypto.onramp.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Data representing a Link user for registration.
 *
 * @property email User's email address.
 * @property phone User's phone number in E.164 format.
 * @property country User's ISO country code.
 * @property fullName Optional full name of the user.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Poko
class LinkUserInfo(
    val email: String,
    val phone: String,
    val country: String,
    val fullName: String?
) : Parcelable
