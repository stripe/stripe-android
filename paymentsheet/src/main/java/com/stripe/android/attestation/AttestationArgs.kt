package com.stripe.android.attestation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AttestationArgs(
    val publishableKey: String,
    val productUsage: List<String>
) : Parcelable
