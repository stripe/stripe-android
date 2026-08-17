package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayDisplayItemsFactory
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal interface CheckoutGooglePayPaymentDataUpdateMapper {
    fun toResponse(
        configuration: CommonConfiguration,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutGooglePayPaymentDataUpdateMapper @Inject constructor(
    private val context: Context,
) : CheckoutGooglePayPaymentDataUpdateMapper {
    override fun toResponse(
        configuration: CommonConfiguration,
        response: CheckoutSessionResponse,
        paymentDataUpdate: GooglePayPaymentDataUpdate
    ): GooglePayPaymentDataUpdateResponse {
        val displayItems = GooglePayDisplayItemsFactory.create(response, context)

        return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = GooglePayJsonFactory.TransactionInfo(
                currencyCode = response.currency.uppercase(),
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                countryCode = merchantCountryCode(configuration, response)?.takeIf { it.isNotEmpty() },
                transactionId = response.stripeIntent()?.id,
                totalPrice = response.totalSummary?.totalAmountDue ?: response.amount,
                totalPriceLabel = context.getString(R.string.stripe_google_pay_total),
                checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
                displayItems = displayItems,
            ),
            error = null,
        )
    }

    private fun merchantCountryCode(
        configuration: CommonConfiguration,
        response: CheckoutSessionResponse,
    ): String? {
        return configuration.googlePay?.countryCode ?: response.merchantCountry
    }

    private fun CheckoutSessionResponse.stripeIntent(): StripeIntent? {
        return paymentIntent ?: setupIntent
    }
}
