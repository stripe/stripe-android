package com.stripe.android.crypto.onramp.model

import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnrampCollectPaymentMethodCallback {
    fun onResult(result: OnrampCollectPaymentMethodResult)
}

/**
 * Result of selecting a payment type in Onramp.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class OnrampCollectPaymentMethodResult {
    /**
     * The user has selected a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Completed internal constructor(val displayData: PaymentMethodDisplayData) : OnrampCollectPaymentMethodResult()

    /**
     * The user declined to select a payment option.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Cancelled internal constructor() : OnrampCollectPaymentMethodResult()

    /**
     * Selecting a payment option failed due to an error.
     * @param error The error that caused the failure.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Failed internal constructor(val error: Throwable) : OnrampCollectPaymentMethodResult()
}

@Poko
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentMethodDisplayData internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val imageLoader: suspend () -> Drawable,

    /**
     * User facing strings representing payment method information
     */
    val label: String,

    val sublabel: String?
) {
    val icon: Drawable = DelegateDrawable(imageLoader = imageLoader)

    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(icon)
}
