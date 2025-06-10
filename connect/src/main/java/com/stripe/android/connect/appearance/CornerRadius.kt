package com.stripe.android.connect.appearance

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class CornerRadius private constructor(
    internal val base: Float?,
    internal val button: Float?,
    internal val badge: Float?,
    internal val overlay: Float?,
    internal val form: Float?
) : Parcelable {

    class Builder {
        private var base: Float? = null
        private var button: Float? = null
        private var badge: Float? = null
        private var overlay: Float? = null
        private var form: Float? = null

        /**
         * The general border radius used throughout the components in dp.
         */
        fun base(base: Float?): Builder =
            apply { this.base = base }

        /**
         * The corner radius used specifically for buttons in dp.
         */
        fun button(button: Float?): Builder =
            apply { this.button = button }

        /**
         * The corner radius used specifically for badges in dp.
         */
        fun badge(badge: Float?): Builder =
            apply { this.badge = badge }

        /**
         * The corner radius used for overlays in dp.
         */
        fun overlay(overlay: Float?): Builder =
            apply { this.overlay = overlay }

        /**
         * The corner radius used for form elements in dp.
         */
        fun form(form: Float?): Builder =
            apply { this.form = form }

        fun build(): CornerRadius {
            return CornerRadius(
                base = base,
                button = button,
                badge = badge,
                overlay = overlay,
                form = form
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
