package com.stripe.android.stripe3ds2.views

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ChallengeProgressArgs(
    val directoryServerName: String,
    val accentColor: Int?,
    val sdkTransactionId: SdkTransactionId
) : Parcelable {
    fun toBundle() = bundleOf(EXTRA_ARGS to this)

    companion object {
        private const val EXTRA_ARGS = "com.stripe.android.stripe3ds2.views.ChallengeProgressArgs"

        fun fromBundle(bundle: Bundle?): ChallengeProgressArgs? {
            return bundle?.getParcelable(EXTRA_ARGS)
        }
    }
}
