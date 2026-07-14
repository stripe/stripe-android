package com.stripe.android.checkout

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.stripe.android.checkout.ece.ExpressCheckoutElementContent
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressCheckoutElement internal constructor(
    private val state: StateFlow<ResolvedState?>,
    private val content: @Composable (ResolvedState) -> Unit,
) {

    @Composable
    fun Content() {
        val resolvedState by state.collectAsState()
        resolvedState?.let { content(it) }
    }

    internal class Factory @Inject constructor(
        private val interactorFactory: ExpressCheckoutElementInteractor.ExpressCheckoutElementInteractorFactory,
    ) {
        fun create(
            controllerState: StateFlow<CheckoutControllerState?>,
            confirmationState: StateFlow<CheckoutConfirmationStateHolder.State?>,
        ): ExpressCheckoutElement {
            return ExpressCheckoutElement(
                state = combineAsStateFlow(controllerState, confirmationState) { checkoutState, confirmationStateValue ->
                    val expressConfiguration = checkoutState?.configuration?.expressCheckoutElementConfiguration
                        ?: return@combineAsStateFlow null
                    val confirmationStateData = confirmationStateValue ?: return@combineAsStateFlow null

                    ResolvedState(
                        commonConfiguration = confirmationStateData.configuration.asCommonConfiguration(),
                        configuration = expressConfiguration,
                        paymentMethodMetadata = confirmationStateData.paymentMethodMetadata,
                    )
                },
            ) { resolvedState ->
                val interactor = remember(resolvedState) {
                    interactorFactory.create(
                        commonConfiguration = resolvedState.commonConfiguration,
                        configuration = resolvedState.configuration,
                        paymentMethodMetadata = resolvedState.paymentMethodMetadata,
                    )
                }
                ExpressCheckoutElementContent(interactor = interactor)
            }
        }
    }

    @Poko
    internal class ResolvedState(
        val commonConfiguration: CommonConfiguration,
        val configuration: Configuration.State,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

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
