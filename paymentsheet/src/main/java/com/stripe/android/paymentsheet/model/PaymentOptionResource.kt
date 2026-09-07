package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentsheet.PaymentSheet

@Stable
internal class PaymentOptionResource(
    private val appearance: PaymentSheet.Appearance,
    private val loader: suspend (useDarkThemeIcon: Boolean) -> Drawable,
) {
    suspend fun load(isSystemDarkTheme: Boolean): Drawable {
        return loader(useDarkThemeIcon(isSystemDarkTheme))
    }

    fun useDarkThemeIcon(isSystemDarkTheme: Boolean): Boolean {
        return appearance.shouldUseDarkThemeIcon(isSystemDarkTheme)
    }
}

@Composable
internal fun rememberPaymentOptionResource(resource: PaymentOptionResource): Drawable {
    val isSystemDarkTheme = isSystemInDarkTheme()
    val useDarkThemeIcon = resource.useDarkThemeIcon(isSystemDarkTheme)
    return remember(resource, useDarkThemeIcon) {
        DelegateDrawable {
            resource.load(isSystemDarkTheme)
        }
    }
}
