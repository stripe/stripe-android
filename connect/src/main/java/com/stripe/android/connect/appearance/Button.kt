package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
@Poko
class Button(
    /**
     * The background color of the button. If null the default will be used.
     */
    @ColorInt val colorBackground: Int? = null,

    /**
     * The border color of the button. If null the default will be used.
     */
    @ColorInt val colorBorder: Int? = null,

    /**
     * The text color of the button. If null the default will be used.
     */
    @ColorInt val colorText: Int? = null,
) : Parcelable {
    internal companion object {
        internal val default = Button()
    }
}
