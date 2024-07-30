package com.stripe.android.stripe3ds2.transaction

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.stripe.android.stripe3ds2.views.ChallengeViewArgs
import kotlinx.parcelize.Parcelize

sealed class InitChallengeResult : Parcelable {
    @Parcelize
    data class Start(
        val challengeViewArgs: ChallengeViewArgs
    ) : InitChallengeResult()

    @Parcelize
    data class End(
        val challengeResult: ChallengeResult
    ) : InitChallengeResult()

    fun toBundle() = bundleOf(KEY_RESULT to this)

    companion object {
        private const val KEY_RESULT = "key_init_challenge_result"

        fun fromBundle(bundle: Bundle): InitChallengeResult {
            return BundleCompat.getParcelable(bundle, KEY_RESULT, InitChallengeResult::class.java) ?: End(
                ChallengeResult.RuntimeError(
                    IllegalArgumentException("Could not retrieve result."),
                    null,
                    IntentData.EMPTY
                )
            )
        }
    }
}
