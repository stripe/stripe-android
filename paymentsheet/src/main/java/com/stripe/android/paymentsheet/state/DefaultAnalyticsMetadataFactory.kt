package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.Nested
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.SimpleBoolean
import com.stripe.android.lpmfoundations.paymentmethod.AnalyticsMetadata.Value.SimpleString
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.analyticsValue
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.payments.financialconnections.GetFinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.uicore.StripeThemeDefaults
import javax.inject.Inject
import javax.inject.Provider

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal class DefaultAnalyticsMetadataFactory @Inject constructor(
    private val cvcRecollectionHandler: CvcRecollectionHandler,
    private val mode: EventReporter.Mode,
    private val analyticEventCallbackProvider: Provider<AnalyticEventCallback?>,
) : DefaultPaymentElementLoader.AnalyticsMetadataFactory {
    override fun create(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationMetadata: IntegrationMetadata,
        elementsSession: ElementsSession,
        isGooglePaySupported: Boolean,
        configuration: PaymentElementLoader.Configuration,
        customerMetadata: CustomerMetadata?,
        linkStateResult: LinkStateResult?,
    ): AnalyticsMetadata = buildMap<String, AnalyticsMetadata.Value> {
        putAll(
            initialization(
                initializationMode = initializationMode,
                integrationMetadata = integrationMetadata,
            )
        )
        putAll(
            intent(
                stripeIntent = elementsSession.stripeIntent,
            )
        )
        putAll(
            link(
                elementsSession = elementsSession,
                linkStateResult = linkStateResult,
                commonConfiguration = configuration.commonConfiguration,
            )
        )
        putAll(
            defaultPaymentMethods(
                elementsSession = elementsSession,
                customerMetadata = customerMetadata
            )
        )

        put("google_pay_enabled", SimpleBoolean(isGooglePaySupported))

        put("mpe_config", Nested(configuration.analyticsMap()))
    }.let { AnalyticsMetadata(it) }

    private fun initialization(
        initializationMode: PaymentElementLoader.InitializationMode,
        integrationMetadata: IntegrationMetadata,
    ) = buildMap<String, AnalyticsMetadata.Value> {
        put("intent_type", SimpleString(initializationMode.defaultAnalyticsValue))
        put("is_decoupled", SimpleBoolean(integrationMetadata.isDeferred()))
        put("is_spt", SimpleBoolean(integrationMetadata.isSpt()))
        put("is_confirmation_tokens", SimpleBoolean(integrationMetadata.isConfirmationTokens()))
    }

    private fun intent(
        stripeIntent: StripeIntent
    ) = buildMap<String, AnalyticsMetadata.Value> {
        put("intent_id", SimpleString(stripeIntent.id))
        put("require_cvc_recollection", SimpleBoolean(cvcRecollectionHandler.cvcRecollectionEnabled(stripeIntent)))
        put("currency", SimpleString(stripeIntent.currency))
        put("setup_future_usage", SimpleString(stripeIntent.setupFutureUsage()?.code))
        val pmoSfuSet = stripeIntent.paymentMethodOptionsSetupFutureUsageMap()
        put("payment_method_options_setup_future_usage", SimpleBoolean(pmoSfuSet))
    }

    private fun link(
        elementsSession: ElementsSession,
        linkStateResult: LinkStateResult?,
        commonConfiguration: CommonConfiguration,
    ) = buildMap<String, AnalyticsMetadata.Value> {
        val linkState = linkStateResult as? LinkState
        put("link_enabled", SimpleBoolean(linkState != null))
        put("link_mode", SimpleString(elementsSession.linkSettings?.linkMode?.analyticsValue))
        val fcAvailability = GetFinancialConnectionsAvailability(elementsSession).toAnalyticsParam()
        put("fc_sdk_availability", SimpleString(fcAvailability))
        put("link_display", SimpleString(commonConfiguration.link.display.analyticsValue))
        val disabledState = (linkStateResult as? LinkDisabledState)
        val disabledReasons = disabledState?.linkDisabledReasons?.map { it.value }
        putNonEmpty("link_disabled_reasons", disabledReasons)
        val signupDisabledReasons = linkState?.signupModeResult?.disabledReasons?.map { it.value }
        putNonEmpty("link_signup_disabled_reasons", signupDisabledReasons)
    }

    private fun defaultPaymentMethods(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ) = buildMap<String, AnalyticsMetadata.Value> {
        put("set_as_default_enabled", SimpleBoolean(customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true))
        put("has_default_payment_method", SimpleBoolean(elementsSession.customer?.defaultPaymentMethod != null))
    }

    private fun PaymentElementLoader.Configuration.analyticsMap() = buildMap<String, AnalyticsMetadata.Value> {
        putAll(commonConfiguration.analyticsMap())

        when (this@analyticsMap) {
            is PaymentElementLoader.Configuration.PaymentSheet -> putAll(analyticsMap())
            is PaymentElementLoader.Configuration.Embedded -> putAll(analyticsMap())
            is PaymentElementLoader.Configuration.CryptoOnramp -> Unit
        }
    }

    private fun PaymentElementLoader.Configuration.PaymentSheet.analyticsMap() =
        buildMap<String, AnalyticsMetadata.Value> {
            put("payment_method_layout", SimpleString(configuration.paymentMethodLayout.toAnalyticsValue()))
        }

    private fun PaymentElementLoader.Configuration.Embedded.analyticsMap() = buildMap<String, AnalyticsMetadata.Value> {
        put(
            "form_sheet_action",
            SimpleString(
                when (configuration.formSheetAction) {
                    EmbeddedPaymentElement.FormSheetAction.Continue -> "continue"
                    EmbeddedPaymentElement.FormSheetAction.Confirm -> "confirm"
                }
            )
        )
        put(
            "row_selection_behavior",
            SimpleString(if (isRowSelectionImmediateAction) "immediate_action" else "default")
        )
        put("embedded_view_displays_mandate_text", SimpleBoolean(configuration.embeddedViewDisplaysMandateText))
    }

    private fun CommonConfiguration.analyticsMap() = buildMap<String, AnalyticsMetadata.Value> {
        put("customer", SimpleBoolean(customer != null))
        put("customer_access_provider", SimpleString(customer?.accessType?.analyticsValue))
        put("googlepay", SimpleBoolean(googlePay != null))
        put("primary_button_color", SimpleBoolean(primaryButtonColorUsage()))
        put("default_billing_details", SimpleBoolean(defaultBillingDetails?.isFilledOut() == true))
        put("allows_delayed_payment_methods", SimpleBoolean(allowsDelayedPaymentMethods))
        putNonEmpty("payment_method_order", paymentMethodOrder)
        put(
            "allows_payment_methods_requiring_shipping_address",
            SimpleBoolean(allowsPaymentMethodsRequiringShippingAddress)
        )
        put("allows_removal_of_last_saved_payment_method", SimpleBoolean(allowsRemovalOfLastSavedPaymentMethod))
        putNonEmpty("preferred_networks", preferredNetworks.map { it.code })
        putNonEmpty("custom_payment_methods", customPaymentMethods.map { it.id })
        putNonEmpty("external_payment_methods", getExternalPaymentMethodsAnalyticsValue())
        put("card_brand_acceptance", SimpleBoolean(cardBrandAcceptance.toAnalyticsValue()))
        put("analytic_callback_set", SimpleBoolean(analyticEventCallbackProvider.get() != null))
        put("open_card_scan_automatically", SimpleBoolean(opensCardScannerAutomatically))
        put("terms_display", SimpleBoolean(termsDisplay.isNotEmpty()))

        put("billing_details_collection_configuration", Nested(billingDetailsCollectionConfiguration.analyticsMap()))

        put("appearance", Nested(appearance.analyticsMap()))
    }

    private fun PaymentSheet.BillingDetailsCollectionConfiguration.analyticsMap() =
        buildMap<String, AnalyticsMetadata.Value> {
            put("attach_defaults", SimpleBoolean(attachDefaultsToPaymentMethod))
            put("name", SimpleString(name.name))
            put("email", SimpleString(email.name))
            put("phone", SimpleString(phone.name))
            put("address", SimpleString(address.name))
        }

    private fun PaymentSheet.Appearance.analyticsMap() = buildMap<String, AnalyticsMetadata.Value> {
        val primaryButtonConfig = primaryButton

        val primaryButtonConfigMap = buildMap<String, AnalyticsMetadata.Value> {
            put(
                "colorsLight",
                SimpleBoolean(primaryButton.colorsLight != PaymentSheet.PrimaryButtonColors.defaultLight)
            )
            put("colorsDark", SimpleBoolean(primaryButton.colorsDark != PaymentSheet.PrimaryButtonColors.defaultDark))
            put("corner_radius", SimpleBoolean(primaryButtonConfig.shape.cornerRadiusDp != null))
            put("border_width", SimpleBoolean(primaryButtonConfig.shape.borderStrokeWidthDp != null))
            put("font", SimpleBoolean(primaryButtonConfig.typography.fontResId != null))
        }
        put("primary_button", Nested(primaryButtonConfigMap))

        val appearanceMap = buildMap<String, AnalyticsMetadata.Value> {
            put("colorsLight", SimpleBoolean(colorsLight != PaymentSheet.Colors.defaultLight))
            put("colorsDark", SimpleBoolean(colorsDark != PaymentSheet.Colors.defaultDark))
            put("corner_radius", SimpleBoolean(shapes.cornerRadiusDp != StripeThemeDefaults.shapes.cornerRadius))
            val customizedBorderWidth = shapes.borderStrokeWidthDp != StripeThemeDefaults.shapes.borderStrokeWidth
            put("border_width", SimpleBoolean(customizedBorderWidth))
            put("font", SimpleBoolean(typography.fontResId != null))
            val customizedSizeFactor = typography.sizeScaleFactor != StripeThemeDefaults.typography.fontSizeMultiplier
            put("size_scale_factor", SimpleBoolean(customizedSizeFactor))
        }
        putAll(appearanceMap)

        var usedEmbeddedAppearance = false
        if (mode == EventReporter.Mode.Embedded) {
            val values = embeddedAppearance.analyticsMap()
            put("embedded_payment_element", Nested(values))
            usedEmbeddedAppearance = values.values.hasTrueBooleanValue()
        }

        // We add a usage field to make queries easier.
        val usedAppearance = appearanceMap.values.hasTrueBooleanValue()
        val usedPrimaryButton = primaryButtonConfigMap.values.hasTrueBooleanValue()

        put("usage", SimpleBoolean(usedAppearance || usedPrimaryButton || usedEmbeddedAppearance))
    }

    internal fun PaymentSheet.Appearance.Embedded.analyticsMap() = buildMap<String, AnalyticsMetadata.Value> {
        put("style", SimpleBoolean(style != PaymentSheet.Appearance.Embedded.default.style))
        put("row_style", SimpleString(style.toAnalyticsValue()))
    }
}

