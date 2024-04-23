package com.stripe.android.lpmfoundations.luxe

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class PaymentMethodIcon : Parcelable {
    data class UrlOrResource(
        /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
        @DrawableRes val iconResource: Int,

        /** A light theme icon url. */
        val lightThemeIconUrl: String,

        /** An optional dark theme icon url. */
        val darkThemeIconUrl: String?,
    ): PaymentMethodIcon()

    data class ResourceOnly(
        /** This describes the image in the LPM selector.  These can be found internally [here](https://www.figma.com/file/2b9r3CJbyeVAmKi1VHV2h9/Mobile-Payment-Element?node-id=1128%3A0) */
        @DrawableRes val iconResource: Int,
    ): PaymentMethodIcon()

    data class UrlsOnly(
        /** A light theme icon url. */
        val lightThemeIconUrl: String,

        /** An optional dark theme icon url. */
        val darkThemeIconUrl: String?,
    ): PaymentMethodIcon()

    fun getNullableIconResource(): Int? {
        return when (this) {
            is UrlOrResource -> iconResource
            is ResourceOnly -> iconResource
            is UrlsOnly -> null
        }
    }

    fun getNullableLightThemeIconUrl(): String? {
        return when (this) {
            is UrlOrResource -> lightThemeIconUrl
            is UrlsOnly -> lightThemeIconUrl
            is ResourceOnly -> null
        }
    }

    fun getNullableDarkThemeIconUrl(): String? {
        return when (this) {
            is UrlOrResource -> darkThemeIconUrl
            is UrlsOnly -> darkThemeIconUrl
            is ResourceOnly -> null
        }
    }

    companion object {
        fun create(
            @DrawableRes iconResource: Int,
            lightThemeIconUrl: String?,
            darkThemeIconUrl: String?
        ): PaymentMethodIcon {
            return if (lightThemeIconUrl == null) {
                ResourceOnly(iconResource = iconResource)
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