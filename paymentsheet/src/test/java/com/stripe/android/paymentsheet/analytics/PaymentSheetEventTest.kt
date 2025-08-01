package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_SELECTION
import com.stripe.android.model.PaymentMethodFixtures.LINK_INLINE_PAYMENT_SELECTION
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PaymentSheetEventTest {

    @Test
    fun `Init event with full config should return expected params`() {
        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer_googlepay"
        )

        val expectedConfig = buildInitMpeConfig(
            customer = true,
            customerAccessProvider = "legacy",
            googlePay = true
        )

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with external payment methods should return expected params`() {
        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_EXTERNAL_PAYMENT_METHODS
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer"
        )

        val expectedConfig = buildInitMpeConfig(
            customer = true,
            customerAccessProvider = "legacy",
            externalPaymentMethods = listOf("external_paypal", "external_fawry")
        )

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with custom payment methods should return expected params`() {
        val config = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_CUSTOM_PAYMENT_METHODS
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer"
        )

        val expectedConfig = buildInitMpeConfig(
            customer = true,
            customerAccessProvider = "legacy",
            customPaymentMethods = "cpmt_123,cpmt_456",
        )

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with vertical mode should return expected params`() {
        val config = PaymentSheetFixtures.CONFIG_CUSTOMER.newBuilder()
            .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Vertical)
            .build()
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer"
        )

        val expectedConfig = buildInitMpeConfig(
            customer = true,
            customerAccessProvider = "legacy",
            paymentMethodLayout = "vertical",
        )

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with minimum config should return expected params`() {
        val config = PaymentSheetFixtures.CONFIG_MINIMUM
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_default"
        )

        val expectedConfig = buildInitMpeConfig()

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with preferred networks`() {
        val config = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder()
            .preferredNetworks(listOf(CardBrand.CartesBancaires, CardBrand.Visa))
            .build()
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = config.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_default"
        )

        val expectedConfig = buildInitMpeConfig(
            preferredNetworks = "cartes_bancaires, visa"
        )

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init with legacy customer has expected keys`() {
        val event = createInitEvent(
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder()
                .customer(
                    PaymentSheet.CustomerConfiguration(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_123"
                    )
                ).build()
        )

        val config = event.params["mpe_config"]?.asMap()

        assertThat(config).containsEntry("customer", true)
        assertThat(config).containsEntry("customer_access_provider", "legacy")
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `Init with customer session enabled customer has expected keys`() {
        val event = createInitEvent(
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM.newBuilder()
                .customer(
                    PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "ek_123",
                    )
                ).build()
        )

        val config = event.params["mpe_config"]?.asMap()

        assertThat(config).containsEntry("customer", true)
        assertThat(config).containsEntry("customer_access_provider", "customer_session")
    }

    @Test
    fun `Init event with embedded appearance should return expected params`() {
        val config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
            .appearance(
                PaymentSheet.Appearance(
                    embeddedAppearance = PaymentSheet.Appearance.Embedded(
                        style = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.default
                    )
                )
            )
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .embeddedViewDisplaysMandateText(true)
            .build()
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Embedded,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = null,
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.Embedded(
                configuration = config,
                isRowSelectionImmediateAction = false
            ),
            isDeferred = true,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_embedded_init"
        )

        val expectedConfig = buildInitMpeConfig(
            appearanceMap = buildAppearanceMap(
                usedParams = false,
                embeddedConfig = mapOf(
                    "style" to true,
                    "row_style" to "flat_with_checkmark"
                )
            ),
            paymentMethodLayout = null,
            primaryButtonColor = null,
        ) {
            put("form_sheet_action", "confirm")
            put("row_selection_behavior", "default")
            put("embedded_view_displays_mandate_text", true)
        }

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", true)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with embedded should return expected params`() {
        val config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
            .appearance(
                PaymentSheet.Appearance(
                    embeddedAppearance = PaymentSheet.Appearance.Embedded(
                        style = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.default
                    )
                )
            )
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            .embeddedViewDisplaysMandateText(false)
            .build()
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Embedded,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = null,
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.Embedded(
                configuration = config,
                isRowSelectionImmediateAction = false
            ),
            isDeferred = true,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_embedded_init"
        )

        val expectedConfig = buildInitMpeConfig(
            appearanceMap = buildAppearanceMap(
                usedParams = false,
                embeddedConfig = mapOf(
                    "style" to true,
                    "row_style" to "flat_with_checkmark"
                )
            ),
            paymentMethodLayout = null,
            primaryButtonColor = null,
        ) {
            put("form_sheet_action", "continue")
            put("row_selection_behavior", "default")
            put("embedded_view_displays_mandate_text", false)
        }

        assertThat(event.params).run {
            containsEntry("link_enabled", false)
            containsEntry("google_pay_enabled", false)
            containsEntry("is_decoupled", true)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with embedded immediateRowSelectionBehavior should return expected params`() {
        val config = EmbeddedPaymentElement.Configuration.Builder("Example, Inc")
            .appearance(
                PaymentSheet.Appearance(
                    embeddedAppearance = PaymentSheet.Appearance.Embedded(
                        style = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.default
                    )
                )
            )
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue)
            .embeddedViewDisplaysMandateText(false)
            .build()
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Embedded,
            configuration = config.asCommonConfiguration(),
            appearance = config.appearance,
            primaryButtonColor = null,
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.Embedded(
                configuration = config,
                isRowSelectionImmediateAction = true
            ),
            isDeferred = true,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_embedded_init"
        )

        val expectedConfig = buildInitMpeConfig(
            appearanceMap = buildAppearanceMap(
                usedParams = false,
                embeddedConfig = mapOf(
                    "style" to true,
                    "row_style" to "flat_with_checkmark"
                )
            ),
            paymentMethodLayout = null,
            primaryButtonColor = null,
        ) {
            put("form_sheet_action", "continue")
            put("row_selection_behavior", "immediate_action")
            put("embedded_view_displays_mandate_text", false)
        }

        assertThat(event.params).run {
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `LoadSucceeded event should return expected toString()`() {
        val event = createLoadSucceededEvent(
            paymentSelection = null,
            orderedLpms = listOf("card", "klarna"),
        )

        assertThat(event.eventName).isEqualTo("mc_load_succeeded")
        assertThat(event.params).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "link_enabled" to false,
                "is_spt" to false,
                "google_pay_enabled" to false,
                "duration" to 5f,
                "selected_lpm" to "none",
                "intent_type" to "payment_intent",
                "ordered_lpms" to "card,klarna",
                "require_cvc_recollection" to false,
                "link_display" to "automatic",
                "fc_sdk_availability" to "FULL",
                "payment_method_options_setup_future_usage" to false,
                "setup_future_usage" to null,
            )
        )
    }

    @Test
    fun `LoadSucceeded event with setAsDefaultPaymentMethod should return expected toString()`() {
        val event = createLoadSucceededEvent(
            paymentSelection = null,
            orderedLpms = listOf("card", "klarna"),
            hasDefaultPaymentMethod = false,
            setAsDefaultEnabled = true,
        )

        assertThat(event.params).containsEntry("set_as_default_enabled", true)
        assertThat(event.params).containsEntry("has_default_payment_method", false)
    }

    @Test
    fun `LoadSucceeded event with setAsDefaultPaymentMethod null should return expected toString()`() {
        val event = createLoadSucceededEvent(
            paymentSelection = null,
            orderedLpms = listOf("card", "klarna"),
            hasDefaultPaymentMethod = false,
            setAsDefaultEnabled = null,
        )

        assertThat(event.params).doesNotContainKey("set_as_default_enabled")
        assertThat(event.params).doesNotContainKey("has_default_payment_method")
    }

    @Test
    fun `LoadSucceeded event with setAsDefaultPaymentMethod false should return expected toString()`() {
        val event = createLoadSucceededEvent(
            paymentSelection = null,
            orderedLpms = listOf("card", "klarna"),
            hasDefaultPaymentMethod = false,
            setAsDefaultEnabled = false,
        )

        assertThat(event.params).containsEntry("set_as_default_enabled", false)
        assertThat(event.params).doesNotContainKey("has_default_payment_method")
    }

    @Test
    fun `LoadSucceeded event with hasDefaultPaymentMethod null should return expected toString()`() {
        val event = createLoadSucceededEvent(
            paymentSelection = null,
            orderedLpms = listOf("card", "klarna"),
            hasDefaultPaymentMethod = null,
            setAsDefaultEnabled = true,
        )

        assertThat(event.params).containsEntry("set_as_default_enabled", true)
        assertThat(event.params).doesNotContainKey("has_default_payment_method")
    }

    @Test
    fun `LoadSucceeded event should return 'google_pay' for selected lpm when saved selection is Google Pay`() {
        val event = createLoadSucceededEvent(
            paymentSelection = PaymentSelection.GooglePay,
        )

        assertThat(event.params).containsEntry("selected_lpm", "google_pay")
    }

    @Test
    fun `LoadSucceeded event should return 'link' for selected lpm when saved selection is Link`() {
        val event = createLoadSucceededEvent(
            paymentSelection = PaymentSelection.Link(),
        )

        assertThat(event.params).containsEntry("selected_lpm", "link")
    }

    @Test
    fun `LoadSucceeded event should return id for selected lpm when saved selection is a payment method`() {
        val event = createLoadSucceededEvent(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
            ),
        )

        assertThat(event.params).containsEntry("selected_lpm", "sepa_debit")
    }

    @Test
    fun `LoadSucceeded event should contain passthrough mode for Link if provided`() {
        val event = createLoadSucceededEvent(
            linkEnabled = true,
            linkMode = LinkMode.Passthrough,
        )

        assertThat(event.params).containsEntry("link_enabled", true)
        assertThat(event.params).containsEntry("link_mode", "passthrough")
    }

    @Test
    fun `LoadSucceeded event should contain payment method mode for Link if provided`() {
        val event = createLoadSucceededEvent(
            linkEnabled = true,
            linkMode = LinkMode.LinkPaymentMethod,
        )

        assertThat(event.params).containsEntry("link_enabled", true)
        assertThat(event.params).containsEntry("link_mode", "payment_method_mode")
    }

    @Test
    fun `LoadSucceeded initialization mode is correct for setup intents`() {
        val event = createLoadSucceededEvent(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(clientSecret = "cs_example")
        )

        assertThat(event.params).containsEntry("intent_type", "setup_intent")
    }

    @Test
    fun `LoadSucceeded initialization mode is correct for deferred setup intents`() {
        val event = createLoadSucceededEvent(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup()
                )
            )
        )

        assertThat(event.params).containsEntry("intent_type", "deferred_setup_intent")
    }

    @Test
    fun `LoadSucceeded initialization mode is correct for deferred payment intents`() {
        val event = createLoadSucceededEvent(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 50,
                        currency = "usd",
                    )
                )
            )
        )

        assertThat(event.params).containsEntry("intent_type", "deferred_payment_intent")
    }

    @Test
    fun `LoadSucceeded with paymentMethodOptionsSetupFutureUsage should return expected params`() {
        val event = createLoadSucceededEvent(
            paymentMethodOptionsSetupfutureUsage = true
        )

        assertThat(event.params).containsEntry(
            "payment_method_options_setup_future_usage",
            true
        )
    }

    @Test
    fun `LoadSucceeded with setup future usage should return expected params`() {
        val event = createLoadSucceededEvent(
            setupFutureUsage = StripeIntent.Usage.OffSession
        )

        assertThat(event.params).containsEntry(
            "setup_future_usage",
            "off_session"
        )
    }

    @Test
    fun `New payment method event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    fun `New payment method set as default true event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            ),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(newPMEvent.params["set_as_default"]).isEqualTo(true)
    }

    @Test
    fun `New payment method set as default false event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            ),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(newPMEvent.params["set_as_default"]).isEqualTo(false)
    }

    @Test
    fun `Saved payment method event should return expected event`() {
        val savedPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_success"
        )
        assertThat(
            savedPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    @Test
    fun `Google pay payment method event should return expected event`() {
        val googlePayEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.GooglePay,
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )
        assertThat(
            googlePayEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "google_pay",
            )
        )
    }

    @Test
    fun `Link payment method event should return expected event`() {
        val linkEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.Link(),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            linkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "link",
                "link_context" to "wallet",
            )
        )
    }

    @Test
    fun `Inline Link payment method event should return expected event`() {
        val inlineLinkEvent = paymentMethodEvent(
            paymentSelection = LINK_INLINE_PAYMENT_SELECTION,
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            inlineLinkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "selected_lpm" to "card",
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `External payment method event should return expected event`() {
        val newPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.ExternalPaymentMethod(
                type = "external_fawry",
                billingDetails = null,
                label = "Fawry".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = null,
            ),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "external_fawry",
            )
        )
    }

    @Test
    fun `Custom payment method event should return expected event`() {
        val newPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.CustomPaymentMethod(
                id = "cpmt_1",
                billingDetails = null,
                label = "BufoPay".resolvableString,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = "some_url",
            ),
            result = PaymentSheetEvent.Payment.Result.Success,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "cpmt_1",
            )
        )
    }

    @Test
    fun `External payment method failure event should return expected event`() {
        val newPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.ExternalPaymentMethod(
                type = "external_fawry",
                billingDetails = null,
                label = "Fawry".resolvableString,
                iconResource = 0,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = null,
            ),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.ExternalPaymentMethod,
            ),
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "external_fawry",
                "error_message" to "externalPaymentMethodError",
            )
        )
    }

    @Test
    fun `Custom payment method failure event should return expected event`() {
        val newPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.CustomPaymentMethod(
                id = "cpmt_1",
                billingDetails = null,
                label = "BufoPay".resolvableString,
                lightThemeIconUrl = "some_url",
                darkThemeIconUrl = "some_url",
            ),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(
                    cause = IllegalStateException("An error occurred!")
                ),
            ),
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "cpmt_1",
                "error_message" to "java.lang.IllegalStateException",
            )
        )
    }

    @Test
    fun `New payment method failure event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            ),
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "card",
                "error_message" to "apiError",
            )
        )
    }

    fun `New payment method failure setAsDefault true event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                setAsDefault = true
            ),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            )
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(newPMEvent.params["set_as_default"]).isEqualTo(true)
    }

    @Test
    fun `New payment method failure setAsDefault false event should return expected event`() {
        val newPMEvent = newCardPaymentMethod(
            paymentMethodExtraParams = PaymentMethodExtraParams.Card(
                setAsDefault = false
            ),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            )
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(newPMEvent.params["set_as_default"]).isEqualTo(false)
    }

    @Test
    fun `Saved payment method failure event should return expected event`() {
        val savedPMEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            ),
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_failure"
        )
        assertThat(
            savedPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "card",
                "error_message" to "apiError",
            )
        )
    }

    @Test
    fun `Google pay payment method failure event should return expected event`() {
        val googlePayEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.GooglePay,
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            ),
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_failure"
        )
        assertThat(
            googlePayEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "google_pay",
                "error_message" to "apiError",
            )
        )
    }

    @Test
    fun `Link payment method failure event should return expected event`() {
        val linkEvent = paymentMethodEvent(
            paymentSelection = PaymentSelection.Link(),
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            ),
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            linkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "selected_lpm" to "link",
                "error_message" to "apiError",
                "link_context" to "wallet",
            )
        )
    }

    @Test
    fun `Inline Link payment method failure event should return expected event`() {
        val inlineLinkEvent = paymentMethodEvent(
            paymentSelection = LINK_INLINE_PAYMENT_SELECTION,
            result = PaymentSheetEvent.Payment.Result.Failure(
                error = PaymentSheetConfirmationError.Stripe(APIException()),
            ),
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            inlineLinkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "selected_lpm" to "card",
                "google_pay_enabled" to false,
                "error_message" to "apiError",
            )
        )
    }

    @Test
    fun `RemoveSavedPaymentMethod event should return expected toString()`() {
        val event = PaymentSheetEvent.RemovePaymentOption(
            mode = EventReporter.Mode.Custom,
            code = "card",
            currency = "usd",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_custom_paymentoption_removed"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_lpm" to "card",
                "currency" to "usd",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `SelectPaymentOption event should return expected toString()`() {
        val event = PaymentSheetEvent.SelectPaymentOption(
            mode = EventReporter.Mode.Custom,
            paymentSelection = PaymentSelection.GooglePay,
            currency = "usd",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_custom_paymentoption_googlepay_select"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `ShowPaymentOptionForm event should return expected toString()`() {
        val event = PaymentSheetEvent.ShowPaymentOptionForm(
            code = "card",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_form_shown"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_lpm" to "card",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `PaymentOptionFormInteraction event should return expected toString()`() {
        val event = PaymentSheetEvent.PaymentOptionFormInteraction(
            code = "card",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_form_interacted"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_lpm" to "card",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `PaymentMethodFormCompleted event should return expected toString()`() {
        val event = PaymentSheetEvent.PaymentMethodFormCompleted(
            code = "card",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_form_completed"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_lpm" to "card",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `CardNumberCompleted event should return expected toString()`() {
        val event = PaymentSheetEvent.CardNumberCompleted(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_card_number_completed"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `ShowEditablePaymentOption event should return expected toString()`() {
        val event = PaymentSheetEvent.ShowEditablePaymentOption(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_open_edit_screen",
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `HideEditablePaymentOption event should return expected toString()`() {
        val event = PaymentSheetEvent.HideEditablePaymentOption(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_cancel_edit_screen",
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `CardBrandSelected event with add source should return expected toString()`() {
        val event = PaymentSheetEvent.CardBrandSelected(
            selectedBrand = CardBrand.CartesBancaires,
            source = PaymentSheetEvent.CardBrandSelected.Source.Add,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_cbc_selected"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "cbc_event_source" to "add",
                "selected_card_brand" to "cartes_bancaires",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `CardBrandSelected event with edit source should return expected toString()`() {
        val event = PaymentSheetEvent.CardBrandSelected(
            selectedBrand = CardBrand.CartesBancaires,
            source = PaymentSheetEvent.CardBrandSelected.Source.Edit,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_cbc_selected"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "cbc_event_source" to "edit",
                "selected_card_brand" to "cartes_bancaires",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `UpdatePaymentOptionSucceeded event should return expected toString()`() {
        val event = PaymentSheetEvent.UpdatePaymentOptionSucceeded(
            selectedBrand = CardBrand.CartesBancaires,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_update_card"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_card_brand" to "cartes_bancaires",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `UpdatePaymentOptionFailed event should return expected toString()`() {
        val event = PaymentSheetEvent.UpdatePaymentOptionFailed(
            selectedBrand = CardBrand.CartesBancaires,
            error = APIException(
                StripeError(type = "network_error", code = "error_123"),
                requestId = "request_123",
                message = "No network available!"
            ),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_update_card_failed"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_card_brand" to "cartes_bancaires",
                "error_message" to "No network available!",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "analytics_value" to "apiError",
                "request_id" to "request_123",
                "error_type" to "network_error",
                "error_code" to "error_123",
            )
        )
    }

    @Test
    fun `SetAsDefaultPaymentMethodSucceeded event with setAsDefaultPaymentMethod should return expected toString()`() {
        val event = PaymentSheetEvent.SetAsDefaultPaymentMethodSucceeded(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            paymentMethodType = PaymentMethod.Type.Card.code,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_set_default_payment_method"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "payment_method_type" to "card",
            )
        )
    }

    @Test
    fun `SetAsDefaultPaymentMethodFailed event with setAsDefaultPaymentMethod should return expected toString()`() {
        val event = PaymentSheetEvent.SetAsDefaultPaymentMethodFailed(
            error = APIException(
                StripeError(type = "network_error", code = "error_123"),
                requestId = "request_123",
                message = "No network available!"
            ),
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            paymentMethodType = PaymentMethod.Type.Card.code,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_set_default_payment_method_failed"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "error_message" to "No network available!",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "payment_method_type" to "card",
                "analytics_value" to "apiError",
                "request_id" to "request_123",
                "error_type" to "network_error",
                "error_code" to "error_123",
            )
        )
    }

    @Test
    fun `Init event should have default params if config is all defaults`() {
        val expectedConfigMap = buildInitMpeConfig()
        val config = PaymentSheetFixtures.CONFIG_MINIMUM
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                primaryButtonColor = config.primaryButtonColorUsage(),
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = true,
                isAnalyticEventCallbackSet = false,
            ).params
        ).isEqualTo(
            mapOf(
                "mpe_config" to expectedConfigMap,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `Init event should should mark all optional params present if they are there`() {
        val expectedConfigMap = buildInitMpeConfig(
            customer = true,
            customerAccessProvider = "legacy",
            googlePay = true,
            primaryButtonColor = true,
            defaultBillingDetails = true,
            allowsDelayedPaymentMethods = true,
            paymentMethodOrder = listOf("klarna", "afterpay", "card"),
            allowPaymentMethodsRequiringShippingAddress = true,
            allowsRemovalOfLastSavedPaymentMethod = false,
            appearanceMap = buildAppearanceMap(true),
            billingDetailsCollectionConfiguration = mapOf(
                "attach_defaults" to true,
                "name" to "Always",
                "email" to "Always",
                "phone" to "Always",
                "address" to "Full",
            ),
            paymentMethodLayout = "automatic",
        )
        val config = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                primaryButtonColor = config.primaryButtonColorUsage(),
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = true,
                isAnalyticEventCallbackSet = false,
            ).params
        ).isEqualTo(
            mapOf(
                "mpe_config" to expectedConfigMap,
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `Init event should report card_scan_available as true if available`() {
        val config = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                primaryButtonColor = config.primaryButtonColorUsage(),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = true,
                isAnalyticEventCallbackSet = false,
            ).params["mpe_config"]?.asMap()?.get("card_scan_available")
        ).isEqualTo(true)
    }

    @Test
    fun `Init event should report card_scan_available as false if unavailable`() {
        val config = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                primaryButtonColor = config.primaryButtonColorUsage(),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = false,
                isAnalyticEventCallbackSet = false,
            ).params["mpe_config"]?.asMap()?.get("card_scan_available")
        ).isEqualTo(false)
    }

    @Test
    fun `Init event should report analytic_callback_set as true if available`() {
        val config = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                primaryButtonColor = config.primaryButtonColorUsage(),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = true,
                isAnalyticEventCallbackSet = true,
            ).params["mpe_config"]?.asMap()?.get("analytic_callback_set")
        ).isEqualTo(true)
    }

    @Test
    fun `Init event should report analytic_callback_set as false if unavailable`() {
        val config = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = config.asCommonConfiguration(),
                appearance = config.appearance,
                configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(config),
                primaryButtonColor = config.primaryButtonColorUsage(),
                isDeferred = false,
                isSpt = false,
                linkEnabled = false,
                googlePaySupported = false,
                isStripeCardScanAvailable = false,
                isAnalyticEventCallbackSet = false,
            ).params["mpe_config"]?.asMap()?.get("analytic_callback_set")
        ).isEqualTo(false)
    }

    @Test
    fun `PressConfirmButton event should return expected toString()`() {
        val event = PaymentSheetEvent.PressConfirmButton(
            selectedLpm = "card",
            currency = "USD",
            duration = 60.seconds,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            linkContext = null,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Full
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_confirm_button_tapped"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "selected_lpm" to "card",
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "duration" to 60f,
                "currency" to "USD",
                "fc_sdk_availability" to "FULL"
            )
        )
    }

    @Test
    fun `PressConfirmButton event should return expected toString() with null values`() {
        val event = PaymentSheetEvent.PressConfirmButton(
            selectedLpm = null,
            currency = null,
            duration = null,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            linkContext = null,
            financialConnectionsAvailability = FinancialConnectionsAvailability.Lite
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_confirm_button_tapped"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "fc_sdk_availability" to "LITE"
            )
        )
    }

    @Test
    fun `CannotProperlyReturnFromLinkAndLPMs event should return expected toString() with null values`() {
        val completeEvent = PaymentSheetEvent.CannotProperlyReturnFromLinkAndLPMs(
            mode = EventReporter.Mode.Complete,
        )

        assertThat(
            completeEvent.eventName
        ).isEqualTo(
            "mc_complete_cannot_return_from_link_and_lpms"
        )

        assertThat(
            completeEvent.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )

        val customEvent = PaymentSheetEvent.CannotProperlyReturnFromLinkAndLPMs(
            mode = EventReporter.Mode.Custom,
        )

        assertThat(
            customEvent.eventName
        ).isEqualTo(
            "mc_custom_cannot_return_from_link_and_lpms"
        )

        assertThat(
            customEvent.params
        ).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `ShopPayWebviewLoadAttempt event should return expected event name and params`() {
        val event = PaymentSheetEvent.ShopPayWebviewLoadAttempt(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )

        assertThat(event.eventName).isEqualTo("mc_shoppay_webview_load_attempt")
        assertThat(event.params).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `ShopPayWebviewConfirmSuccess event should return expected event name and params`() {
        val event = PaymentSheetEvent.ShopPayWebviewConfirmSuccess(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
        )

        assertThat(event.eventName).isEqualTo("mc_shoppay_webview_confirm_success")
        assertThat(event.params).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
            )
        )
    }

    @Test
    fun `ShopPayWebviewCancelled event with ECE click should return expected event name and params`() {
        val event = PaymentSheetEvent.ShopPayWebviewCancelled(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            didReceiveECEClick = true,
        )

        assertThat(event.eventName).isEqualTo("mc_shoppay_webview_cancelled")
        assertThat(event.params).isEqualTo(
            mapOf(
                "is_decoupled" to false,
                "is_spt" to false,
                "link_enabled" to false,
                "google_pay_enabled" to false,
                "did_receive_ece_click" to true,
            )
        )
    }

    @Test
    fun `ShopPayWebviewCancelled event without ECE click should return expected event name and params`() {
        val event = PaymentSheetEvent.ShopPayWebviewCancelled(
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            didReceiveECEClick = false,
        )

        assertThat(event.params["did_receive_ece_click"]).isEqualTo(false)
    }

    @Test
    fun `ShopPay events with different deferred and link states should return expected params`() {
        val loadAttemptEvent = PaymentSheetEvent.ShopPayWebviewLoadAttempt(
            isDeferred = true,
            isSpt = false,
            linkEnabled = true,
            googlePaySupported = true,
        )

        assertThat(loadAttemptEvent.params).isEqualTo(
            mapOf(
                "is_decoupled" to true,
                "is_spt" to false,
                "link_enabled" to true,
                "google_pay_enabled" to true,
            )
        )
    }

    private fun newCardPaymentMethod(
        paymentMethodExtraParams: PaymentMethodExtraParams? = null,
        result: PaymentSheetEvent.Payment.Result
    ): PaymentSheetEvent.Payment {
        return paymentMethodEvent(
            paymentSelection = CARD_PAYMENT_SELECTION.copy(
                paymentMethodExtraParams = paymentMethodExtraParams
            ),
            result = result
        )
    }

    private fun paymentMethodEvent(
        paymentSelection: PaymentSelection,
        result: PaymentSheetEvent.Payment.Result,
    ): PaymentSheetEvent.Payment {
        return PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = paymentSelection,
            duration = 1.milliseconds,
            result = result,
            currency = "usd",
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            googlePaySupported = false,
            deferredIntentConfirmationType = null,
        )
    }

    private val paymentIntentInitializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
        clientSecret = "cs_example"
    )

    private fun createInitEvent(
        configuration: PaymentSheet.Configuration,
    ): PaymentSheetEvent.Init {
        return PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = configuration.asCommonConfiguration(),
            appearance = configuration.appearance,
            primaryButtonColor = configuration.primaryButtonColorUsage(),
            configurationSpecificPayload = PaymentSheetEvent.ConfigurationSpecificPayload.PaymentSheet(configuration),
            googlePaySupported = true,
            isDeferred = false,
            isSpt = false,
            linkEnabled = false,
            isStripeCardScanAvailable = true,
            isAnalyticEventCallbackSet = false,
        )
    }

    private fun Any.asMap(): Map<*, *> {
        return this as Map<*, *>
    }

    private fun createLoadSucceededEvent(
        isDeferred: Boolean = false,
        isSpt: Boolean = false,
        linkEnabled: Boolean = false,
        linkMode: LinkMode? = null,
        googlePaySupported: Boolean = false,
        duration: Duration = (5L).seconds,
        paymentSelection: PaymentSelection? = null,
        initializationMode: PaymentElementLoader.InitializationMode = paymentIntentInitializationMode,
        orderedLpms: List<String> = listOf("card"),
        hasDefaultPaymentMethod: Boolean? = null,
        setAsDefaultEnabled: Boolean? = null,
        financialConnectionsAvailability: FinancialConnectionsAvailability = FinancialConnectionsAvailability.Full,
        linkDisplay: PaymentSheet.LinkConfiguration.Display = PaymentSheet.LinkConfiguration.Display.Automatic,
        paymentMethodOptionsSetupfutureUsage: Boolean = false,
        setupFutureUsage: StripeIntent.Usage? = null
    ): PaymentSheetEvent.LoadSucceeded {
        return PaymentSheetEvent.LoadSucceeded(
            isDeferred = isDeferred,
            isSpt = isSpt,
            linkEnabled = linkEnabled,
            linkMode = linkMode,
            googlePaySupported = googlePaySupported,
            duration = duration,
            paymentSelection = paymentSelection,
            initializationMode = initializationMode,
            orderedLpms = orderedLpms,
            hasDefaultPaymentMethod = hasDefaultPaymentMethod,
            setAsDefaultEnabled = setAsDefaultEnabled,
            linkDisplay = linkDisplay,
            financialConnectionsAvailability = financialConnectionsAvailability,
            paymentMethodOptionsSetupFutureUsage = paymentMethodOptionsSetupfutureUsage,
            setupFutureUsage = setupFutureUsage
        )
    }

    private fun buildInitMpeConfig(
        customer: Boolean = false,
        customerAccessProvider: String? = null,
        googlePay: Boolean = false,
        primaryButtonColor: Boolean? = false,
        defaultBillingDetails: Boolean = false,
        allowsDelayedPaymentMethods: Boolean = false,
        appearanceMap: Map<String, Any?> = buildAppearanceMap(false),
        paymentMethodOrder: List<String> = listOf(),
        allowPaymentMethodsRequiringShippingAddress: Boolean = false,
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        billingDetailsCollectionConfiguration: Map<String, Any?> = billingDetailsCollectionConfigurationDefault,
        preferredNetworks: String? = null,
        externalPaymentMethods: List<String>? = null,
        customPaymentMethods: String? = null,
        paymentMethodLayout: String? = "horizontal",
        cardBrandAcceptance: Boolean = false,
        cardScanAvailable: Boolean = true,
        analyticCallbackSet: Boolean = false,
        extraParams: MutableMap<String, Any?>.() -> Unit = {},
    ): Map<String, Any?> {
        return mutableMapOf(
            "customer" to customer,
            "customer_access_provider" to customerAccessProvider,
            "googlepay" to googlePay,
            "primary_button_color" to primaryButtonColor,
            "default_billing_details" to defaultBillingDetails,
            "allows_delayed_payment_methods" to allowsDelayedPaymentMethods,
            "appearance" to appearanceMap,
            "payment_method_order" to paymentMethodOrder,
            "allows_payment_methods_requiring_shipping_address" to allowPaymentMethodsRequiringShippingAddress,
            "allows_removal_of_last_saved_payment_method" to allowsRemovalOfLastSavedPaymentMethod,
            "billing_details_collection_configuration" to billingDetailsCollectionConfiguration,
            "preferred_networks" to preferredNetworks,
            "external_payment_methods" to externalPaymentMethods,
            "custom_payment_methods" to customPaymentMethods,
            "card_brand_acceptance" to cardBrandAcceptance,
            "card_scan_available" to cardScanAvailable,
            "analytic_callback_set" to analyticCallbackSet,
        ).apply {
            if (paymentMethodLayout != null) {
                put("payment_method_layout", paymentMethodLayout)
            }
            extraParams()
        }
    }

    private fun buildAppearanceMap(usedParams: Boolean, embeddedConfig: Map<String, Any>? = null): Map<String, Any?> {
        return mapOf(
            "colorsLight" to usedParams,
            "colorsDark" to usedParams,
            "corner_radius" to usedParams,
            "border_width" to usedParams,
            "font" to usedParams,
            "size_scale_factor" to usedParams,
            "primary_button" to mapOf(
                "colorsLight" to usedParams,
                "colorsDark" to usedParams,
                "corner_radius" to usedParams,
                "border_width" to usedParams,
                "font" to usedParams,
            ),
            "embedded_payment_element" to embeddedConfig,
            "usage" to (usedParams || embeddedConfig != null),
        )
    }

    private val billingDetailsCollectionConfigurationDefault = mapOf(
        "attach_defaults" to false,
        "name" to "Automatic",
        "email" to "Automatic",
        "phone" to "Automatic",
        "address" to "Automatic",
    )
}