private val PaymentElementLoader.InitializationMode.defaultAnalyticsValue: String
    get() = when (this) {
        is PaymentElementLoader.InitializationMode.CryptoOnramp -> "crypto_onramp"
        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            when (this.intentConfiguration.mode) {
                is PaymentSheet.IntentConfiguration.Mode.Payment -> "deferred_payment_intent"
                is PaymentSheet.IntentConfiguration.Mode.Setup -> "deferred_setup_intent"
            }
        }
        is PaymentElementLoader.InitializationMode.PaymentIntent -> "payment_intent"
        is PaymentElementLoader.InitializationMode.SetupIntent -> "setup_intent"
    }

private fun IntegrationMetadata.isDeferred(): Boolean = when (this) {
    is IntegrationMetadata.IntentFirst -> false
    IntegrationMetadata.CryptoOnramp -> true
    is IntegrationMetadata.CustomerSheet -> true
    is IntegrationMetadata.DeferredIntentWithConfirmationToken -> true
    is IntegrationMetadata.DeferredIntentWithPaymentMethod -> true
    is IntegrationMetadata.DeferredIntentWithSharedPaymentToken -> true
}

private fun IntegrationMetadata.isSpt(): Boolean {
    return this is IntegrationMetadata.DeferredIntentWithSharedPaymentToken
}

