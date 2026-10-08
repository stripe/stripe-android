package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.image.rememberDrawablePainter

@Stable
internal interface PaymentOptionResource {
    suspend fun load(isSystemDarkTheme: Boolean?): Drawable

    @Composable
    fun rememberPainter(): Painter
}

@Stable
internal class DefaultPaymentOptionResource(
    private val appearance: PaymentSheet.Appearance,
    private val loader: suspend (AppearancePacket) -> Drawable,
) : PaymentOptionResource {
    override suspend fun load(isSystemDarkTheme: Boolean?): Drawable {
        return loader(AppearancePacket(appearance, isSystemDarkTheme))
    }

    @Composable
    override fun rememberPainter(): Painter {
        val drawable = rememberPaymentOptionDrawable()
        return rememberDrawablePainter(drawable)
    }

    @Composable
    private fun rememberPaymentOptionDrawable(): Drawable {
        val isSystemDarkTheme = isSystemInDarkTheme()
        val useDarkThemeIcon = appearance.shouldUseDarkThemeIcon(isSystemDarkTheme)
        return remember(this, useDarkThemeIcon) {
            DelegateDrawable {
                load(isSystemDarkTheme)
            }
        }
    }

    data class AppearancePacket(
        val appearance: PaymentSheet.Appearance,
        val isSystemDarkTheme: Boolean?,
    )
}

internal object ErrorPaymentOptionResource : PaymentOptionResource {
    override suspend fun load(isSystemDarkTheme: Boolean?): Drawable {
        throw IllegalStateException("Must pass in an image loader to use icon() or iconPainter.")
    }

    @Composable
    override fun rememberPainter(): Painter {
        throw IllegalStateException("Must pass in an image loader to use icon() or iconPainter.")
    }
}
