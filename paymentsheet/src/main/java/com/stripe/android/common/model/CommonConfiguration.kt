package com.stripe.android.common.model

import android.os.Parcelable
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.validation.CustomerSessionClientSecretValidator
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.LinkController
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.TermsDisplay
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize
import com.stripe.android.paymentsheet.forms.generated.BillingDetailsCollectionConfigV1 as BillingDetailsCollectionConfig
import com.stripe.android.paymentsheet.forms.generated.BillingDetailsPresenceV1 as BillingDetailsPresence
import com.stripe.android.paymentsheet.forms.generated.CardBrandAcceptanceV1 as CardBrandAcceptance
import com.stripe.android.paymentsheet.forms.generated.CustomPaymentMethodConfigV1 as CustomPaymentMethodConfig
import com.stripe.android.paymentsheet.forms.generated.GooglePayConfigV1 as GooglePayConfig
import com.stripe.android.paymentsheet.forms.generated.LinkConfigV1 as LinkConfig
import com.stripe.android.paymentsheet.forms.generated.PaymentSheetConfigV1 as PaymentSheetConfig
import com.stripe.android.paymentsheet.forms.generated.WalletButtonsConfigV1 as WalletButtonsConfig

@Parcelize
internal data class CommonConfiguration(
    val merchantDisplayName: String,
    val customer: PaymentSheet.CustomerConfiguration?,
    val googlePay: PaymentSheet.GooglePayConfiguration?,
    val link: PaymentSheet.LinkConfiguration,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val preferredNetworks: List<CardBrand>,
    val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    val paymentMethodOrder: List<String>,
    val externalPaymentMethods: List<String>,
    val paymentMethodLayout: PaymentSheet.PaymentMethodLayout,
    val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
    internal val allowedCardFundingTypes: List<PaymentSheet.CardFundingType>,
    val customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
    val googlePlacesApiKey: String?,
    val linkAppearance: LinkAppearance.State? = null,
    val termsDisplay: Map<PaymentMethod.Type, TermsDisplay>,
    val walletButtons: PaymentSheet.WalletButtonsConfiguration?,
    val opensCardScannerAutomatically: Boolean,
    val userOverrideCountry: String?,
    val appearance: PaymentSheet.Appearance,
    val primaryButtonLabel: String? = null,
) : Parcelable {

    fun allowedCardFundingTypes(enabled: Boolean): List<PaymentSheet.CardFundingType> {
        if (enabled) return allowedCardFundingTypes
        return ConfigurationDefaults.allowedCardFundingTypes
    }

    fun validate(
        initializationMode: PaymentElementLoader.InitializationMode,
        isLiveMode: Boolean,
        @PaymentElementCallbackIdentifier callbackIdentifier: String,
        isTapToAddSupported: Boolean = true,
    ) {
        customerAndMerchantValidate()
        checkoutSessionValidate(initializationMode)
        externalPaymentMethodsValidate(isLiveMode)
        confirmationTokenValidate(isLiveMode, callbackIdentifier)
        if (isTapToAddSupported) {
            tapToAddValidate(callbackIdentifier)
        }

        customer?.accessType?.let { customerAccessType ->
            customerAccessTypeValidate(customerAccessType)
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @Suppress("ThrowsCount")
    private fun customerAndMerchantValidate() {
        when {
            merchantDisplayName.isBlank() -> {
                throw IllegalArgumentException(
                    "When a Configuration is passed to PaymentSheet," +
                        " the Merchant display name cannot be an empty string."
                )
            }
            customer?.id?.isBlank() == true -> {
                throw IllegalArgumentException(
                    "When a CustomerConfiguration is passed to PaymentSheet," +
                        " the Customer ID cannot be an empty string."
                )
            }
        }
    }

    @Suppress("ThrowsCount")
    private fun checkoutSessionValidate(initializationMode: PaymentElementLoader.InitializationMode) {
        if (initializationMode !is PaymentElementLoader.InitializationMode.CheckoutSession) return
        if (customer != null) {
            throw IllegalArgumentException(
                "configuration.customer must not be set when using CheckoutSession initialization mode. " +
                    "Customer information is provided by the checkout session."
            )
        }
        if (defaultBillingDetails?.email == null) {
            throw IllegalArgumentException(
                "configuration.defaultBillingDetails.email must be set when using CheckoutSession initialization mode."
            )
        }
        if (externalPaymentMethods.isNotEmpty()) {
            throw IllegalArgumentException(
                "configuration.externalPaymentMethods must not be set when using CheckoutSession initialization mode."
            )
        }
        if (customPaymentMethods.isNotEmpty()) {
            throw IllegalArgumentException(
                "configuration.customPaymentMethods must not be set when using CheckoutSession initialization mode."
            )
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @Suppress("ThrowsCount")
    private fun externalPaymentMethodsValidate(isLiveMode: Boolean) {
        externalPaymentMethods.forEach { externalPaymentMethod ->
            if (!externalPaymentMethod.startsWith("external_") && isLiveMode.not()) {
                throw IllegalArgumentException(
                    "External payment method '$externalPaymentMethod' does not start with 'external_'. " +
                        "All external payment methods must use the 'external_' prefix. " +
                        "See https://docs.stripe.com/payments/external-payment-methods?platform=android#available-" +
                        "external-payment-methods"
                )
            }
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @Suppress("ThrowsCount")
    private fun confirmationTokenValidate(
        isLiveMode: Boolean,
        @PaymentElementCallbackIdentifier callbackIdentifier: String
    ) {
        if (
            PaymentElementCallbackReferences[callbackIdentifier]?.createIntentWithConfirmationTokenCallback != null &&
            customer?.accessType is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey &&
            isLiveMode.not()
        ) {
            throw IllegalArgumentException(
                "createIntentWithConfirmationTokenCallback must be used with CustomerSession."
            )
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @OptIn(TapToAddPreview::class)
    private fun tapToAddValidate(
        @PaymentElementCallbackIdentifier callbackIdentifier: String
    ) {
        if (
            PaymentElementCallbackReferences[callbackIdentifier]?.createCardPresentSetupIntentCallback != null &&
            billingDetailsCollectionConfiguration.collectsAnything
        ) {
            throw IllegalArgumentException(
                "Tap to Add does not supporting collecting billing fields with " +
                    "BillingDetailsCollectionConfiguration. To use Tap to Add, set all " +
                    "BillingDetailsCollectionConfiguration config options to 'Automatic'."
            )
        }
    }

    private fun customerAccessTypeValidate(customerAccessType: PaymentSheet.CustomerAccessType) {
        when (customerAccessType) {
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> {
                legacyCustomerEphemeralKeyValidate(customerAccessType)
            }
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                customerSessionValidate(customerAccessType)
            }
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @Suppress("ThrowsCount")
    private fun customerSessionValidate(customerAccessType: PaymentSheet.CustomerAccessType.CustomerSession) {
        val result = CustomerSessionClientSecretValidator
            .validate(customerAccessType.customerSessionClientSecret)

        when (result) {
            is CustomerSessionClientSecretValidator.Result.Error.Empty -> {
                throw IllegalArgumentException(
                    "When a CustomerConfiguration is passed to PaymentSheet, " +
                        "the customerSessionClientSecret cannot be an empty string."
                )
            }
            is CustomerSessionClientSecretValidator.Result.Error.LegacyEphemeralKey -> {
                throw IllegalArgumentException(
                    "Argument looks like an Ephemeral Key secret, but expecting a CustomerSession client " +
                        "secret. See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
                )
            }
            is CustomerSessionClientSecretValidator.Result.Error.UnknownKey -> {
                throw IllegalArgumentException(
                    "Argument does not look like a CustomerSession client secret. " +
                        "See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
                )
            }
            is CustomerSessionClientSecretValidator.Result.Valid -> Unit
        }
    }

    // These exception messages are not localized as they are not intended to be displayed to a user.
    @Suppress("ThrowsCount")
    private fun legacyCustomerEphemeralKeyValidate(
        customerAccessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey
    ) {
        if (customerAccessType.ephemeralKeySecret != customer?.ephemeralKeySecret) {
            throw IllegalArgumentException(
                "Conflicting ephemeralKeySecrets between CustomerConfiguration " +
                    "and CustomerConfiguration.customerAccessType"
            )
        } else if (customerAccessType.ephemeralKeySecret.isBlank() || customer.ephemeralKeySecret.isBlank()) {
            throw IllegalArgumentException(
                "When a CustomerConfiguration is passed to PaymentSheet, " +
                    "the ephemeralKeySecret cannot be an empty string."
            )
        } else if (
            customerAccessType.ephemeralKeySecret.isEKClientSecretValid().not() ||
            customer.ephemeralKeySecret.isEKClientSecretValid().not()
        ) {
            throw IllegalArgumentException(
                "`ephemeralKeySecret` format does not match expected client secret formatting"
            )
        }
    }
}

internal fun PaymentSheet.Configuration.asCommonConfiguration(): CommonConfiguration = CommonConfiguration(
    merchantDisplayName = merchantDisplayName,
    customer = customer,
    googlePay = googlePay,
    defaultBillingDetails = defaultBillingDetails,
    shippingDetails = shippingDetails,
    allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
    allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    preferredNetworks = preferredNetworks,
    allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
    paymentMethodOrder = paymentMethodOrder,
    externalPaymentMethods = externalPaymentMethods,
    paymentMethodLayout = paymentMethodLayout,
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = customPaymentMethods,
    link = link,
    googlePlacesApiKey = googlePlacesApiKey,
    termsDisplay = termsDisplay,
    walletButtons = walletButtons,
    opensCardScannerAutomatically = opensCardScannerAutomatically,
    userOverrideCountry = userOverrideCountry,
    appearance = appearance,
    allowedCardFundingTypes = allowedCardFundingTypes,
    primaryButtonLabel = primaryButtonLabel,
)

internal fun EmbeddedPaymentElement.Configuration.asCommonConfiguration(): CommonConfiguration = CommonConfiguration(
    merchantDisplayName = merchantDisplayName,
    customer = customer,
    googlePay = googlePay,
    defaultBillingDetails = defaultBillingDetails,
    shippingDetails = shippingDetails,
    allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
    allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    preferredNetworks = preferredNetworks,
    allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
    paymentMethodOrder = paymentMethodOrder,
    externalPaymentMethods = externalPaymentMethods,
    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = customPaymentMethods,
    link = link,
    googlePlacesApiKey = null,
    termsDisplay = termsDisplay,
    walletButtons = null,
    opensCardScannerAutomatically = opensCardScannerAutomatically,
    userOverrideCountry = userOverrideCountry,
    appearance = appearance,
    allowedCardFundingTypes = allowedCardFundingTypes,
)

internal fun LinkController.Configuration.State.asCommonConfiguration(): CommonConfiguration = CommonConfiguration(
    merchantDisplayName = merchantDisplayName,
    customer = null,
    googlePay = null,
    defaultBillingDetails = defaultBillingDetails,
    shippingDetails = null,
    allowsDelayedPaymentMethods = ConfigurationDefaults.allowsDelayedPaymentMethods,
    allowsPaymentMethodsRequiringShippingAddress = ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
    preferredNetworks = ConfigurationDefaults.preferredNetworks,
    allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
    paymentMethodOrder = ConfigurationDefaults.paymentMethodOrder,
    externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
    link = PaymentSheet.LinkConfiguration(
        display = PaymentSheet.LinkConfiguration.Display.Automatic,
        collectMissingBillingDetailsForExistingPaymentMethods = true,
        allowUserEmailEdits = allowUserEmailEdits,
        allowLogOut = allowLogout,
        disallowFundingSourceCreation = emptySet(),
    ),
    googlePlacesApiKey = null,
    linkAppearance = linkAppearance,
    termsDisplay = emptyMap(),
    walletButtons = null,
    opensCardScannerAutomatically = false,
    userOverrideCountry = null,
    appearance = PaymentSheet.Appearance(),
    allowedCardFundingTypes = ConfigurationDefaults.allowedCardFundingTypes,
)

internal fun CommonConfiguration.toMobileSessionConfig(): PaymentSheetConfig = PaymentSheetConfig(
    merchantCountryCode = googlePay?.countryCode,
    allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
    allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
    googlePay = googlePay?.toMobileSessionConfig(),
    link = link.toMobileSessionConfig(),
    returnUrlProvided = false,
    merchantDisplayNameProvided = merchantDisplayName.isNotEmpty(),
    customerConfigured = customer != null,
    customerAccessType = customer?.accessType?.analyticsValue,
    customApiClient = false,
    defaultBillingDetails = defaultBillingDetails.toMobileSessionPresence(),
    shippingDetailsProvided = shippingDetails != null,
    savePaymentMethodOptInBehavior = "automatic",
    primaryButtonLabelProvided = primaryButtonLabel != null,
    appearanceCustomized = appearance != PaymentSheet.Appearance(),
    userInterfaceStyle = "automatic",
    preferredNetworks = preferredNetworks.map { it.code },
    billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration.toMobileSessionConfig(),
    externalPaymentMethods = externalPaymentMethods,
    customPaymentMethodIds = customPaymentMethods.map { it.id },
    customPaymentMethods = customPaymentMethods.map { it.toMobileSessionConfig() },
    externalPaymentMethodHandlerProvided = externalPaymentMethods.isNotEmpty(),
    customPaymentMethodHandlerProvided = customPaymentMethods.isNotEmpty(),
    paymentMethodOrder = paymentMethodOrder,
    paymentMethodLayout = paymentMethodLayout.name.lowercase(),
    cardBrandAcceptance = cardBrandAcceptance.toMobileSessionValue(),
    allowedCardFundingTypes = allowedCardFundingTypes.map { it.name.lowercase() },
    termsDisplay = termsDisplay.mapKeys { it.key.code }.mapValues { it.value.name.lowercase() },
    allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
    removeSavedPaymentMethodMessageProvided = false,
    opensCardScannerAutomatically = opensCardScannerAutomatically,
    disableWalletPaymentMethodFiltering = false,
    linkPaymentMethodsOnly = false,
    walletButtons = walletButtons.toMobileSessionConfig(),
    googlePlacesApiKeyProvided = googlePlacesApiKey != null,
    userOverrideCountry = userOverrideCountry,
)

private fun PaymentSheet.GooglePayConfiguration.toMobileSessionConfig(): GooglePayConfig = GooglePayConfig(
    merchantCountryCode = countryCode,
    environment = environment.name.lowercase(),
    currencyCode = currencyCode,
    amountProvided = amount != null,
    labelProvided = label != null,
    buttonType = buttonType.name.lowercase(),
    additionalEnabledNetworks = additionalEnabledNetworks,
)

private fun PaymentSheet.LinkConfiguration.toMobileSessionConfig(): LinkConfig = LinkConfig(
    display = display.analyticsValue,
    disabledFundingSources = disallowFundingSourceCreation.sorted(),
    collectMissingBillingDetailsForExistingPaymentMethods = collectMissingBillingDetailsForExistingPaymentMethods,
    allowUserEmailEdits = allowUserEmailEdits,
    allowLogout = allowLogOut,
)

private fun PaymentSheet.BillingDetails?.toMobileSessionPresence(): BillingDetailsPresence = BillingDetailsPresence(
    name = this?.name != null,
    email = this?.email != null,
    phone = this?.phone != null,
    address = this?.address != null,
    addressCity = this?.address?.city != null,
    addressLine1 = this?.address?.line1 != null,
    addressLine2 = this?.address?.line2 != null,
    addressPostalCode = this?.address?.postalCode != null,
    addressState = this?.address?.state != null,
    addressCountryCode = this?.address?.country,
)

private fun PaymentSheet.BillingDetailsCollectionConfiguration.toMobileSessionConfig():
    BillingDetailsCollectionConfig = BillingDetailsCollectionConfig(
        name = name.name.lowercase(),
        phone = phone.name.lowercase(),
        email = email.name.lowercase(),
        address = address.name.lowercase(),
        attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
        allowedCountries = allowedBillingCountries.sorted(),
    )

private fun PaymentSheet.CustomPaymentMethod.toMobileSessionConfig(): CustomPaymentMethodConfig =
    CustomPaymentMethodConfig(
        id = id,
        subtitleProvided = subtitle != null,
        disableBillingDetailCollection = disableBillingDetailCollection,
    )

private fun PaymentSheet.WalletButtonsConfiguration?.toMobileSessionConfig(): WalletButtonsConfig = WalletButtonsConfig(
    willDisplayExternally = this?.willDisplayExternally == true,
    paymentElement = this?.visibility?.paymentElement.toMobileSessionPaymentElementConfig(),
    walletButtonsView = this?.visibility?.walletButtonsView.toMobileSessionWalletButtonsViewConfig(),
)

private fun Map<
    PaymentSheet.WalletButtonsConfiguration.Wallet,
    PaymentSheet.WalletButtonsConfiguration.PaymentElementVisibility,
>?.toMobileSessionPaymentElementConfig(): Map<String, String> = toMobileSessionConfig { it.name.lowercase() }

private fun Map<
    PaymentSheet.WalletButtonsConfiguration.Wallet,
    PaymentSheet.WalletButtonsConfiguration.WalletButtonsViewVisibility,
>?.toMobileSessionWalletButtonsViewConfig(): Map<String, String> = toMobileSessionConfig { it.name.lowercase() }

private fun <T> Map<PaymentSheet.WalletButtonsConfiguration.Wallet, T>?.toMobileSessionConfig(
    value: (T) -> String,
): Map<String, String> = orEmpty().mapKeys {
    it.key.mobileSessionValue
}.mapValues {
    value(it.value)
}

private val PaymentSheet.WalletButtonsConfiguration.Wallet.mobileSessionValue: String
    get() = when (this) {
        PaymentSheet.WalletButtonsConfiguration.Wallet.Link -> "link"
        PaymentSheet.WalletButtonsConfiguration.Wallet.GooglePay -> "google_pay"
    }

private fun PaymentSheet.CardBrandAcceptance.toMobileSessionValue(): CardBrandAcceptance = when (this) {
    PaymentSheet.CardBrandAcceptance.All -> CardBrandAcceptance()
    is PaymentSheet.CardBrandAcceptance.Allowed -> CardBrandAcceptance(
        filter = "allowed",
        brands = brands.map { it.name.lowercase() },
    )
    is PaymentSheet.CardBrandAcceptance.Disallowed -> CardBrandAcceptance(
        filter = "disallowed",
        brands = brands.map { it.name.lowercase() },
    )
}

private fun String.isEKClientSecretValid(): Boolean {
    return Regex(EK_CLIENT_SECRET_VALID_REGEX_PATTERN).matches(this)
}

private const val EK_CLIENT_SECRET_VALID_REGEX_PATTERN = "^(ek|uk)_[^_](.)+$"

internal fun CommonConfiguration.containsVolatileDifferences(
    other: CommonConfiguration
): Boolean {
    return toVolatileConfiguration() != other.toVolatileConfiguration()
}

/**
 * Creates a subset of the [CommonConfiguration] values that affect the behavior of [PaymentSelection].
 */
private fun CommonConfiguration.toVolatileConfiguration(): VolatileCommonConfiguration {
    return VolatileCommonConfiguration(
        defaultBillingDetails = defaultBillingDetails,
        billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        cardBrandAcceptance = cardBrandAcceptance,
    )
}

private data class VolatileCommonConfiguration(
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
)
