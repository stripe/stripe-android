package com.stripe.android.connect

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager internal constructor(
    private val configuration: Configuration,
    private val fetchClientSecret: FetchClientSecretCallback,
) {
    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun update(@Suppress("UNUSED_PARAMETER") appearance: Appearance) {
        throw NotImplementedError("Appearance update functionality is not yet implemented")
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun logout() {
        throw NotImplementedError("Logout functionality is not yet implemented")
    }

    // Configuration

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable

    // Appearance classes

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Appearance(
        val colorsLight: Colors = Colors(),
        val colorsDark: Colors? = null,
        val cornerRadius: CornerRadius = CornerRadius(),
        val typography: Typography = Typography(),
        val buttonPrimaryLight: Button = Button(),
        val buttonPrimaryDark: Button? = null,
        val buttonSecondaryLight: Button = Button(),
        val buttonSecondaryDark: Button? = null,
        val badgeNeutralLight: Badge = Badge(),
        val badgeNeutralDark: Badge? = null,
        val badgeSuccessLight: Badge = Badge(),
        val badgeSuccessDark: Badge? = null,
        val badgeWarningLight: Badge = Badge(),
        val badgeWarningDark: Badge? = null,
        val badgeDangerLight: Badge = Badge(),
        val badgeDangerDark: Badge? = null
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Colors(
        @ColorInt val primary: Int? = null,
        @ColorInt val background: Int? = null,
        @ColorInt val text: Int? = null,
        @ColorInt val secondaryText: Int? = null,
        @ColorInt val danger: Int? = null,
        @ColorInt val border: Int? = null,
        @ColorInt val actionPrimaryText: Int? = null,
        @ColorInt val actionSecondaryText: Int? = null,
        @ColorInt val offsetBackground: Int? = null,
        @ColorInt val formBackground: Int? = null,
        @ColorInt val formHighlightBorder: Int? = null,
        @ColorInt val formAccent: Int? = null
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Button(
        @ColorInt val colorBackground: Int? = null,
        @ColorInt val colorBorder: Int? = null,
        @ColorInt val colorText: Int? = null
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Badge(
        @ColorInt val colorBackground: Int? = null,
        @ColorInt val colorText: Int? = null,
        @ColorInt val colorBorder: Int? = null
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class CornerRadius(
        val base: Float? = null,
        val button: Float? = null,
        val badge: Float? = null,
        val overlay: Float? = null,
        val form: Float? = null
    ) : Parcelable

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Typography(
        val fontFamily: String? = null,
        val fontSizeBase: Float? = null,
        val headingXl: Style? = null,
        val headingLg: Style? = null,
        val headingMd: Style? = null,
        val headingSm: Style? = null,
        val headingXs: Style? = null,
        val bodyMd: Style? = null,
        val bodySm: Style? = null,
        val labelMd: Style? = null,
        val labelSm: Style? = null
    ) : Parcelable {

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Style(
            val fontSize: Float? = null,
            val fontWeight: Int? = null,
            val textTransform: TextTransform = TextTransform.None
        ) : Parcelable
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class TextTransform {
        None,
        Uppercase,
        Lowercase,
        Capitalize
    }

    @PrivateBetaConnectSDK
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        private var instance: EmbeddedComponentManager? = null

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        fun init(
            configuration: Configuration,
            fetchClientSecret: FetchClientSecretCallback,
        ) {
            instance = EmbeddedComponentManager(
                configuration,
                fetchClientSecret,
            )
        }
    }
}
