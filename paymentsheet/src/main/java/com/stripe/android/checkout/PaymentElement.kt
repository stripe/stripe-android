package com.stripe.android.checkout

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.utils.collectAsState
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentElement @Inject internal constructor(
    private val contentHelper: EmbeddedContentHelper,
) {

    @Composable
    fun PaymentOptionsContent() {
        val embeddedContent by contentHelper.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    fun presentPaymentOptions() {
        contentHelper.presentPaymentOptions()
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var embeddedViewDisplaysMandateText: Boolean = true
        private var billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            BillingDetailsCollectionConfiguration()

        fun embeddedViewDisplaysMandateText(
            embeddedViewDisplaysMandateText: Boolean
        ): Configuration = apply {
            this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
        }

        fun billingDetailsCollectionConfiguration(
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
        ): Configuration = apply {
            this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
        }

        @Parcelize
        internal data class State(
            val embeddedViewDisplaysMandateText: Boolean,
            val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration.State,
        ) : Parcelable

        internal fun build(): State = State(
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.build(),
        )
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
}
