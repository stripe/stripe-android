package com.stripe.android.paymentsheet.model

import android.content.Context
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader,
    private val context: Context,
) {
    fun create(
        selection: PaymentSelection,
    ): PaymentOption {
        return createInternal(
            selection = selection,
            linkBrand = null,
        )
    }

    fun create(
        selection: PaymentSelection,
        linkBrand: LinkBrand,
    ): PaymentOption {
        return createInternal(
            selection = selection,
            linkBrand = linkBrand,
        )
    }

    private fun createInternal(
        selection: PaymentSelection,
        linkBrand: LinkBrand?,
    ): PaymentOption {
        val drawableResourceId = selection.drawableResourceId
        val lightThemeIconUrl = selection.lightThemeIconUrl
        val darkThemeIconUrl = selection.darkThemeIconUrl

        return PaymentOption(
            drawableResourceId = drawableResourceId,
            label = when (linkBrand) {
                null -> selection.label
                else -> selection.label(linkBrand)
            }.resolve(context),
            paymentMethodType = selection.paymentMethodType,
            _labels = when (linkBrand) {
                null -> PaymentOptionLabelsFactory.create(context, selection)
                else -> PaymentOptionLabelsFactory.create(
                    context = context,
                    selection = selection,
                    linkBrand = linkBrand,
                )
            },
            billingDetails = selection.billingDetails?.toPaymentSheetBillingDetails(),
            _shippingDetails = selection.shippingDetails,
            imageLoader = {
                cardArtDrawableLoader.load(selection) ?: iconLoader.load(
                    drawableResourceId = drawableResourceId,
                    drawableResourceIdNight = drawableResourceId,
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
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
