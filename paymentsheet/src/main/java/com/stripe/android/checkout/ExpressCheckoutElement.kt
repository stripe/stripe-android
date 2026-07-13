package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement internal constructor(
    private val interactor: ExpressCheckoutElementInteractor,
) {

    @Composable
    fun Content() {
        ExpressCheckoutElementContent(interactor = interactor)
    }

    internal class Factory @Inject constructor(
        private val interactorFactory: ExpressCheckoutElementInteractor.ExpressCheckoutElementInteractorFactory,
    ) {
        fun create(
        configuration: Configuration.State,
        paymentMethodMetadata: PaymentMethodMetadata,
        commonConfiguration: CommonConfiguration,
    ): ExpressCheckoutElement {
        val interactor = interactorFactory.create(
            configuration = configuration,
            paymentMethodMetadata = paymentMethodMetadata,
            commonConfiguration = commonConfiguration,
        )
        return ExpressCheckoutElement(
            interactor = interactor,
        )
    }
    }

    enum class ExpressButtonVisibility {
        Automatic,
        Never,
    }

    enum class ExpressButton {
        Link,
        GooglePay,
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var visibility: Map<ExpressButton, ExpressButtonVisibility> = emptyMap()
        private var googlePayButtonType: PaymentSheet.GooglePayConfiguration.ButtonType? = null

        fun visibility(
            visibility: Map<ExpressButton, ExpressButtonVisibility>
        ): Configuration = apply {
            this.visibility = visibility
        }

        fun googlePayButtonType(
            googlePayButtonType: PaymentSheet.GooglePayConfiguration.ButtonType?
        ) {
            this.googlePayButtonType = googlePayButtonType
        }

        @Parcelize
        internal data class State(
            val visibility: Map<ExpressButton, ExpressButtonVisibility>,
            val googlePayButtonType: PaymentSheet.GooglePayConfiguration.ButtonType?,
        ) : Parcelable

        internal fun build(): State = State(
            visibility = visibility,
            googlePayButtonType = googlePayButtonType,
        )
    }
}
