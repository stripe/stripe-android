package com.stripe.android.paymentsheet.model

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal class PaymentOptionFactory @Inject constructor(
    private val resources: Resources,
    private val imageLoader: StripeImageLoader,
    private val context: Context,
) {
    private fun isDarkTheme(): Boolean {
        return resources.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    @VisibleForTesting
    internal suspend fun loadPaymentOption(paymentOption: PaymentOption): Drawable {
        fun loadResource(): Drawable {
            @Suppress("DEPRECATION")
            return runCatching {
                ResourcesCompat.getDrawable(
                    resources,
                    paymentOption.drawableResourceId,
                    null
                )
            }.getOrNull() ?: emptyDrawable
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
        } else {
            loadResource()
        }
    }

    fun create(selection: PaymentSelection): PaymentOption {
        return PaymentOption(
            drawableResourceId = selection.drawableResourceId,
            lightThemeIconUrl = selection.lightThemeIconUrl,
            darkThemeIconUrl = selection.darkThemeIconUrl,
            label = selection.label.resolve(context),
            imageLoader = ::loadPaymentOption,
        )
    }

    companion object {
        @VisibleForTesting
        internal val emptyDrawable = ShapeDrawable()
    }
}
