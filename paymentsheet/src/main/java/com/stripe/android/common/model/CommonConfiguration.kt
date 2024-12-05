package com.stripe.android.common.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.common.validation.CustomerSessionClientSecretValidator
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY)
const val IsEKClientSecretValidRegexPattern = "^ek_[^_](.)+$"

@Parcelize
internal data class CommonConfiguration(
    val merchantDisplayName: String,
    val customer: PaymentSheet.CustomerConfiguration?,
    val googlePay: PaymentSheet.GooglePayConfiguration?,
    val defaultBillingDetails: PaymentSheet.BillingDetails?,
    val shippingDetails: AddressDetails?,
    val allowsDelayedPaymentMethods: Boolean,
    val allowsPaymentMethodsRequiringShippingAddress: Boolean,
    val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
    val preferredNetworks: List<CardBrand>,
    val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    val paymentMethodOrder: List<String>,
    val externalPaymentMethods: List<String>,
    val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
) : Parcelable {
    @Suppress("LongMethod", "ThrowsCount")
    fun validate() {
        // These are not localized as they are not intended to be displayed to a user.
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

        customer?.accessType?.let { customerAccessType ->
            when (customerAccessType) {
                is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> {
                    if (customerAccessType.ephemeralKeySecret.isBlank() || customer.ephemeralKeySecret.isBlank()) {
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
                is PaymentSheet.CustomerAccessType.CustomerSession -> {
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
            }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal fun String.isEKClientSecretValid(): Boolean {
    return Regex(IsEKClientSecretValidRegexPattern).matches(this)
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
)

@ExperimentalEmbeddedPaymentElementApi
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
)
