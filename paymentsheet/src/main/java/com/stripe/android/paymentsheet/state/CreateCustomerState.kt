package com.stripe.android.paymentsheet.state

import com.stripe.android.CardFundingFilter
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.Deferred
import javax.inject.Inject

internal class CreateCustomerState @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val paymentMethodFilter: PaymentMethodFilter,
) {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: com.stripe.android.common.model.CommonConfiguration,
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        metadata: PaymentMethodMetadata,
        savedSelection: Deferred<SavedSelection>,
        cardBrandFilter: PaymentSheetCardBrandFilter,
        cardFundingFilter: CardFundingFilter,
    ): CustomerState? {
        val customerState = when (customerMetadata) {
            is CustomerMetadata.CheckoutSession -> {
                val checkoutInit = initializationMode as PaymentElementLoader.InitializationMode.CheckoutSession
                checkoutInit.checkoutSessionResponse.customer?.let {
                    createForCheckoutSession(
                        customer = it,
                        supportedSavedPaymentMethodTypes = metadata.supportedSavedPaymentMethodTypes(),
                    )
                }
            }
            is CustomerMetadata.CustomerSession -> {
                elementsSession.customer?.let { customer ->
                    createForCustomerSession(
                        customer = customer,
                        supportedSavedPaymentMethodTypes = metadata.supportedSavedPaymentMethodTypes(),
                    )
                }
            }
            is CustomerMetadata.LegacyEphemeralKey -> {
                val customerConfig = configuration.customer ?: return null
                createForLegacyEphemeralKey(
                    paymentMethods = retrieveCustomerPaymentMethods(
                        metadata = metadata,
                        customerConfig = customerConfig,
                    )
                )
            }
            null -> null
        }

        return customerState?.let { state ->
            state.copy(
                paymentMethods = paymentMethodFilter.filter(
                    paymentMethods = state.paymentMethods,
                    params = PaymentMethodFilter.FilterParams(
                        billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
                        customerMetadata = metadata.customerMetadata,
                        cardBrandFilter = cardBrandFilter,
                        cardFundingFilter = cardFundingFilter,
                        remoteDefaultPaymentMethodId = state.defaultPaymentMethodId,
                        localSavedSelection = savedSelection,
                    )
                )
            )
        }
    }

    private fun createForCustomerSession(
        customer: ElementsSession.Customer,
        supportedSavedPaymentMethodTypes: List<PaymentMethod.Type>,
    ): CustomerState {
        return CustomerState(
            paymentMethods = customer.paymentMethods.filter {
                supportedSavedPaymentMethodTypes.contains(it.type)
            },
            defaultPaymentMethodId = customer.defaultPaymentMethod
        )
    }

    private fun createForLegacyEphemeralKey(
        paymentMethods: List<PaymentMethod>,
    ): CustomerState {
        return CustomerState(
            paymentMethods = paymentMethods,
            defaultPaymentMethodId = null
        )
    }

    private fun createForCheckoutSession(
        customer: CheckoutSessionResponse.Customer,
        supportedSavedPaymentMethodTypes: List<PaymentMethod.Type>,
    ): CustomerState {
        return CustomerState(
            paymentMethods = customer.paymentMethods.filter {
                supportedSavedPaymentMethodTypes.contains(it.type)
            },
            defaultPaymentMethodId = null
        )
    }

    private suspend fun retrieveCustomerPaymentMethods(
        metadata: PaymentMethodMetadata,
        customerConfig: PaymentSheet.CustomerConfiguration,
    ): List<PaymentMethod> {
        val paymentMethodTypes = metadata.supportedSavedPaymentMethodTypes()

        val paymentMethods = customerRepository.getPaymentMethods(
            customerId = customerConfig.id,
            ephemeralKeySecret = customerConfig.ephemeralKeySecret,
            types = paymentMethodTypes,
            silentlyFail = metadata.stripeIntent.isLiveMode,
        ).getOrThrow()

        return paymentMethods.filter { paymentMethod ->
            paymentMethod.hasExpectedDetails()
        }
    }
}
