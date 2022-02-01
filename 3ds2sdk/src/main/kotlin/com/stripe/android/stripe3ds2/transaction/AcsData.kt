package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.security.interfaces.ECPublicKey

@Parcelize
internal data class AcsData constructor(
    val acsUrl: String,
    val acsEphemPubKey: ECPublicKey,
    val sdkEphemPubKey: ECPublicKey
) : Parcelable
