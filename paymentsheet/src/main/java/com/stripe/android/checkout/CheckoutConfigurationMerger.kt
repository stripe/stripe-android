package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutSessionData {
    val checkoutSessionResponse: CheckoutSessionResponse
    val shippingName: String?
    val billingName: String?
    val shippingPhoneNumber: String?
    val billingPhoneNumber: String?
    val shippingAddress: Address.State?
    val billingAddress: Address.State?
}

@OptIn(CheckoutSessionPreview::class)
internal sealed class CheckoutConfigurationMerger<T> {

    abstract fun forCheckoutSession(state: CheckoutSessionData): T

    class PaymentSheetConfiguration(
        private val config: PaymentSheet.Configuration,
    ) : CheckoutConfigurationMerger<PaymentSheet.Configuration>() {
        override fun forCheckoutSession(state: CheckoutSessionData): PaymentSheet.Configuration {
            val merged = mergeCheckoutSessionData(
                existingBillingDetails = config.defaultBillingDetails,
                existingShippingDetails = config.shippingDetails,
                state = state,
            )
            return config.newBuilder().apply {
                defaultBillingDetails(merged.billingDetails)
                shippingDetails(merged.shippingDetails)
                billingDetailsCollectionConfiguration(
                    config.billingDetailsCollectionConfiguration.copy(attachDefaultsToPaymentMethod = true)
                )
            }.build()
        }
    }

    class EmbeddedConfiguration(
        private val config: EmbeddedPaymentElement.Configuration,
    ) : CheckoutConfigurationMerger<EmbeddedPaymentElement.Configuration>() {
        override fun forCheckoutSession(state: CheckoutSessionData): EmbeddedPaymentElement.Configuration {
            val merged = mergeCheckoutSessionData(
                existingBillingDetails = config.defaultBillingDetails,
                existingShippingDetails = config.shippingDetails,
                state = state,
            )
            return config.newBuilder().apply {
                defaultBillingDetails(merged.billingDetails)
                shippingDetails(merged.shippingDetails)
                billingDetailsCollectionConfiguration(
                    config.billingDetailsCollectionConfiguration.copy(attachDefaultsToPaymentMethod = true)
                )
            }.build()
        }
    }
}

private data class MergedDetails(
    val billingDetails: PaymentSheet.BillingDetails,
    val shippingDetails: AddressDetails,
)

@OptIn(CheckoutSessionPreview::class)
private fun mergeCheckoutSessionData(
    existingBillingDetails: PaymentSheet.BillingDetails?,
    existingShippingDetails: AddressDetails?,
    state: CheckoutSessionData,
): MergedDetails {
    val response = state.checkoutSessionResponse
    return MergedDetails(
        billingDetails = PaymentSheet.BillingDetails(
            address = existingBillingDetails?.address ?: state.billingAddress?.asPaymentSheetAddress(),
            // customer_email is authoritative; fall back to the merchant default only when absent.
            email = response.customerEmail ?: existingBillingDetails?.email,
            name = existingBillingDetails?.name ?: state.billingName,
            phone = existingBillingDetails?.phone ?: state.billingPhoneNumber,
        ),
        shippingDetails = AddressDetails(
            name = existingShippingDetails?.name ?: state.shippingName,
            address = existingShippingDetails?.address ?: state.shippingAddress?.asPaymentSheetAddress(),
            phoneNumber = existingShippingDetails?.phoneNumber ?: state.shippingPhoneNumber,
            isCheckboxSelected = existingShippingDetails?.isCheckboxSelected,
        ),
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun Address.State.asPaymentSheetAddress(): PaymentSheet.Address {
    return PaymentSheet.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}
