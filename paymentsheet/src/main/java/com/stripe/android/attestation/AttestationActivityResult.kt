package com.stripe.android.attestation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface AttestationActivityResult : Parcelable {
    @Parcelize
    data class Success(val token: String) : AttestationActivityResult

    @Parcelize
    data object Failed : AttestationActivityResult

    @Parcelize
    data object NoResult : AttestationActivityResult
}
