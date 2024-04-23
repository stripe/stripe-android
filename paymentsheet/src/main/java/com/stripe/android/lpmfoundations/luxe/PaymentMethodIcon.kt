package com.stripe.android.lpmfoundations.luxe

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class PaymentMethodIcon : Parcelable {

    internal data class Resource(
        /**
         * This describes the image in the LPM selector.
         *
         * These can be found internally
         * [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0).
         * */
        @DrawableRes val iconResource: Int,
    ) : PaymentMethodIcon()

    internal data class Url(
        /** A light theme icon url. */
        val lightThemeIconUrl: String,

        /** An optional dark theme icon url. */
        val darkThemeIconUrl: String?,
    ) : PaymentMethodIcon()

    fun getNullableIconResource(): Int? {
        return when (this) {
            is Resource -> iconResource
            is Url -> null
        }
    }

    fun getNullableLightThemeIconUrl(): String? {
        return when (this) {
            is Url -> lightThemeIconUrl
            is Resource -> null
        }
    }

    fun getNullableDarkThemeIconUrl(): String? {
        return when (this) {
            is Url -> darkThemeIconUrl
            is Resource -> null
        }
    }

    companion object {
        fun create(
            @DrawableRes iconResource: Int,
            lightThemeIconUrl: String?,
            darkThemeIconUrl: String?
        ): PaymentMethodIcon {
            return if (lightThemeIconUrl == null) {
                Resource(iconResource = iconResource)
            } else {
                Url(
                    lightThemeIconUrl = lightThemeIconUrl,
                    darkThemeIconUrl = darkThemeIconUrl,
                )
            }
        }
    }
}
