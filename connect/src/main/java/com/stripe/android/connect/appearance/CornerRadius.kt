package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
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
        fun setBase(base: Float?) =
            apply { this.base = base }

        /**
         * The corner radius used specifically for buttons in dp.
         */
        fun setButton(button: Float?) =
            apply { this.button = button }

        /**
         * The corner radius used specifically for badges in dp.
         */
        fun setBadge(badge: Float?) =
            apply { this.badge = badge }

        /**
         * The corner radius used for overlays in dp.
         */
        fun setOverlay(overlay: Float?) =
            apply { this.overlay = overlay }

        /**
         * The corner radius used for form elements in dp.
         */
        fun setForm(form: Float?) =
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
