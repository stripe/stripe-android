package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import kotlinx.parcelize.Parcelize

sealed class ChallengeRequestResult : Parcelable {
    @Parcelize
    data class Success(
        val creqData: ChallengeRequestData,
        val cresData: ChallengeResponseData,
        internal val creqExecutorConfig: ChallengeRequestExecutor.Config
    ) : ChallengeRequestResult()

    sealed class Failure : ChallengeRequestResult()

    @Parcelize
    data class ProtocolError(
        val data: ErrorData
    ) : Failure()

    @Parcelize
    data class RuntimeError(
        val throwable: Throwable
    ) : Failure()

    @Parcelize
    data class Timeout(
        val data: ErrorData
    ) : Failure()
}
