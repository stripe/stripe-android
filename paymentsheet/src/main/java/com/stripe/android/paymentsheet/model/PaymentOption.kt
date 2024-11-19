package com.stripe.android.paymentsheet.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.uicore.image.rememberDrawablePainter

/**
 * The customer's selected payment option.
 */
data class PaymentOption internal constructor(
    /**
     * The drawable resource id of the icon that represents the payment option.
     */
    @Deprecated("Please use icon() instead.")
    val drawableResourceId: Int,
    /**
     * A label that describes the payment option.
     *
     * For example, "···· 4242" for a Visa ending in 4242.
     */
    val label: String,
    private val imageLoader: suspend () -> Drawable,
) {
    @Deprecated("Not intended for public use.")
    constructor(
        @DrawableRes
        drawableResourceId: Int,
        label: String
    ) : this(
        drawableResourceId = drawableResourceId,
        label = label,
        imageLoader = errorImageLoader,
    )

    /**
     * A [Painter] to draw the icon associated with this [PaymentOption].
     */
    val iconPainter: Painter
        @Composable
        get() = rememberDrawablePainter(icon())

    /**
     * Fetches the icon associated with this [PaymentOption].
     */
    fun icon(): Drawable {
        return DelegateDrawable(
            imageLoader = imageLoader,
        )
    }
}

private val errorImageLoader: suspend () -> Drawable = {
    throw IllegalStateException("Must pass in an image loader to use icon() or iconPainter.")
}
