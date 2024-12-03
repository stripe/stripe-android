package com.stripe.android.paymentsheet.example.playground

import android.os.Parcelable
import androidx.compose.runtime.Stable
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.settings.AdditionalInsetsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.AutomaticPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.BottomSeparatorEnabledSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckmarkColorSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckmarkInsetsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CurrencySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSessionSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.FloatingButtonSpacingSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodConfigurationSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.RequireCvcRecollectionDefinition
import com.stripe.android.paymentsheet.example.playground.settings.RowStyleEnum
import com.stripe.android.paymentsheet.example.playground.settings.RowStyleSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SelectedColorSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SeparatorColorSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SeparatorInsetsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SeparatorThicknessSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.SupportedPaymentMethodsSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.TopSeparatorEnabledSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.UnselectedColorSettingsDefinition
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
        val customerConfig: PaymentSheet.CustomerConfiguration?,
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

        fun intentConfiguration(): PaymentSheet.IntentConfiguration {
            return PaymentSheet.IntentConfiguration(
                mode = checkoutMode.intentConfigurationMode(this),
                paymentMethodTypes = paymentMethodTypes,
                paymentMethodConfigurationId = paymentMethodConfigurationId,
                requireCvcRecollection = requireCvcRecollectionForDeferred
            )
        }

        fun paymentSheetConfiguration(): PaymentSheet.Configuration {
            return snapshot.paymentSheetConfiguration(this)
        }

        @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
        fun embeddedConfiguration(): EmbeddedPaymentElement.Configuration {
            return snapshot.embeddedConfiguration(this)
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

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    data class EmbeddedAppearanceState(
        val snapshot: PlaygroundSettings.Snapshot
    ) {

        val rowStyle: RowStyleEnum
            get() = snapshot[RowStyleSettingsDefinition]

        val separatorThicknessDp: Float
            get() = snapshot[SeparatorThicknessSettingsDefinition]

        val separatorInsetsDp: Float
            get() = snapshot[SeparatorInsetsSettingsDefinition]

        val additionalInsetsDp: Float
            get() = snapshot[AdditionalInsetsSettingsDefinition]

        val checkmarkInsetsDp: Float
            get() = snapshot[CheckmarkInsetsSettingsDefinition]

        val floatingButtonSpacingDp: Float
            get() = snapshot[FloatingButtonSpacingSettingsDefinition]

        val topSeparatorEnabled: Boolean
            get() = snapshot[TopSeparatorEnabledSettingsDefinition]

        val bottomSeparatorEnabled: Boolean
            get() = snapshot[BottomSeparatorEnabledSettingsDefinition]

        val separatorColor: Int
            get() = snapshot[SeparatorColorSettingsDefinition]

        val selectedColor: Int
            get() = snapshot[SelectedColorSettingsDefinition]

        val unselectedColor: Int
            get() = snapshot[UnselectedColorSettingsDefinition]

        val checkmarkColor: Int
            get() = snapshot[CheckmarkColorSettingsDefinition]

        fun getRow(): PaymentSheet.Appearance.Embedded.RowStyle {
            return when (rowStyle) {
                RowStyleEnum.FlatWithRadio -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio(
                    separatorThicknessDp = separatorThicknessDp,
                    separatorColor = separatorColor,
                    separatorInsetsDp = separatorInsetsDp,
                    topSeparatorEnabled = topSeparatorEnabled,
                    bottomSeparatorEnabled = bottomSeparatorEnabled,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    additionalInsetsDp = additionalInsetsDp
                )
                RowStyleEnum.FlatWithCheckmark -> PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                    separatorThicknessDp = separatorThicknessDp,
                    separatorColor = separatorColor,
                    separatorInsetsDp = separatorInsetsDp,
                    topSeparatorEnabled = topSeparatorEnabled,
                    bottomSeparatorEnabled = bottomSeparatorEnabled,
                    checkmarkColor = checkmarkColor,
                    checkmarkInsetDp = checkmarkInsetsDp,
                    additionalInsetsDp = additionalInsetsDp
                )
                RowStyleEnum.FloatingButton -> PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton(
                    spacingDp = floatingButtonSpacingDp,
                    additionalInsetsDp = additionalInsetsDp
                )
            }
        }
    }
}
