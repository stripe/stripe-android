package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Action private constructor(
    @ColorInt internal val colorText: Int?,
    internal val textTransform: TextTransform?
) : Parcelable {

    class Builder {
        @ColorInt private var colorText: Int? = null
        private var textTransform: TextTransform = TextTransform.None

        fun colorText(@ColorInt colorText: Int?): Builder =
            apply { this.colorText = colorText }

        fun textTransform(textTransform: TextTransform): Builder =
            apply { this.textTransform = textTransform }

        fun build(): Action {
            return Action(colorText = colorText, textTransform = textTransform)
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
