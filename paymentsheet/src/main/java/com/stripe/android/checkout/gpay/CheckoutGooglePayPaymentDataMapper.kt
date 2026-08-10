package com.stripe.android.checkout.gpay

import android.content.Context
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.checkout.CheckoutControllerState
import com.stripe.android.googlepaylauncher.callback.GooglePayIntermediatePaymentData
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataRequestUpdate
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutGooglePayPaymentDataMapper {
    fun createRequestUpdate(
        state: CheckoutControllerState,
        context: Context,
        intermediatePaymentData: GooglePayIntermediatePaymentData,
    ): GooglePayPaymentDataRequestUpdate {
        val shouldUpdateShippingOptions = intermediatePaymentData.callbackTrigger !=
            GooglePayIntermediatePaymentData.CallbackTrigger.SHIPPING_OPTION

        return GooglePayPaymentDataRequestUpdate(
            newTransactionInfo = createTransactionInfo(
                state = state,
                context = context,
            ),
            newShippingOptionParameters = if (shouldUpdateShippingOptions) {
                createShippingOptionParameters(
                    response = state.checkoutSessionResponse,
                    selectedShippingOptionId = intermediatePaymentData.shippingOption?.id,
                )
            } else {
                null
            },
        )
    }

    fun createShippingOptionParameters(
        response: CheckoutSessionResponse,
        selectedShippingOptionId: String? = null,
    ): GooglePayJsonFactory.ShippingOptionParameters? {
        if (response.shippingOptions.isEmpty()) {
            return null
        }

        val defaultSelectedOptionId = selectedShippingOptionId
            ?: response.totalSummary?.shippingRate?.id
            ?: response.shippingOptions.first().id

        return GooglePayJsonFactory.ShippingOptionParameters(
            defaultSelectedOptionId = defaultSelectedOptionId,
            shippingOptions = response.shippingOptions.map { shippingRate ->
                GooglePayJsonFactory.ShippingOption(
                    id = shippingRate.id,
                    label = shippingRate.displayName,
                    description = shippingRate.deliveryEstimate,
                )
            },
        )
    }

    internal fun createTransactionInfo(
        state: CheckoutControllerState,
        context: Context,
    ): GooglePayJsonFactory.TransactionInfo {
        val response = state.checkoutSessionResponse
        val displayItems = GooglePayDisplayItemsFactory.create(state.paymentMethodMetadata)
            .map { it.resolve(context) }

        return GooglePayJsonFactory.TransactionInfo(
            currencyCode = response.currency.uppercase(),
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
            countryCode = merchantCountryCode(state).takeIf { it.isNotEmpty() },
            transactionId = response.stripeIntent()?.id,
            totalPrice = (response.totalSummary?.totalAmountDue ?: response.amount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt(),
            totalPriceLabel = context.getString(R.string.stripe_google_pay_total),
            checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
            displayItems = displayItems,
        )
    }

    private fun merchantCountryCode(state: CheckoutControllerState): String {
        return state.commonConfiguration.googlePay?.countryCode
            ?: state.checkoutSessionResponse.merchantCountry.orEmpty()
    }

    private fun CheckoutSessionResponse.stripeIntent(): StripeIntent? {
        return paymentIntent ?: setupIntent
    }
}
