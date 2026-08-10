package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp
import com.stripe.android.crypto.onramp.exception.SDKVersion
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.LinkAppearance
import com.stripe.android.model.CardBrand

/**
 * Configuration options required to initialize the Onramp flow.
 *
 * @property merchantDisplayName The display name to use for the merchant.
 * @property publishableKey The publishable key from the API dashboard to enable requests.
 * @property appearance Appearance settings for the PaymentSheet UI.
 * @property cryptoCustomerId The unique customer ID for crypto onramp.
 * @property googlePayConfig The configuration for Google Pay, if Google Pay is supported by the merchant.
 * @property samsungPayConfig The configuration for Samsung Pay, if Samsung Pay is supported by the merchant.
 * @property additionalSdkVersions Additional wrapper SDK versions to include in developer diagnostics.
 */
@ExperimentalCryptoOnramp
class OnrampConfiguration {
    private var merchantDisplayName: String? = null
    private var publishableKey: String? = null
    private var appearance: LinkAppearance? = null
    private var cryptoCustomerId: String? = null
    private var googlePayConfig: GooglePayPaymentMethodLauncher.Config? = null
    private var samsungPayConfig: SamsungPayConfig? = null
    private var additionalSdkVersions: List<SDKVersion> = emptyList()

    /**
     * Sets the display name of the merchant.
     */
    fun merchantDisplayName(merchantDisplayName: String) = apply {
        this.merchantDisplayName = merchantDisplayName
    }

    /**
     * Sets the publishable key of the merchant.
     */
    fun publishableKey(publishableKey: String) = apply {
        this.publishableKey = publishableKey
    }

    /**
     * Sets appearance settings for the payment sheet user interface presented by Stripe.
     * This does have a default appearance.
     */
    fun appearance(appearance: LinkAppearance) = apply {
        this.appearance = appearance
    }

    /**
     * Sets the unique crypto customer ID to use.
     */
    fun cryptoCustomerId(cryptoCustomerId: String?) = apply {
        this.cryptoCustomerId = cryptoCustomerId
    }

    /**
     * Sets the Google Pay configuration to use if Google Pay is supported by the merchant.
     */
    fun googlePayConfig(googlePayConfig: GooglePayPaymentMethodLauncher.Config) = apply {
        this.googlePayConfig = googlePayConfig
    }

    /**
     * Sets the Samsung Pay configuration to use if Samsung Pay is supported by the merchant.
     *
     * The client application must include Samsung Pay SDK 2.22.00 at runtime. Stripe does not
     * package or transitively depend on the Samsung Pay SDK.
     */
    fun samsungPayConfig(samsungPayConfig: SamsungPayConfig) = apply {
        this.samsungPayConfig = samsungPayConfig
    }

    /**
     * Configuration for collecting a payment method with Samsung Pay.
     *
     * @param serviceId The Samsung Pay in-app service ID assigned to the merchant.
     * @param merchantId An optional merchant identifier supplied to Samsung Pay.
     * @param merchantName The merchant name shown in the Samsung Pay sheet. When omitted, the
     * onramp merchant display name is used.
     * @param allowedCardBrands Card brands that may be selected in Samsung Pay.
     */
    @ExperimentalCryptoOnramp
    class SamsungPayConfig @JvmOverloads constructor(
        internal val serviceId: String,
        internal val merchantId: String? = null,
        internal val merchantName: String? = null,
        internal val allowedCardBrands: List<CardBrand> = listOf(
            CardBrand.Visa,
            CardBrand.MasterCard,
            CardBrand.AmericanExpress,
            CardBrand.Discover,
        ),
    )

    /**
     * Additional wrapper SDK versions to include in developer diagnostics, such as the
     * Stripe React Native SDK version. Do not include Stripe Android; it is always included
     * automatically.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun additionalSdkVersions(additionalSdkVersions: List<SDKVersion>) = apply {
        this.additionalSdkVersions = additionalSdkVersions
    }

    internal class State(
        val merchantDisplayName: String,
        val publishableKey: String,
        val appearance: LinkAppearance,
        val cryptoCustomerId: String? = null,
        val googlePayConfig: GooglePayPaymentMethodLauncher.Config? = null,
        val samsungPayConfig: SamsungPayConfig?,
        val additionalSdkVersions: List<SDKVersion> = emptyList()
    )

    internal fun build(): State {
        return State(
            merchantDisplayName = requireNotNull(merchantDisplayName) {
                "merchantDisplayName must not be null"
            },
            publishableKey = requireNotNull(publishableKey) {
                "publishableKey must not be null"
            },
            appearance = (appearance ?: LinkAppearance()).reduceLinkBranding(true),
            cryptoCustomerId = cryptoCustomerId,
            googlePayConfig = googlePayConfig,
            samsungPayConfig = samsungPayConfig,
            additionalSdkVersions = additionalSdkVersions
        )
    }
}
