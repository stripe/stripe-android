package com.stripe.android.paymentsheet.model

import android.content.Context
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(selection: PaymentSelection): PaymentOption {
        return PaymentOption(
            drawableResourceId = selection.drawableResourceId,
            label = selection.label.resolve(context),
            paymentMethodType = selection.paymentMethodType,
            _shippingDetails = selection.shippingDetails,
            imageLoader = {
                iconLoader.load(
                    drawableResourceId = selection.drawableResourceId,
                    lightThemeIconUrl = selection.lightThemeIconUrl,
                    darkThemeIconUrl = selection.darkThemeIconUrl,
                )
            },
        )
    }
}

internal val PaymentSelection.shippingDetails: AddressDetails?
    get() = when (this) {
        is PaymentSelection.CustomPaymentMethod,
        is PaymentSelection.ExternalPaymentMethod,
        is PaymentSelection.GooglePay,
        is PaymentSelection.New.Card,
        is PaymentSelection.New.GenericPaymentMethod,
        is PaymentSelection.New.LinkInline,
        is PaymentSelection.New.USBankAccount,
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
