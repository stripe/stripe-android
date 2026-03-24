package com.stripe.android.connect.appearance

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * @param paddingX The horizontal padding of the button. If null the value will be derived from the spacingUnit.
 * @param paddingY The vertical padding of the button. If null the value will be derived from the spacingUnit.
 * @param labelTypography The label typography of the button. If null labelMd will be used.
 */
@Parcelize
@Poko
class ButtonDefaults(
    internal val paddingX: Float? = null,
    internal val paddingY: Float? = null,
    internal val labelTypography: Typography.Style? = null,
) : Parcelable {
    internal companion object {
        internal fun default() = ButtonDefaults()
    }
}
