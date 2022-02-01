package com.stripe.android.stripe3ds2.transaction

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.UiType
import kotlinx.parcelize.Parcelize

/**
 * A sealed class representing the possible outcomes of a 3DS2 challenge.
 */
sealed class ChallengeResult : Parcelable {
    abstract val intentData: IntentData
    abstract val initialUiType: UiType?

    internal fun toBundle(): Bundle {
        return bundleOf(EXTRA_RESULT to this)
    }

    @Parcelize
    data class Succeeded(
        val uiTypeCode: String,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    @Parcelize
    data class Failed(
        val uiTypeCode: String,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    @Parcelize
    data class Canceled(
        val uiTypeCode: String?,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    @Parcelize
    data class ProtocolError(
        val data: ErrorData,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    @Parcelize
    data class RuntimeError(
        val throwable: Throwable,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    @Parcelize
    data class Timeout(
        val uiTypeCode: String?,
        override val initialUiType: UiType?,
        override val intentData: IntentData
    ) : ChallengeResult()

    companion object {
        private const val EXTRA_RESULT = "extra_result"

        fun fromIntent(intent: Intent?): ChallengeResult {
            return intent?.getParcelableExtra(EXTRA_RESULT) ?: RuntimeError(
                IllegalStateException("Intent extras did not contain a valid ChallengeResult."),
                null,
                IntentData.EMPTY
            )
        }
    }
}
