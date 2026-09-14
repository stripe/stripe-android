package com.stripe.android.attestation

import android.os.Parcelable
import com.stripe.android.core.ApiConfiguration
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AttestationArgs(
    val apiConfiguration: ApiConfiguration.State,
    val productUsage: List<String>
) : Parcelable
