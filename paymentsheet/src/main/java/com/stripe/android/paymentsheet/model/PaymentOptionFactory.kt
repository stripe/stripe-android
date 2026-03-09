package com.stripe.android.paymentsheet.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val context: Context,
) {
    fun create(selection: PaymentSelection): PaymentOption {
        val drawableResourceId = selection.drawableResourceId
        val lightThemeIconUrl = selection.lightThemeIconUrl
        val darkThemeIconUrl = selection.darkThemeIconUrl
        val cardArtUrl = (selection as? PaymentSelection.Saved)?.paymentMethod?.card?.cardArt?.artImage?.url

        return PaymentOption(
            drawableResourceId = drawableResourceId,
            label = selection.label.resolve(context),
            paymentMethodType = selection.paymentMethodType,
            _labels = PaymentOptionLabelsFactory.create(context, selection),
            billingDetails = selection.billingDetails?.toPaymentSheetBillingDetails(),
            _shippingDetails = selection.shippingDetails,
            imageLoader = {
                val icon = iconLoader.load(
                    drawableResourceId = drawableResourceId,
                    drawableResourceIdNight = drawableResourceId,
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
                )
                val artDrawable = cardArtUrl?.let {
                    val imageLoader = StripeImageLoader(context)
                    imageLoader.load(it)
                }?.map { bitmap ->
                    bitmap?.let {
                        val scaledBitmap = it.scale(icon.intrinsicWidth, icon.intrinsicHeight)
                        scaledBitmap.toDrawable(context.resources)
                    }
                }?.getOrNull()
                artDrawable ?: icon
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
