package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.utils.ObjectUtils
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import kotlin.coroutines.CoroutineContext

interface ChallengeRequestExecutor {
    suspend fun execute(creqData: ChallengeRequestData): ChallengeRequestResult

    fun interface Factory : Serializable {
        fun create(
            errorReporter: ErrorReporter,
            workContext: CoroutineContext
        ): ChallengeRequestExecutor
    }

    @Parcelize
    data class Config(
        internal val messageTransformer: MessageTransformer,
        internal val sdkReferenceId: String,
        internal val creqData: ChallengeRequestData,
        internal val acsUrl: String,
        internal val keys: Keys
    ) : Serializable, Parcelable {

        /**
         * Override `equals` and `hashCode` to support equality checks for [ByteArray].
         */
        @Parcelize
        data class Keys(
            internal val sdkPrivateKeyEncoded: ByteArray,
            internal val acsPublicKeyEncoded: ByteArray,
        ) : Serializable, Parcelable {
            override fun hashCode(): Int {
                return ObjectUtils.hash(sdkPrivateKeyEncoded, acsPublicKeyEncoded)
            }

            override fun equals(other: Any?): Boolean {
                return when {
                    this === other -> true
                    other is Keys -> typedEquals(other)
                    else -> false
                }
            }

            private fun typedEquals(keys: Keys): Boolean {
                return sdkPrivateKeyEncoded.contentEquals(keys.sdkPrivateKeyEncoded) &&
                    acsPublicKeyEncoded.contentEquals(keys.acsPublicKeyEncoded)
            }
        }
    }
}
