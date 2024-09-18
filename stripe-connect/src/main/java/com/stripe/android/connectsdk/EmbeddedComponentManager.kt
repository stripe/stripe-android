package com.stripe.android.connectsdk

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager private constructor(
    private val activity: ComponentActivity,
    private val configuration: Configuration,
    internal val fetchClientSecret: FetchClientSecretCallback,
) {

    @PrivateBetaConnectSDK
    fun presentAccountOnboarding() {
        val intent = EmbeddedComponentActivity.newIntent(
            activity,
            EmbeddedComponent.AccountOnboarding,
            configuration,
        )
        activity.startActivity(intent)
    }

    @PrivateBetaConnectSDK
    fun presentPayouts() {
        val intent = EmbeddedComponentActivity.newIntent(
            activity,
            EmbeddedComponent.Payouts,
            configuration,
        )
        activity.startActivity(intent)
    }

    @PrivateBetaConnectSDK
    fun update(appearance: Appearance) {
        throw NotImplementedError("Not yet implemented")
    }

    @PrivateBetaConnectSDK
    fun logout() {
        throw NotImplementedError("Not yet implemented")
    }

    companion object {
        var instance: EmbeddedComponentManager? = null

        fun init(
            activity: ComponentActivity,
            configuration: Configuration,
            fetchClientSecret: FetchClientSecretCallback,
        ): EmbeddedComponentManager {
            instance = EmbeddedComponentManager(
                activity = activity,
                configuration = configuration,
                fetchClientSecret = fetchClientSecret
            )
            return instance!!
        }
    }

    // Configuration

    @Parcelize
    data class Configuration(
        val publishableKey: String,
    ) : Parcelable

    // Appearance classes

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
    ): Parcelable {
        companion object {
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
        ): Parcelable {
            @Parcelize
            data class Style(
                val fontSize: Float? = null,
                val weight: String? = null,
                val textTransform: TextTransform? = null
            ): Parcelable
        }

        enum class TextTransform {
            NONE,
            UPPERCASE,
            LOWERCASE,
            CAPITALIZE
        }

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
        ): Parcelable

        @Parcelize
        data class Button(
            @ColorInt val colorBackground: Int? = null,
            @ColorInt val colorBorder: Int? = null,
            @ColorInt val colorText: Int? = null
        ): Parcelable

        @Parcelize
        data class Badge(
            @ColorInt val colorBackground: Int? = null,
            @ColorInt val colorText: Int? = null,
            @ColorInt val colorBorder: Int? = null
        ): Parcelable

        @Parcelize
        data class CornerRadius(
            val base: Float? = null,
            val form: Float? = null,
            val button: Float? = null,
            val badge: Float? = null,
            val overlay: Float? = null
        ): Parcelable
    }
}