package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import com.stripe.android.connect.PreviewConnectSDK
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
    internal val paddingX: Float? = null,
    internal val paddingY: Float? = null,
    internal val labelTypography: Typography.Style? = null,
) : Parcelable {

    // Deprecating old version without padding and label typography
    @Deprecated("Use builder instead")
    constructor(
        @ColorInt colorBackground: Int? = null,
        @ColorInt colorBorder: Int? = null,
        @ColorInt colorText: Int? = null,
    ) : this(
        colorBackground = colorBackground,
        colorBorder = colorBorder,
        colorText = colorText,
        paddingX = null,
        paddingY = null,
        labelTypography = null,
    )

    class Builder {
        @ColorInt private var colorBackground: Int? = null

        @ColorInt private var colorBorder: Int? = null

        @ColorInt private var colorText: Int? = null
        private var paddingX: Float? = null
        private var paddingY: Float? = null
        private var labelTypography: Typography.Style? = null

        fun colorBackground(@ColorInt colorBackground: Int?): Builder =
            apply { this.colorBackground = colorBackground }

        fun colorBorder(@ColorInt colorBorder: Int?): Builder =
            apply { this.colorBorder = colorBorder }

        fun colorText(@ColorInt colorText: Int?): Builder =
            apply { this.colorText = colorText }

        @PreviewConnectSDK
        fun paddingX(paddingX: Float?): Builder =
            apply { this.paddingX = paddingX }

        @PreviewConnectSDK
        fun paddingY(paddingY: Float?): Builder =
            apply { this.paddingY = paddingY }

        @PreviewConnectSDK
        fun labelTypography(labelTypography: Typography.Style?): Builder =
            apply { this.labelTypography = labelTypography }

        fun build(): Button {
            return Button(
                colorBackground = colorBackground,
                colorBorder = colorBorder,
                colorText = colorText,
                paddingX = paddingX,
                paddingY = paddingY,
                labelTypography = labelTypography
            )
        }
    }

    internal companion object {
        internal fun default() = Button()
    }
}
