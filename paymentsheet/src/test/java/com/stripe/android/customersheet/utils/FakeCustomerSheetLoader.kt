package com.stripe.android.customersheet.utils

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetState
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration

@OptIn(ExperimentalCustomerSheetApi::class)
internal class FakeCustomerSheetLoader(
    private val stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
    private val shouldFail: Boolean = false,
    private val customerPaymentMethods: List<PaymentMethod> = emptyList(),
    private val supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod> = listOf(
        LpmRepository.HardcodedCard,
        LpmRepository.hardCodedUsBankAccount,
    ),
    private val paymentSelection: PaymentSelection? = null,
    private val isGooglePayAvailable: Boolean = false,
    private val delay: Duration = Duration.ZERO,
    private val cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
) : CustomerSheetLoader {

    override suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState.Full> {
        delay(delay)
        return if (shouldFail) {
            Result.failure(IllegalStateException("failed to load"))
        } else {
            Result.success(
                CustomerSheetState.Full(
                    config = configuration,
                    stripeIntent = stripeIntent,
                    supportedPaymentMethods = supportedPaymentMethods,
                    customerPaymentMethods = customerPaymentMethods,
                    isGooglePayReady = isGooglePayAvailable,
                    paymentSelection = paymentSelection,
                    cbcEligibility = cbcEligibility,
                )
            )
        }
    }
}
