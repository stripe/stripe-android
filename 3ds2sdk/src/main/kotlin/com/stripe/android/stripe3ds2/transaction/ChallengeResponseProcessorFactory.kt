package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.MessageTransformer
import javax.crypto.SecretKey

internal fun interface ChallengeResponseProcessorFactory {
    fun create(secretKey: SecretKey): ChallengeResponseProcessor

    class Default(
        private val messageTransformer: MessageTransformer,
        private val errorReporter: ErrorReporter,
        private val creqExecutorConfig: ChallengeRequestExecutor.Config
    ) : ChallengeResponseProcessorFactory {
        override fun create(secretKey: SecretKey) = ChallengeResponseProcessor.Default(
            messageTransformer,
            secretKey,
            errorReporter,
            creqExecutorConfig
        )
    }
}
