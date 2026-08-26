package com.stripe.android.paymentsheet.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentOptionCardArtDrawableLoader
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.ui.MIN_LUMINANCE_FOR_LIGHT_ICON
import com.stripe.android.paymentsheet.ui.isDarkTheme
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.isSystemDarkTheme
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val iconLoader: PaymentSelection.IconLoader,
    private val cardArtDrawableLoader: PaymentOptionCardArtDrawableLoader,
    private val context: Context,
) {
    fun create(
        selection: PaymentSelection,
        linkBrand: LinkBrand?,
        appearance: PaymentSheet.Appearance?,
    ): PaymentOption {
        val drawableResourceId = selection.drawableResourceId
        val lightThemeIconUrl = selection.lightThemeIconUrl
        val darkThemeIconUrl = selection.darkThemeIconUrl

        return PaymentOption(
            drawableResourceId = drawableResourceId,
            label = selection.label(linkBrand).resolve(context),
            paymentMethodType = selection.paymentMethodType,
            _labels = PaymentOptionLabelsFactory.create(context, selection, linkBrand),
            billingDetails = selection.billingDetails?.toPaymentSheetBillingDetails(),
            _shippingDetails = selection.shippingDetails,
            imageLoader = {
                cardArtDrawableLoader.load(selection) ?: iconLoader.load(
                    drawableResourceId = drawableResourceId,
                    drawableResourceIdNight = drawableResourceId,
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
                    useDarkThemeIcon = appearance?.shouldUseDarkThemeIcon(context)
                        ?: shouldUseDarkThemeIcon(context),
                )
            },
        )
    }
}

internal fun shouldUseDarkThemeIcon(context: Context): Boolean {
    return context.isSystemDarkTheme() ||
        StripeTheme.colorsLightMutable.component.luminance() < MIN_LUMINANCE_FOR_LIGHT_ICON
}

internal fun PaymentSheet.Appearance.shouldUseDarkThemeIcon(context: Context): Boolean {
    val isDark = themeMode.isDarkTheme(context.isSystemDarkTheme())
    val componentColor = Color(getColors(isDark).component)
    return componentColor.luminance() < MIN_LUMINANCE_FOR_LIGHT_ICON
}

internal val PaymentSelection.shippingDetails: AddressDetails?
    get() = when (this) {
        is PaymentSelection.CustomPaymentMethod,
        is PaymentSelection.ExternalPaymentMethod,
        is PaymentSelection.GooglePay,
        is PaymentSelection.New.Card,
        is PaymentSelection.New.GenericPaymentMethod,
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
