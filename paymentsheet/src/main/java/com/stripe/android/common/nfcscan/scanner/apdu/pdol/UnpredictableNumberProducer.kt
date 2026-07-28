package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import java.security.SecureRandom

internal object UnpredictableNumberProducer : TagValueProducer {
    private val secureRandom = SecureRandom()

    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_UNPREDICTABLE_NUMBER,
            value = createUnpredictableNumber(),
        )
    }

    private fun createUnpredictableNumber(): ByteArray {
        return ByteArray(UNPREDICTABLE_NUMBER_LENGTH).also {
            secureRandom.nextBytes(it)
        }
    }

    private const val TAG_UNPREDICTABLE_NUMBER = "9F37"
    private const val UNPREDICTABLE_NUMBER_LENGTH = 4
}
