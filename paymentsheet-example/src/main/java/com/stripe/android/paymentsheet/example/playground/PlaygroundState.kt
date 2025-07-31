package com.stripe.android.paymentsheet.example.playground

import android.os.Parcelable
import androidx.compose.runtime.Stable
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.customersheet.CustomerSheet
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodConfigurationSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodOptionsSetupFutureUsageSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.RequireCvcRecollectionDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.utils.getPMOSFUFromStringMap
import com.stripe.android.paymentsheet.example.utils.stringValueToMap
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Stable
internal sealed interface PlaygroundState : Parcelable {
    val snapshot: PlaygroundSettings.Snapshot
    val integrationType: PlaygroundConfigurationData.IntegrationType
    val countryCode: Country
    val endpoint: String

    @Stable
    @Parcelize
    data class Payment(
        override val snapshot: PlaygroundSettings.Snapshot,
        val amount: Long,
        val paymentMethodTypes: List<String>,
        val customerConfig: CustomerConfiguration?,
        val clientSecret: String,
        private val defaultEndpoint: String,
    ) : PlaygroundState {
        override val integrationType
            get() = snapshot.configurationData.integrationType

        override val countryCode
            get() = snapshot[CountrySettingsDefinition]

        val initializationType
            get() = snapshot[InitializationTypeSettingsDefinition]

        val checkoutMode
            get() = snapshot[CheckoutModeSettingsDefinition]

        val currencyCode
            get() = snapshot[CurrencySettingsDefinition]

        val paymentMethodConfigurationId: String?
            get() = snapshot[PaymentMethodConfigurationSettingsDefinition].ifEmpty { null }

        val stripeIntentId: String
            get() = clientSecret.substringBefore("_secret_")

        val requireCvcRecollectionForDeferred
            get() = snapshot[RequireCvcRecollectionDefinition]

        override val endpoint: String
            get() = snapshot[CustomEndpointDefinition] ?: defaultEndpoint

        @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
        val paymentMethodOptionsSetupFutureUsage: IntentConfiguration.Mode.Payment.PaymentMethodOptions
            get() {
                val map = stringValueToMap(
                    snapshot[PaymentMethodOptionsSetupFutureUsageOverrideSettingsDefinition]
                ) ?: snapshot[PaymentMethodOptionsSetupFutureUsageSettingsDefinition].valuesMap
                return getPMOSFUFromStringMap(map)
            }

        fun intentConfiguration(): IntentConfiguration {
            return IntentConfiguration(
                mode = checkoutMode.intentConfigurationMode(this),
                paymentMethodTypes = paymentMethodTypes,
                paymentMethodConfigurationId = paymentMethodConfigurationId,
                requireCvcRecollection = requireCvcRecollectionForDeferred
            )
        }

        fun paymentSheetConfiguration(settings: Settings): PaymentSheet.Configuration {
            return snapshot.paymentSheetConfiguration(playgroundState = this, appSettings = settings)
        }

        fun embeddedConfiguration(): EmbeddedPaymentElement.Configuration {
            return snapshot.embeddedConfiguration(this)
        }

        fun linkControllerConfiguration(): LinkController.Configuration {
            return snapshot.linkControllerConfiguration(this)
        }

        fun displaysShippingAddressButton(): Boolean {
            return integrationType in setOf(
                PlaygroundConfigurationData.IntegrationType.PaymentSheet,
                PlaygroundConfigurationData.IntegrationType.FlowController,
            )
        }
    }

    @Stable
    @Parcelize
    data class Customer(
        override val snapshot: PlaygroundSettings.Snapshot,
        override val endpoint: String,
    ) : PlaygroundState {
        override val integrationType
            get() = snapshot.configurationData.integrationType

        override val countryCode
            get() = snapshot[CountrySettingsDefinition]

        val isNewCustomer
            get() = snapshot[CustomerSettingsDefinition] == CustomerType.NEW

        val isUsingCustomerSession
            get() = snapshot[CustomerSessionSettingsDefinition]

        val inSetupMode
            get() = snapshot[CustomerSheetPaymentMethodModeDefinition] ==
                PaymentMethodMode.SetupIntent

        val supportedPaymentMethodTypes: List<String>
            get() = snapshot[SupportedPaymentMethodsSettingsDefinition]
                .split(",")
                .filterNot { value ->
                    value.isBlank()
                }

        fun customerSheetConfiguration(): CustomerSheet.Configuration {
            return snapshot.customerSheetConfiguration(this)
        }

        fun customerEphemeralKeyRequest(): CustomerEphemeralKeyRequest {
            return snapshot.customerEphemeralKeyRequest()
        }
    }

    @Parcelize
    data class SharedPaymentToken(
        override val snapshot: PlaygroundSettings.Snapshot,
        val customerId: String,
        val customerSessionClientSecret: String,
    ) : PlaygroundState {
        override val integrationType
            get() = snapshot.configurationData.integrationType

        override val countryCode
            get() = snapshot[CountrySettingsDefinition]

        @IgnoredOnParcel
        val amount = 9999L

        @IgnoredOnParcel
        val currencyCode = Currency.USD

        override val endpoint: String
            get() = ""

        fun paymentSheetConfiguration(): PaymentSheet.Configuration {
            return snapshot.paymentSheetConfiguration(this)
        }

        @OptIn(SharedPaymentTokenSessionPreview::class)
        fun intentConfiguration(): IntentConfiguration {
            return IntentConfiguration(
                sharedPaymentTokenSessionWithMode = IntentConfiguration.Mode.Payment(
                    amount = amount,
                    currency = currencyCode.value,
                    captureMethod = IntentConfiguration.CaptureMethod.Manual,
                ),
                sellerDetails = IntentConfiguration.SellerDetails(
                    networkId = "internal",
                    externalId = "stripe_test_merchant"
                ),
                paymentMethodTypes = listOf("card", "link", "shop_pay")
            )
        }
    }

    fun asPaymentState(): Payment? {
        return this as? Payment
    }

    fun asCustomerState(): Customer? {
        return this as? Customer
    }

    companion object {
        fun CheckoutResponse.asPlaygroundState(
            snapshot: PlaygroundSettings.Snapshot,
            defaultEndpoint: String,
        ): PlaygroundState {
            val paymentMethodTypes = if (snapshot[AutomaticPaymentMethodsSettingsDefinition]) {
                emptyList()
            } else {
                paymentMethodTypes
                    .orEmpty()
                    .split(",")
            }
            return Payment(
                snapshot = snapshot,
                amount = amount,
                paymentMethodTypes = paymentMethodTypes,
                customerConfig = makeCustomerConfig(snapshot.checkoutRequest().customerKeyType),
                clientSecret = intentClientSecret,
                defaultEndpoint = defaultEndpoint
            )
        }
    }
}
