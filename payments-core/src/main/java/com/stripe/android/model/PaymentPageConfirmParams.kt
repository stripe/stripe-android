package com.stripe.android.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class PaymentPageConfirmParams(
    private val paymentMethodId: String,
    private val expectedPaymentMethodType: String?,
    private val expectedAmount: Long,
    private val clientAttributionMetadata: ClientAttributionMetadata,
    private val returnUrl: String,
    private val passiveCaptchaToken: String?,
) {
    fun asMap(): Map<String, Any?> = mapOf(
        "payment_method" to paymentMethodId,
        "expected_payment_method_type" to expectedPaymentMethodType,
        "expected_amount" to expectedAmount,
        "client_attribution_metadata" to clientAttributionMetadata.toParamMap(),
        "return_url" to returnUrl,
        "passive_captcha_token" to passiveCaptchaToken,
    )
}
