package com.stripe.hcaptcha

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HCaptchaStateListener(
    val onOpen: () -> Unit,
    val onSuccess: (token: String) -> Unit,
    val onFailure: (exception: HCaptchaException) -> Unit
) : Parcelable
