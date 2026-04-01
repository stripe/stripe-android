package com.stripe.android.connect.appearance

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * @param paddingX The horizontal padding of the badge. If null the value will be derived from the spacingUnit.
 * @param paddingY The vertical padding of the badge. If null the value will be derived from the spacingUnit.
 * @param labelTypography The label typography of the badge. If null labelSm will be used.
 */
@Parcelize
@Poko
class BadgeDefaults(
    internal val paddingX: Float?,
    internal val paddingY: Float?,
    internal val labelTypography: Typography.Style?
) : Parcelable {

    class Builder {
        private var paddingX: Float? = null
        private var paddingY: Float? = null
        private var labelTypography: Typography.Style? = null

        fun paddingX(paddingX: Float?): Builder =
            apply { this.paddingX = paddingX }

        fun paddingY(paddingY: Float?): Builder =
            apply { this.paddingY = paddingY }

        fun labelTypography(labelTypography: Typography.Style?): Builder =
            apply { this.labelTypography = labelTypography }

        fun build(): BadgeDefaults {
            return BadgeDefaults(paddingX = paddingX, paddingY = paddingY, labelTypography = labelTypography)
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
