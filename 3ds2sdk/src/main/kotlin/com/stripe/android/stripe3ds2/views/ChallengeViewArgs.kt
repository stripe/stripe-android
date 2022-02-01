package com.stripe.android.stripe3ds2.views

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutor
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChallengeViewArgs(
    internal val cresData: ChallengeResponseData,
    internal val creqData: ChallengeRequestData,
    internal val uiCustomization: StripeUiCustomization,
    internal val creqExecutorConfig: ChallengeRequestExecutor.Config,
    internal val creqExecutorFactory: ChallengeRequestExecutor.Factory,
    internal val timeoutMins: Int,
    internal val intentData: IntentData
) : Parcelable {
    internal val sdkTransactionId get() = creqData.sdkTransId

    fun toBundle() = bundleOf(EXTRA_ARGS to this)

    companion object {
        private const val EXTRA_ARGS = "extra_args"

        /**
         * Create a [ChallengeViewArgs] from Intent extras
         */
        fun create(extras: Bundle): ChallengeViewArgs {
            return requireNotNull(extras.getParcelable(EXTRA_ARGS))
        }
    }
}
