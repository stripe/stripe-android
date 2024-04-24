package com.stripe.android.lpmfoundations.luxe

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class PaymentMethodIcon : Parcelable {

    abstract val iconResource: Int?
    abstract val lightThemeIconUrl: String?
    abstract val darkThemeIconUrl: String?

    internal data class Resource(
        /**
         * This describes the image in the LPM selector.
         *
         * These can be found internally
         * [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0).
         * */
        @DrawableRes override val iconResource: Int,
    ) : PaymentMethodIcon() {
        override val lightThemeIconUrl: String?
            get() = null
        override val darkThemeIconUrl: String?
            get() = null
    }

    internal data class Url(
        /** A light theme icon url. */
        override val lightThemeIconUrl: String,

        /** An optional dark theme icon url. */
        override val darkThemeIconUrl: String?,
    ) : PaymentMethodIcon() {
        override val iconResource: Int?
            get() = null
    }

    internal data class UrlOrResource(
        /**
         * This describes the image in the LPM selector.
         *
         * These can be found internally
         * [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0).
         * */
        @DrawableRes override val iconResource: Int,

        /** A light theme icon url. */
        override val lightThemeIconUrl: String,

        /** An optional dark theme icon url. */
        override val darkThemeIconUrl: String?,
    ) : PaymentMethodIcon()

    companion object {
        fun create(
            @DrawableRes iconResource: Int,
            lightThemeIconUrl: String?,
            darkThemeIconUrl: String?
        ): PaymentMethodIcon {
            return if (lightThemeIconUrl == null) {
                Resource(iconResource = iconResource)
            } else {
                UrlOrResource(
                    iconResource = iconResource,
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
                )
            }
        }
    }
}
