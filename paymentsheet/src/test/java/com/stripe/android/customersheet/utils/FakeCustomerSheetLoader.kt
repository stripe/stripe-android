package com.stripe.android.customersheet.utils

import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetState
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import kotlinx.coroutines.delay
import kotlin.time.Duration

internal class FakeCustomerSheetLoader(
    private val stripeIntent: StripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
    private val shouldFail: Boolean = false,
    private val customerPaymentMethods: List<PaymentMethod> = emptyList(),
    private val supportedPaymentMethods: List<SupportedPaymentMethod> = listOf(
        LpmRepositoryTestHelpers.card,
        LpmRepositoryTestHelpers.usBankAccount,
    ),
    private val paymentSelection: PaymentSelection? = null,
    private val isGooglePayAvailable: Boolean = false,
    private val delay: Duration = Duration.ZERO,
    private val cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
    private val financialConnectionsAvailable: Boolean = false,
    private val permissions: CustomerPermissions = CustomerPermissions(
        canRemovePaymentMethods = true,
        canRemoveLastPaymentMethod = true,
    ),
    private val isPaymentMethodSyncDefaultEnabled: Boolean = false,
) : CustomerSheetLoader {

    override suspend fun load(configuration: CustomerSheet.Configuration): Result<CustomerSheetState.Full> {
        delay(delay)
        return if (shouldFail) {
            Result.failure(IllegalStateException("failed to load"))
        } else {
            Result.success(
                CustomerSheetState.Full(
                    config = configuration,
                    PaymentMethodMetadataFactory.create(
                        stripeIntent = stripeIntent,
                        cbcEligibility = cbcEligibility,
                        paymentMethodOrder = configuration.paymentMethodOrder,
                        isGooglePayReady = isGooglePayAvailable,
                        isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSyncDefaultEnabled,
                    ),
                    supportedPaymentMethods = supportedPaymentMethods,
                    customerPaymentMethods = customerPaymentMethods,
                    customerPermissions = permissions,
                    paymentSelection = paymentSelection,
                    validationError = null,
                )
            )
        }
    }
}
