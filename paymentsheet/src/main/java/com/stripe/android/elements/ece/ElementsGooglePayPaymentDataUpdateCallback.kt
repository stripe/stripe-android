package com.stripe.android.elements.ece

import android.content.Context
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.R
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingAddress
import com.stripe.android.elements.ElementsController.ExpressCheckoutElement.ShippingChangeResult
import com.stripe.android.elements.ElementsControllerState
import com.stripe.android.elements.ElementsControllerStateHolder
import com.stripe.android.elements.ElementsShippingHandler
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataError
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdate.CallbackTrigger
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.model.PaymentIntent
import javax.inject.Inject

/** Translates Google Pay transport objects to wallet-neutral merchant shipping events. */
internal class ElementsGooglePayPaymentDataUpdateCallback @Inject constructor(
    private val stateHolder: ElementsControllerStateHolder,
    private val shippingHandler: ElementsShippingHandler,
    private val context: Context,
) : GooglePayPaymentDataUpdateCallback {
    override suspend fun onPaymentDataChanged(
        paymentDataUpdate: GooglePayPaymentDataUpdate,
    ): GooglePayPaymentDataUpdateResponse {
        val update = paymentDataUpdate
        val result = when (update.callbackTrigger) {
            CallbackTrigger.Initialize, CallbackTrigger.ShippingAddress -> update.shippingAddress?.let {
                shippingHandler.onAddressChange(
                    ShippingAddress(
                        country = it.countryCode,
                        state = it.normalizedAdministrativeArea.takeIf(String::isNotEmpty),
                        city = it.locality.takeIf(String::isNotEmpty),
                        postalCode = it.postalCode.takeIf(String::isNotEmpty),
                    )
                )
            }
            else -> null
        }
        val loaded = stateHolder.state
        return when {
            result is ShippingChangeResult.Reject -> rejection(result.message)
            loaded != null &&
                (result is ShippingChangeResult.Resolve || update.callbackTrigger == CallbackTrigger.Initialize) ->
                GooglePayPaymentDataUpdateResponse(
                    newTransactionInfo = loaded.transactionInfo(),
                    error = null,
                )
            else -> GooglePayPaymentDataUpdateResponse(newTransactionInfo = null, error = null)
        }
    }

    private fun ElementsControllerState.transactionInfo(): GooglePayJsonFactory.TransactionInfo {
        return GooglePayJsonFactory.TransactionInfo(
            currencyCode = walletCurrencyCode(),
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
            countryCode = configuration.googlePay?.countryCode,
            transactionId = paymentMethodMetadata.stripeIntent.id,
            totalPrice = walletState.amount,
            totalPriceLabel = configuration.expressCheckoutElementConfiguration?.googlePayConfiguration?.label
                ?: configuration.googlePay?.label ?: context.getString(R.string.stripe_google_pay_total),
            checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
            displayItems = walletState.lineItems.map {
                GooglePayJsonFactory.DisplayItem(
                    label = it.name,
                    type = if (it.amount < 0) {
                        GooglePayJsonFactory.DisplayItem.Type.DISCOUNT
                    } else {
                        GooglePayJsonFactory.DisplayItem.Type.LINE_ITEM
                    },
                    price = it.amount,
                )
            },
        )
    }

    private fun rejection(message: String): GooglePayPaymentDataUpdateResponse {
        return GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = GooglePayPaymentDataError(
                reason = GooglePayPaymentDataError.Reason.ShippingAddressInvalid,
                message = message,
                intent = GooglePayPaymentDataError.Intent.ShippingAddress,
            ),
        )
    }
}

private fun ElementsControllerState.walletCurrencyCode(): String =
    (paymentMethodMetadata.stripeIntent as? PaymentIntent)?.currency ?: configuration.googlePay?.currencyCode.orEmpty()
