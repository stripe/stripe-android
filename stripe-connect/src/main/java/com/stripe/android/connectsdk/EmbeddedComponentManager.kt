package com.stripe.android.connectsdk

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import com.stripe.android.connectsdk.EmbeddedComponentActivity.EmbeddedComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmbeddedComponentManager private constructor(
    private val activity: ComponentActivity,
    private val configuration: Configuration,
    initialAppearance: AppearanceVariables = AppearanceVariables(),
    internal val fetchClientSecret: FetchClientSecretCallback,
) {

    private var _appearance = MutableStateFlow(initialAppearance)
    internal var appearance: StateFlow<AppearanceVariables> = _appearance

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
    fun update(appearance: AppearanceVariables) {
        _appearance.value = appearance
    }

    @PrivateBetaConnectSDK
    fun logout() {
        throw NotImplementedError("Not yet implemented")
    }

    internal fun buildInitParams(): StripeConnectParams {
        return StripeConnectParams(
            locale = null
        )
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

    @Serializable
    data class AppearanceOptions(
        val variables: AppearanceVariables? = null
    )

    @Parcelize
    @Serializable
    data class AppearanceVariables(
        val fontFamily: String? = null,
        val fontSizeBase: String? = null,
        val spacingUnit: String? = null,
        val borderRadius: String? = null,
        val colorPrimary: String? = null,
        val colorBackground: String? = null,
        val colorText: String? = null,
        val colorDanger: String? = null,
        val buttonPrimaryColorBackground: String? = null,
        val buttonPrimaryColorBorder: String? = null,
        val buttonPrimaryColorText: String? = null,
        val buttonSecondaryColorBackground: String? = null,
        val buttonSecondaryColorBorder: String? = null,
        val buttonSecondaryColorText: String? = null,
        val colorSecondaryText: String? = null,
        val actionPrimaryColorText: String? = null,
        val actionSecondaryColorText: String? = null,
        val badgeNeutralColorBackground: String? = null,
        val badgeNeutralColorText: String? = null,
        val badgeNeutralColorBorder: String? = null,
        val badgeSuccessColorBackground: String? = null,
        val badgeSuccessColorText: String? = null,
        val badgeSuccessColorBorder: String? = null,
        val badgeWarningColorBackground: String? = null,
        val badgeWarningColorText: String? = null,
        val badgeWarningColorBorder: String? = null,
        val badgeDangerColorBackground: String? = null,
        val badgeDangerColorText: String? = null,
        val badgeDangerColorBorder: String? = null,
        val offsetBackgroundColor: String? = null,
        val formBackgroundColor: String? = null,
        val colorBorder: String? = null,
        val formHighlightColorBorder: String? = null,
        val formAccentColor: String? = null,
        val buttonBorderRadius: String? = null,
        val formBorderRadius: String? = null,
        val badgeBorderRadius: String? = null,
        val overlayBorderRadius: String? = null,
        val overlayZIndex: Int? = null,
        val bodyMdFontSize: String? = null,
        val bodyMdFontWeight: String? = null,
        val bodySmFontSize: String? = null,
        val bodySmFontWeight: String? = null,
        val headingXlFontSize: String? = null,
        val headingXlFontWeight: String? = null,
        val headingXlTextTransform: String? = null,
        val headingLgFontSize: String? = null,
        val headingLgFontWeight: String? = null,
        val headingLgTextTransform: String? = null,
        val headingMdFontSize: String? = null,
        val headingMdFontWeight: String? = null,
        val headingMdTextTransform: String? = null,
        val headingSmFontSize: String? = null,
        val headingSmFontWeight: String? = null,
        val headingSmTextTransform: String? = null,
        val headingXsFontSize: String? = null,
        val headingXsFontWeight: String? = null,
        val headingXsTextTransform: String? = null,
        val labelMdFontSize: String? = null,
        val labelMdFontWeight: String? = null,
        val labelMdTextTransform: String? = null,
        val labelSmFontSize: String? = null,
        val labelSmFontWeight: String? = null,
        val labelSmTextTransform: String? = null
    ) : Parcelable

    @Serializable
    data class StripeConnectParams(
        /**
         * Appearance options for the Connect instance.
         * @see https://stripe.com/docs/connect/customize-connect-embedded-components
         */
        val appearance: AppearanceOptions? = null,

        /**
         * The locale to use for the Connect instance.
         */
        val locale: String? = null,

//        /**
//         * An array of custom fonts, which embedded components created from a ConnectInstance can use.
//         */
//        val fonts: List<Any?>? = null ,
    )
}