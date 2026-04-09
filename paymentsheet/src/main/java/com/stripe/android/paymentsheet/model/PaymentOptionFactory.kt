package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toDrawable
import com.stripe.android.core.exception.StripeException
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.paymentsheet.PaymentOptionCardArtProvider
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val paymentOptionCardArtProvider: PaymentOptionCardArtProvider,
    private val imageLoader: StripeImageLoader,
    private val errorReporter: ErrorReporter,
    private val context: Context,
) {
    fun create(selection: PaymentSelection): PaymentOption {
        val drawableResourceId = selection.drawableResourceId
        val lightThemeIconUrl = selection.lightThemeIconUrl
        val darkThemeIconUrl = selection.darkThemeIconUrl

        return PaymentOption(
            drawableResourceId = drawableResourceId,
            label = selection.label.resolve(context),
            paymentMethodType = selection.paymentMethodType,
            _labels = PaymentOptionLabelsFactory.create(context, selection),
            billingDetails = selection.billingDetails?.toPaymentSheetBillingDetails(),
            _shippingDetails = selection.shippingDetails,
            imageLoader = {
                loadCardArtDrawable(selection) ?: iconLoader.load(
                    drawableResourceId = drawableResourceId,
                    drawableResourceIdNight = drawableResourceId,
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
                )
            },
        )
    }

    private suspend fun loadCardArtDrawable(selection: PaymentSelection): Drawable? {
        val cardArt = (selection as? PaymentSelection.Saved)?.paymentMethod?.card?.cardArt ?: return null
        val url = paymentOptionCardArtProvider(cardArt) ?: return null
        return imageLoader.load(url)
            .mapCatching { bitmap -> bitmap?.toDrawable(context.resources) }
            .onFailure { error ->
                errorReporter.report(
                    errorEvent = ExpectedErrorEvent.PAYMENT_OPTION_CARD_ART_LOAD_FAILURE,
                    stripeException = StripeException.create(error),
                )
            }
            .getOrNull()
    }
}

internal val PaymentSelection.shippingDetails: AddressDetails?
    get() = when (this) {
        is PaymentSelection.CustomPaymentMethod,
        is PaymentSelection.ExternalPaymentMethod,
        is PaymentSelection.GooglePay,
        is PaymentSelection.New.Card,
        is PaymentSelection.New.GenericPaymentMethod,
        is PaymentSelection.New.USBankAccount,
        is PaymentSelection.ShopPay,
        is PaymentSelection.Saved -> {
            null
        }
        is PaymentSelection.Link -> {
            makeAddressDetails()
        }
    }

private fun PaymentSelection.Link.makeAddressDetails(): AddressDetails? {
    return shippingAddress?.let { address ->
        AddressDetails(
            name = address.address.name,
            phoneNumber = address.unredactedPhoneNumber,
            address = PaymentSheet.Address(
                line1 = address.address.line1,
                line2 = address.address.line2,
                city = address.address.locality,
                state = address.address.administrativeArea,
                postalCode = address.address.postalCode,
                country = address.address.countryCode?.value,
            ),
            isCheckboxSelected = null,
        )
    }
}
