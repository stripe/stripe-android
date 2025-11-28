package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
@Parcelize
class CheckoutSessionResponse(
    val currency: String,
    val amount: Long,
    val mode: Mode,
    val setupFutureUsage: StripeIntent.Usage?,
    val captureMethod: PaymentIntent.CaptureMethod,
    val paymentMethodOptionsJsonString: String?,
    val paymentMethodTypes: List<String>,
    val onBehalfOf: String?,
    val intent: StripeIntent?,
    val paymentMethods: List<PaymentMethod>?,
) : StripeModel {
    enum class Mode {
        Payment, Subscription,
    }
}
