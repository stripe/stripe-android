package com.stripe.android.connect.appearance

import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Form private constructor(
    @ColorInt internal val colorBackground: Int?,
    @ColorInt internal val highlightBorder: Int?,
    @ColorInt internal val accent: Int?,
    @ColorInt internal val placeholderTextColor: Int?,
    internal val inputFieldPaddingY: Float?,
    internal val inputFieldPaddingX: Float?,
) : Parcelable {

    class Builder {
        @ColorInt private var colorBackground: Int? = null

        @ColorInt private var highlightBorder: Int? = null

        @ColorInt private var accent: Int? = null

        @ColorInt private var placeholderTextColor: Int? = null
        private var inputFieldPaddingY: Float? = null
        private var inputFieldPaddingX: Float? = null

        fun colorBackground(@ColorInt colorBackground: Int?): Builder =
            apply { this.colorBackground = colorBackground }

        fun highlightBorder(@ColorInt highlightBorder: Int?): Builder =
            apply { this.highlightBorder = highlightBorder }

        fun accent(@ColorInt accent: Int?): Builder =
            apply { this.accent = accent }

        fun placeholderTextColor(@ColorInt placeholderTextColor: Int?): Builder =
            apply { this.placeholderTextColor = placeholderTextColor }

        fun inputFieldPaddingY(inputFieldPaddingY: Float): Builder =
            apply { this.inputFieldPaddingY = inputFieldPaddingY }

        fun inputFieldPaddingX(inputFieldPaddingX: Float): Builder =
            apply { this.inputFieldPaddingX = inputFieldPaddingX }

        fun build(): Form {
            return Form(
                colorBackground = colorBackground,
                highlightBorder = highlightBorder,
                accent = accent,
                placeholderTextColor = placeholderTextColor,
                inputFieldPaddingY = inputFieldPaddingY,
                inputFieldPaddingX = inputFieldPaddingX,
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
