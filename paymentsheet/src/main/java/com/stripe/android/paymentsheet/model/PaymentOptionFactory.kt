package com.stripe.android.paymentsheet.model

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.createCardLabel
import com.stripe.android.paymentsheet.ui.getCardBrandIcon
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject
import com.stripe.android.R as StripeR

internal class PaymentOptionFactory @Inject constructor(
    private val resources: Resources,
    private val imageLoader: StripeImageLoader,
    private val errorReporter: ErrorReporter,
) {
    private fun isDarkTheme(): Boolean {
        return resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private suspend fun loadPaymentOption(paymentOption: PaymentOption): Drawable {
        fun loadResource(): Drawable {
            @Suppress("DEPRECATION")
            return ResourcesCompat.getDrawable(
                resources,
                paymentOption.drawableResourceId,
                null
            ) ?: ShapeDrawable()
        }

        suspend fun loadIcon(url: String): Drawable {
            return imageLoader.load(url).getOrNull()?.let {
                BitmapDrawable(resources, it)
            } ?: loadResource()
        }

        // If the payment option has an icon URL, we prefer it.
        // Some payment options don't have an icon URL, and are loaded locally via resource.
        val lightThemeIconUrl = paymentOption.lightThemeIconUrl
        val darkThemeIconUrl = paymentOption.darkThemeIconUrl
        return if (isDarkTheme() && darkThemeIconUrl != null) {
            loadIcon(darkThemeIconUrl)
        } else if (lightThemeIconUrl != null) {
            loadIcon(lightThemeIconUrl)
        } else if (paymentOption.drawableResourceId == MISSING_DRAWABLE_RESOURCE) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.PAYMENT_OPTION_MISSING_ICON_URL_AND_RES,
                additionalNonPiiParams = mapOf("payment_option" to paymentOption.label)
            )
            throw IllegalStateException("Missing icon resource and icon URLs - can't load")
        } else {
            loadResource()
        }
    }

    fun create(selection: PaymentSelection): PaymentOption {
        return when (selection) {
            PaymentSelection.GooglePay -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_google_pay_mark,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = resources.getString(StripeR.string.stripe_google_pay),
                    imageLoader = ::loadPaymentOption,
                )
            }
            PaymentSelection.Link -> {
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_paymentsheet_link,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = resources.getString(StripeR.string.stripe_link),
                    imageLoader = ::loadPaymentOption,
                )
            }
            is PaymentSelection.Saved -> {
                PaymentOption(
                    drawableResourceId = getSavedIcon(selection),
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    label = getSavedLabel(selection).orEmpty(),
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
                    drawableResourceId = selection.paymentMethodIcon.getNullableIconResource()
                        ?: MISSING_DRAWABLE_RESOURCE,
                    lightThemeIconUrl = selection.paymentMethodIcon.getNullableLightThemeIconUrl(),
                    darkThemeIconUrl = selection.paymentMethodIcon.getNullableDarkThemeIconUrl(),
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

    private fun getSavedLabel(selection: PaymentSelection.Saved): String? {
        return selection.paymentMethod.getLabel(resources) ?: run {
            when (selection.walletType) {
                PaymentSelection.Saved.WalletType.Link -> resources.getString(StripeR.string.stripe_link)
                PaymentSelection.Saved.WalletType.GooglePay -> resources.getString(StripeR.string.stripe_google_pay)
                else -> null
            }
        }
    }

    private fun getSavedIcon(selection: PaymentSelection.Saved): Int {
        return when (val resourceId = selection.paymentMethod.getSavedPaymentMethodIcon()) {
            R.drawable.stripe_ic_paymentsheet_card_unknown -> {
                when (selection.walletType) {
                    PaymentSelection.Saved.WalletType.Link -> R.drawable.stripe_ic_paymentsheet_link
                    PaymentSelection.Saved.WalletType.GooglePay -> R.drawable.stripe_google_pay_mark
                    else -> resourceId
                }
            }
            else -> resourceId
        }
    }

    companion object {
        private const val MISSING_DRAWABLE_RESOURCE = 0
    }
}
