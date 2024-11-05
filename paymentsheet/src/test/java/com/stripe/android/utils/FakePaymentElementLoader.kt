package com.stripe.android.utils

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetLoadingException
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakePaymentElementLoader(
    private val stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
    private val shouldFail: Boolean = false,
    private var customer: CustomerState? = null,
    private var paymentSelection: PaymentSelection? = null,
    private val isGooglePayAvailable: Boolean = false,
    private val delay: Duration = Duration.ZERO,
    private val linkState: LinkState? = null,
    private val validationError: PaymentSheetLoadingException? = null,
    private val cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
) : PaymentElementLoader {

    fun updatePaymentMethods(paymentMethods: List<PaymentMethod>) {
        this.customer = customer?.copy(
            paymentMethods = paymentMethods
        )
        this.paymentSelection = paymentSelection.takeIf {
            (it !is PaymentSelection.Saved) || it.paymentMethod in paymentMethods
        }
    }

    override suspend fun load(
        initializationMode: PaymentSheet.InitializationMode,
        paymentSheetConfiguration: PaymentSheet.Configuration,
        isReloadingAfterProcessDeath: Boolean,
        initializedViaCompose: Boolean,
    ): Result<PaymentSheetState.Full> {
        delay(delay)
        return if (shouldFail) {
            Result.failure(IllegalStateException("oh no"))
        } else {
            Result.success(
                PaymentSheetState.Full(
                    config = paymentSheetConfiguration,
                    customer = customer,
                    linkState = linkState,
                    paymentSelection = paymentSelection,
                    validationError = validationError,
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                        stripeIntent = stripeIntent,
                        billingDetailsCollectionConfiguration = paymentSheetConfiguration
                            .billingDetailsCollectionConfiguration,
                        allowsDelayedPaymentMethods = paymentSheetConfiguration.allowsDelayedPaymentMethods,
                        allowsPaymentMethodsRequiringShippingAddress = paymentSheetConfiguration
                            .allowsPaymentMethodsRequiringShippingAddress,
                        isGooglePayReady = isGooglePayAvailable,
                        cbcEligibility = cbcEligibility,
                    ),
                )
            )
        }
    }
}
