package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails

@OptIn(CheckoutSessionPreview::class)
internal sealed class CheckoutConfigurationMerger<T> {

    abstract fun forCheckoutSession(state: InternalState): T

    class PaymentSheetConfiguration(
        private val config: PaymentSheet.Configuration,
    ) : CheckoutConfigurationMerger<PaymentSheet.Configuration>() {
        override fun forCheckoutSession(state: InternalState): PaymentSheet.Configuration {
            val merged = mergeCheckoutSessionData(
                existingBillingDetails = config.defaultBillingDetails,
                existingShippingDetails = config.shippingDetails,
                state = state,
            )
            return config.newBuilder().apply {
                defaultBillingDetails(merged.billingDetails)
                shippingDetails(merged.shippingDetails)
            }.build()
        }
    }

    class EmbeddedConfiguration(
        private val config: EmbeddedPaymentElement.Configuration,
    ) : CheckoutConfigurationMerger<EmbeddedPaymentElement.Configuration>() {
        override fun forCheckoutSession(state: InternalState): EmbeddedPaymentElement.Configuration {
            val merged = mergeCheckoutSessionData(
                existingBillingDetails = config.defaultBillingDetails,
                existingShippingDetails = config.shippingDetails,
                state = state,
            )
            return config.newBuilder().apply {
                defaultBillingDetails(merged.billingDetails)
                shippingDetails(merged.shippingDetails)
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
    state: InternalState,
): MergedDetails {
    val response = state.checkoutSessionResponse
    return MergedDetails(
        billingDetails = PaymentSheet.BillingDetails(
            address = existingBillingDetails?.address ?: state.billingAddress?.asPaymentSheetAddress(),
            email = existingBillingDetails?.email ?: response.customerEmail,
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