private fun IntegrationMetadata.isConfirmationTokens(): Boolean {
    return this is IntegrationMetadata.DeferredIntentWithConfirmationToken
}

private fun StripeIntent.paymentMethodOptionsSetupFutureUsageMap(): Boolean {
    return getPaymentMethodOptions().any { (_, value) ->
        (value as? Map<*, *>)?.get("setup_future_usage") != null
    }
}

private fun StripeIntent.setupFutureUsage(): StripeIntent.Usage? = when (this) {
    is SetupIntent -> usage
    is PaymentIntent -> setupFutureUsage
}

private fun FinancialConnectionsAvailability?.toAnalyticsParam(): String = when (this) {
    FinancialConnectionsAvailability.Full -> "FULL"
    FinancialConnectionsAvailability.Lite -> "LITE"
    null -> "NONE"
}

private fun CommonConfiguration.primaryButtonColorUsage(): Boolean {
    return appearance.primaryButton.colorsLight.background != null ||
        appearance.primaryButton.colorsDark.background != null
}

private fun PaymentSheet.Appearance.Embedded.RowStyle.toAnalyticsValue(): String {
    return when (this) {
        is PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton -> "floating_button"
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio -> "flat_with_radio"
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark -> "flat_with_checkmark"
        is PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure -> "flat_with_disclosure"
    }
}

private fun Collection<AnalyticsMetadata.Value>.hasTrueBooleanValue(): Boolean {
    for (value in this) {
        if ((value as? SimpleBoolean)?.value == true) {
            return true
        }
    }
    return false
}

private fun CommonConfiguration.getExternalPaymentMethodsAnalyticsValue(): List<String> {
    return this.externalPaymentMethods.take(PaymentSheetEvent.MAX_EXTERNAL_PAYMENT_METHODS)
}

private fun PaymentSheet.CardBrandAcceptance.toAnalyticsValue(): Boolean {
    return this !is PaymentSheet.CardBrandAcceptance.All
}

private fun PaymentSheet.PaymentMethodLayout.toAnalyticsValue(): String {
    return when (this) {
        PaymentSheet.PaymentMethodLayout.Horizontal -> "horizontal"
        PaymentSheet.PaymentMethodLayout.Vertical -> "vertical"
        PaymentSheet.PaymentMethodLayout.Automatic -> "automatic"
    }
}

private fun MutableMap<String, AnalyticsMetadata.Value>.putNonEmpty(key: String, list: List<String>?) {
    if (!list.isNullOrEmpty()) {
        put(key, SimpleString(list.joinToString(",")))
    }
}
