package com.stripe.android.paymentsheet

import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenCreateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Address
import com.stripe.android.model.ShippingInformation
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.networking.StripeRepository
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.Logger
import javax.inject.Inject

/**
 * Creates ConfirmationTokens from PaymentSheet checkout state.
 *
 * This class follows the web Elements pattern of creating tokens on-demand when users
 * submit payment forms, auto-collecting data from the PaymentSheet's internal form state.
 */
internal class ConfirmationTokenCreator @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val logger: Logger
) {

    /**
     * Creates a ConfirmationToken from the current PaymentSheet checkout state.
     *
     * Equivalent to web Elements' `stripe.createConfirmationToken({ elements })` call,
     * auto-collecting payment method data, shipping, and billing details from PaymentSheet form state.
     *
     * @param paymentSelection The selected payment method from PaymentSheet
     * @param configuration PaymentSheet configuration containing billing/shipping defaults
     * @param shippingDetails Shipping information collected by PaymentSheet
     * @param publishableKey The publishable key for API calls
     * @return Result containing the ConfirmationToken or an error
     */
    suspend fun createConfirmationToken(
        paymentSelection: PaymentSelection?,
        configuration: PaymentSheet.Configuration,
        shippingDetails: AddressDetails? = null,
        publishableKey: String,
    ): Result<ConfirmationToken> {
        return try {
            when (paymentSelection) {
                is PaymentSelection.New -> {
                    createConfirmationTokenFromNewPaymentMethod(
                        paymentSelection = paymentSelection,
                        configuration = configuration,
                        shippingDetails = shippingDetails,
                        publishableKey = publishableKey
                    )
                }
                is PaymentSelection.Saved -> {
                    createConfirmationTokenFromSavedPaymentMethod(
                        paymentSelection = paymentSelection,
                        configuration = configuration,
                        shippingDetails = shippingDetails,
                        publishableKey = publishableKey
                    )
                }
                is PaymentSelection.GooglePay -> {
                    Result.failure(
                        UnsupportedOperationException("ConfirmationToken creation not supported for Google Pay")
                    )
                }
                is PaymentSelection.Link -> {
                    Result.failure(
                        UnsupportedOperationException("ConfirmationToken creation not supported for Link")
                    )
                }
                is PaymentSelection.ShopPay -> {
                    Result.failure(
                        UnsupportedOperationException("ConfirmationToken creation not supported for ShopPay")
                    )
                }
                is PaymentSelection.CustomPaymentMethod -> {
                    Result.failure(
                        UnsupportedOperationException("ConfirmationToken creation not supported for CustomPaymentMethod")
                    )
                }
                is PaymentSelection.ExternalPaymentMethod -> {
                    Result.failure(
                        UnsupportedOperationException("ConfirmationToken creation not supported for ExternalPaymentMethod")
                    )
                }
                null -> {
                    Result.failure(
                        IllegalArgumentException("No payment method selected")
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("ConfirmationToken creation failed", e)
            Result.failure(e)
        }
    }

    private suspend fun createConfirmationTokenFromNewPaymentMethod(
        paymentSelection: PaymentSelection.New,
        configuration: PaymentSheet.Configuration,
        shippingDetails: AddressDetails?,
        publishableKey: String,
    ): Result<ConfirmationToken> {
        // Auto-collect payment method data from PaymentSheet form state
        val paymentMethodCreateParams = createPaymentMethodCreateParams(
            paymentSelection = paymentSelection,
            defaultBillingDetails = configuration.defaultBillingDetails
        )

        // Auto-collect shipping details if provided
        val shipping = createShippingInformation(shippingDetails, configuration.shippingDetails)

        // Create ConfirmationToken create params (equivalent to web's elements parameter)
        val createParams = ConfirmationTokenCreateParams.createWithPaymentMethodCreateParams(
            paymentMethodCreateParams = paymentMethodCreateParams,
            shipping = shipping,
            // Return URL from configuration (for redirect-based payment methods)
            returnUrl = extractReturnUrl(configuration),
            // Setup future usage based on PaymentSheet configuration
            setupFutureUsage = determineSetupFutureUsage(paymentSelection)
        )

        // Make API call to create ConfirmationToken
        val result = stripeRepository.createConfirmationToken(
            confirmationTokenCreateParams = createParams,
            options = ApiRequest.Options(publishableKey)
        )

        return if (result.isSuccess) {
            Result.success(result.getOrThrow())
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown ConfirmationToken creation error"))
        }
    }

    private suspend fun createConfirmationTokenFromSavedPaymentMethod(
        paymentSelection: PaymentSelection.Saved,
        configuration: PaymentSheet.Configuration,
        shippingDetails: AddressDetails?,
        publishableKey: String,
    ): Result<ConfirmationToken> {
        val paymentMethod = paymentSelection.paymentMethod
        
        // Auto-collect shipping details if provided
        val shipping = createShippingInformation(shippingDetails, configuration.shippingDetails)

        // Create ConfirmationToken with saved payment method ID
        val createParams = ConfirmationTokenCreateParams.createWithPaymentMethodId(
            paymentMethodId = paymentMethod.id!!,
            paymentMethodType = paymentMethod.type!!,
            shipping = shipping,
            returnUrl = extractReturnUrl(configuration),
            // For saved payment methods, check if we need to collect CVC or setup future usage
            setupFutureUsage = if (paymentSelection.walletType != null) {
                // Wallet payments typically don't need setup future usage
                null
            } else {
                determineSetupFutureUsageForSaved(paymentSelection)
            }
        )

        // Make API call to create ConfirmationToken  
        val result = stripeRepository.createConfirmationToken(
            confirmationTokenCreateParams = createParams,
            options = ApiRequest.Options(publishableKey)
        )

        return if (result.isSuccess) {
            Result.success(result.getOrThrow())
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown ConfirmationToken creation error"))
        }
    }

    private fun createPaymentMethodCreateParams(
        paymentSelection: PaymentSelection.New,
        defaultBillingDetails: PaymentSheet.BillingDetails?
    ): PaymentMethodCreateParams {
        return paymentSelection.paymentMethodCreateParams.copy(
            // Merge form-collected billing details with configuration defaults
            billingDetails = mergeBillingDetails(
                formBillingDetails = paymentSelection.paymentMethodCreateParams.billingDetails,
                defaultBillingDetails = defaultBillingDetails
            )
        )
    }

    private fun createShippingInformation(
        shippingDetails: AddressDetails?,
        configShippingDetails: AddressDetails?
    ): ShippingInformation? {
        val details = shippingDetails ?: configShippingDetails ?: return null
        
        return ShippingInformation(
            name = details.name ?: "",
            address = com.stripe.android.model.Address(
                line1 = details.address?.line1,
                line2 = details.address?.line2,
                city = details.address?.city,
                state = details.address?.state,
                postalCode = details.address?.postalCode,
                country = details.address?.country
            ),
            phone = details.phoneNumber
        )
    }

    private fun mergeBillingDetails(
        formBillingDetails: PaymentMethod.BillingDetails?,
        defaultBillingDetails: PaymentSheet.BillingDetails?
    ): PaymentMethod.BillingDetails? {
        if (formBillingDetails == null && defaultBillingDetails == null) return null

        return PaymentMethod.BillingDetails(
            name = formBillingDetails?.name ?: defaultBillingDetails?.name,
            email = formBillingDetails?.email ?: defaultBillingDetails?.email,
            phone = formBillingDetails?.phone ?: defaultBillingDetails?.phone,
            address = mergeBillingAddresses(
                formBillingDetails?.address,
                defaultBillingDetails?.address
            )
        )
    }

    private fun mergeBillingAddresses(
        formAddress: Address?,
        defaultAddress: PaymentSheet.Address?
    ): Address? {
        if (formAddress == null && defaultAddress == null) return null

        return Address(
            line1 = formAddress?.line1 ?: defaultAddress?.line1,
            line2 = formAddress?.line2 ?: defaultAddress?.line2,
            city = formAddress?.city ?: defaultAddress?.city,
            state = formAddress?.state ?: defaultAddress?.state,
            postalCode = formAddress?.postalCode ?: defaultAddress?.postalCode,
            country = formAddress?.country ?: defaultAddress?.country
        )
    }

    private fun extractReturnUrl(configuration: PaymentSheet.Configuration): String? {
        // For PaymentSheet, return URL is typically provided via IntentConfiguration
        // or determined dynamically based on the app's deep link configuration
        return null // TODO: Implement return URL extraction logic
    }

    private fun determineSetupFutureUsage(
        paymentSelection: PaymentSelection.New
    ): ConfirmationTokenCreateParams.SetupFutureUsage? {
        // Determine setup future usage based on PaymentSheet form state
        // This would typically be based on a "Save for future payments" checkbox
        return when (paymentSelection.customerRequestedSave) {
            PaymentSelection.CustomerRequestedSave.RequestReuse -> {
                ConfirmationTokenCreateParams.SetupFutureUsage.OffSession
            }
            PaymentSelection.CustomerRequestedSave.RequestNoReuse,
            PaymentSelection.CustomerRequestedSave.NoRequest -> {
                null
            }
        }
    }

    private fun determineSetupFutureUsageForSaved(
        paymentSelection: PaymentSelection.Saved
    ): ConfirmationTokenCreateParams.SetupFutureUsage? {
        // For saved payment methods, setup future usage is typically not needed
        // unless we're updating the payment method (e.g., CVC recollection)
        return null
    }
}
