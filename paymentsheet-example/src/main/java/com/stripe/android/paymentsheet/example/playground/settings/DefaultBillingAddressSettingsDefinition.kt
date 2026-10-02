@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import java.util.UUID

internal object DefaultBillingAddressSettingsDefinition :
    PlaygroundSettingDefinition<DefaultBillingAddress>,
    PlaygroundSettingDefinition.Saveable<DefaultBillingAddress>,
    PlaygroundSettingDefinition.Displayable<DefaultBillingAddress> {

    override val key: String = "defaultBillingAddress"
    override val defaultValue: DefaultBillingAddress = DefaultBillingAddress.On

    override fun convertToValue(value: String): DefaultBillingAddress {
        return when (value) {
            "on" -> DefaultBillingAddress.On
            "on_with_random_email" -> DefaultBillingAddress.OnWithRandomEmail
            "off" -> DefaultBillingAddress.Off
            else -> if (value.startsWith(WITH_EMAIL_AND_NO_PHONE_PREFIX)) {
                DefaultBillingAddress.WithEmail(
                    email = value.removePrefix(WITH_EMAIL_AND_NO_PHONE_PREFIX),
                    phone = null,
                )
            } else {
                defaultValue
            }
        }
    }

    override fun convertToString(value: DefaultBillingAddress): String {
        return when (value) {
            is DefaultBillingAddress.WithEmail -> if (value.phone == null) {
                WITH_EMAIL_AND_NO_PHONE_PREFIX + value.email
            } else {
                value.value
            }
            else -> value.value
        }
    }

    override val displayName: String
        get() = "Default Billing Address"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("On", DefaultBillingAddress.On),
        option("On with random email", DefaultBillingAddress.OnWithRandomEmail),
        option("Off", DefaultBillingAddress.Off),
    )

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: LinkController.Configuration,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    private fun createBillingDetails(value: DefaultBillingAddress): PaymentSheet.BillingDetails? {
        val email = when (value) {
            DefaultBillingAddress.On -> "email@email.com"
            DefaultBillingAddress.OnWithRandomEmail -> "email_${UUID.randomUUID()}@email.com"
            DefaultBillingAddress.Off -> null
            is DefaultBillingAddress.WithEmail -> value.email
        }

        return email?.let {
            PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "354 Oyster Point Blvd",
                    line2 = null,
                    city = "South San Francisco",
                    state = "CA",
                    postalCode = "94080",
                    country = "US",
                ),
                email = email,
                name = "Jenny Rosen",
                phone = if (value is DefaultBillingAddress.WithEmail) {
                    value.phone
                } else {
                    DEFAULT_BILLING_ADDRESS_PHONE
                },
            )
        }
    }

    private const val WITH_EMAIL_AND_NO_PHONE_PREFIX = "with_email_and_no_phone:"
}

internal const val DEFAULT_BILLING_ADDRESS_PHONE = "+18008675309"

internal sealed class DefaultBillingAddress(val value: String) {
    data object On : DefaultBillingAddress("on")
    data object OnWithRandomEmail : DefaultBillingAddress("on_with_random_email")
    data object Off : DefaultBillingAddress("off")
    data class WithEmail(
        val email: String,
        val phone: String?,
    ) : DefaultBillingAddress("with_email")
}
