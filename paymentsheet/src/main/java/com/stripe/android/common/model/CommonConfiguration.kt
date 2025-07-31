package com.stripe.android.common.model

import android.os.Parcelable
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.validation.CustomerSessionClientSecretValidator
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.CustomPaymentMethod
import com.stripe.android.elements.payment.EmbeddedPaymentElement
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.LinkConfiguration
import com.stripe.android.link.LinkController
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CommonConfiguration(
    val merchantDisplayName: String,
    val customer: CustomerConfiguration?,
    val googlePay: GooglePayConfiguration?,
    val link: LinkConfiguration,
    val defaultBillingDetails: BillingDetails?,
    val shippingDetails: AddressDetails?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    val preferredNetworks: List<CardBrand>,
    val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    val paymentMethodOrder: List<String>,
    val externalPaymentMethods: List<String>,
    val cardBrandAcceptance: CardBrandAcceptance,
    val customPaymentMethods: List<CustomPaymentMethod>,
    val shopPayConfiguration: PaymentSheet.ShopPayConfiguration?,
    val googlePlacesApiKey: String?,
) : Parcelable {

    fun validate(isLiveMode: Boolean) {
        customerAndMerchantValidate()
        externalPaymentMethodsValidate(isLiveMode)

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
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = customPaymentMethods,
    link = link,
    shopPayConfiguration = shopPayConfiguration,
    googlePlacesApiKey = googlePlacesApiKey,
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
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = customPaymentMethods,
    link = link,
    shopPayConfiguration = null,
    googlePlacesApiKey = null,
)

internal fun LinkController.Configuration.asCommonConfiguration(): CommonConfiguration = CommonConfiguration(
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
    cardBrandAcceptance = cardBrandAcceptance,
    customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
    link = LinkConfiguration(
        display = LinkConfiguration.Display.Automatic,
        collectMissingBillingDetailsForExistingPaymentMethods = true,
        allowUserEmailEdits = allowUserEmailEdits,
    ),
    shopPayConfiguration = null,
    googlePlacesApiKey = null,
)

private fun String.isEKClientSecretValid(): Boolean {
    return Regex(EK_CLIENT_SECRET_VALID_REGEX_PATTERN).matches(this)
}

private const val EK_CLIENT_SECRET_VALID_REGEX_PATTERN = "^ek_[^_](.)+$"

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
    val defaultBillingDetails: BillingDetails?,
    val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
    val cardBrandAcceptance: CardBrandAcceptance,
)
