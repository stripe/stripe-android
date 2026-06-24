package com.stripe.android.checkout

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement internal constructor(
    private val controller: CheckoutController,
) {

    @Composable
    fun PaymentOptionsContent() {
        // TODO: Render payment method selection UI.
    }

    fun presentPaymentOptions() {
        // TODO: Launch payment options selection sheet.
    }

    fun clearPaymentOption() {
        controller.updateSelection(null)
    }

    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class PaymentOptionDisplayData internal constructor(
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val imageLoader: suspend () -> Drawable,
        val label: String,
        val billingDetails: PaymentSheet.BillingDetails?,
        val paymentMethodType: String,
        val mandateText: AnnotatedString?,
    ) {
        private val iconDrawable: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(iconDrawable)
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
        )
    }
}
