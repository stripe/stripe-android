package com.stripe.android.paymentsheet.model

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.createCardLabel
import com.stripe.android.paymentsheet.ui.getCardBrandIcon
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.uicore.image.StripeImageLoader

internal class PaymentOptionFactory(
    private val resources: Resources,
    private val imageLoader: StripeImageLoader,
) {
    private fun isDarkTheme(): Boolean {
        return resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private suspend fun loadPaymentOption(paymentOption: PaymentOption): Drawable {
        // If the payment option has an icon URL, we prefer it.
        // Some payment options don't have an icon URL, and are loaded locally via resource.
        paymentOption.lightThemeIconUrl?.let { iconUrl ->
            val darkThemeIconUrl = paymentOption.darkThemeIconUrl
            val iconToLoad = if (isDarkTheme() && darkThemeIconUrl != null) {
                darkThemeIconUrl
            } else {
                iconUrl
            }
            imageLoader.load(iconToLoad).getOrNull()?.let {
                return BitmapDrawable(resources, it)
            }
        }
        @Suppress("DEPRECATION")
        ResourcesCompat.getDrawable(resources, paymentOption.drawableResourceId, null)?.let {
            return it
        }
        return ShapeDrawable()
    }

    fun create(selection: PaymentSelection): PaymentOption {
        return when (selection) {
            PaymentSelection.GooglePay -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_google_pay_mark,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = resources.getString(R.string.google_pay),
                    imageLoader = ::loadPaymentOption,
                )
            }
            PaymentSelection.Link -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_paymentsheet_link,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = resources.getString(R.string.link),
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.Saved -> {
                PaymentOption(
                    drawableResourceId = selection.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = selection.paymentMethod.getLabel(resources).orEmpty(),
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.New.Card -> {
                // TODO: Should use labelResource paymentMethodCreateParams or extension function
                PaymentOption(
                    drawableResourceId = selection.brand.getCardBrandIcon(),
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = createCardLabel(
                        resources,
                        selection.last4
                    ),
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.New.LinkInline -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = selection.label,
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.New.GenericPaymentMethod -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    lightThemeIconUrl = selection.lightThemeIconUrl,
                    darkThemeIconUrl = selection.darkThemeIconUrl,
                    label = selection.labelResource,
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.New.USBankAccount -> {
                PaymentOption(
                    drawableResourceId = selection.iconResource,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = selection.labelResource,
                    imageLoader = ::loadPaymentOption,
                )
            }
        }
    }
}
