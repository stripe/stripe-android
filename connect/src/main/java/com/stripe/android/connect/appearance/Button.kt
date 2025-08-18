package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * @param colorBackground The background color of the button. If null the default will be used.
 * @param colorBorder The border color of the button. If null the default will be used.
 * @param colorText The text color of the button. If null the default will be used.
 */
@Parcelize
@Poko
class Button(
    @ColorInt internal val colorBackground: Int? = null,
    @ColorInt internal val colorBorder: Int? = null,
    @ColorInt internal val colorText: Int? = null,
) : Parcelable {
    internal companion object {
        internal fun default() = Button()
    }
}
