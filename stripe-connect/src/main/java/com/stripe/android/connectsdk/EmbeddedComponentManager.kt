package com.stripe.android.connectsdk

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager internal constructor() {

    // TODO MXMOBILE-2760 - replace with actual implementation and remove the @Suppress annotations
    constructor(
        @Suppress("UNUSED_PARAMETER") activity: ComponentActivity,
        @Suppress("UNUSED_PARAMETER") configuration: Configuration,
        @Suppress("UNUSED_PARAMETER") fetchClientSecret: FetchClientSecretCallback,
    ) : this() {
        throw NotImplementedError("Not yet implemented")
    }

    @PrivateBetaConnectSDK
    fun presentAccountOnboarding() {
        throw NotImplementedError("Not yet implemented")
    }

    @PrivateBetaConnectSDK
    fun presentPayouts() {
        throw NotImplementedError("Not yet implemented")
    }

    @PrivateBetaConnectSDK
    fun update(
        // TODO MXMOBILE-2507 - replace with actual implementation and remove the @Suppress annotations
        @Suppress("UNUSED_PARAMETER") appearance: Appearance,
    ) {
        throw NotImplementedError("Not yet implemented")
    }

    @PrivateBetaConnectSDK
    fun logout() {
        throw NotImplementedError("Not yet implemented")
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
        val typography: Typography,
        val colors: Colors,
        val spacingUnit: Float? = null,
        val buttonPrimary: Button,
        val buttonSecondary: Button,
        val badgeNeutral: Badge,
        val badgeSuccess: Badge,
        val badgeWarning: Badge,
        val badgeDanger: Badge,
        val cornerRadius: CornerRadius
    ) : Parcelable {
        companion object {
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            val default = Appearance(
                typography = Typography(),
                colors = Colors(),
                spacingUnit = null,
                buttonPrimary = Button(),
                buttonSecondary = Button(),
                badgeNeutral = Badge(),
                badgeSuccess = Badge(),
                badgeWarning = Badge(),
                badgeDanger = Badge(),
                cornerRadius = CornerRadius()
            )
        }

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Typography(
            val font: String? = null,
            val fontSizeBase: Float? = null,
            val bodyMd: Style = Style(),
            val bodySm: Style = Style(),
            val headingXl: Style = Style(),
            val headingLg: Style = Style(),
            val headingMd: Style = Style(),
            val headingSm: Style = Style(),
            val headingXs: Style = Style(),
            val labelMd: Style = Style(),
            val labelSm: Style = Style()
        ) : Parcelable {

            @PrivateBetaConnectSDK
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Parcelize
            data class Style(
                val fontSize: Float? = null,
                val weight: String? = null,
                val textTransform: TextTransform? = null
            ) : Parcelable
        }

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class TextTransform {
            NONE,
            UPPERCASE,
            LOWERCASE,
            CAPITALIZE
        }

        @PrivateBetaConnectSDK
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        data class Colors(
            @ColorInt val primary: Int? = null,
            @ColorInt val text: Int? = null,
            @ColorInt val danger: Int? = null,
            @ColorInt val background: Int? = null,
            @ColorInt val secondaryText: Int? = null,
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
            val form: Float? = null,
            val button: Float? = null,
            val badge: Float? = null,
            val overlay: Float? = null
        ) : Parcelable
    }
}
