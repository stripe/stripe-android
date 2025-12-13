package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Flag
import com.stripe.android.model.LinkDisabledReason
import com.stripe.android.model.LinkMode
import com.stripe.android.model.LinkSignupDisabledReason
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import org.junit.Test
import javax.inject.Provider

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@Suppress("LargeClass")
class DefaultAnalyticsMetadataFactoryTest {

    @Test
    fun `create returns expected analytics metadata map`() = runScenario {
        val resultMap = createAnalyticsMetadata()

        assertThat(resultMap["intent_type"]).isEqualTo("payment_intent")
        assertThat(resultMap["is_decoupled"]).isEqualTo(false)
        assertThat(resultMap["is_spt"]).isEqualTo(false)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(false)

        assertThat(resultMap["intent_id"]).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id)
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap["currency"]).isEqualTo("usd")
        assertThat(resultMap).doesNotContainKey("setup_future_usage")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["google_pay_enabled"]).isEqualTo(false)

        assertThat(resultMap["link_enabled"]).isEqualTo(false)

        assertThat(resultMap["set_as_default_enabled"]).isEqualTo(false)
        assertThat(resultMap["has_default_payment_method"]).isEqualTo(false)

        assertThat(resultMap).containsKey("mpe_config")
        assertThat((resultMap["mpe_config"] as? Map<*, *>)?.get("customer")).isEqualTo(false)

        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        val appearance = mpeConfig?.get("appearance") as? Map<*, *>
        assertThat(appearance).doesNotContainKey("embedded_payment_element")
    }

    @Test
    fun `create returns expected values for payment intent`() = runScenario {
        val clientSecret = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.clientSecret!!
        val resultMap = createAnalyticsMetadata(
            initializationMode = InitializationMode.PaymentIntent(clientSecret),
            integrationMetadata = IntegrationMetadata.IntentFirst(clientSecret),
            elementsSession = createElementsSession(stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        )

        assertThat(resultMap["intent_type"]).isEqualTo("payment_intent")
        assertThat(resultMap["intent_id"]).isEqualTo(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.id)
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap["currency"]).isEqualTo("usd")
        assertThat(resultMap).doesNotContainKey("setup_future_usage")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["is_decoupled"]).isEqualTo(false)
        assertThat(resultMap["is_spt"]).isEqualTo(false)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(false)
    }

    @Test
    fun `create returns expected values for setup intent`() = runScenario {
        val clientSecret = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.clientSecret!!
        val resultMap = createAnalyticsMetadata(
            initializationMode = InitializationMode.SetupIntent(clientSecret),
            integrationMetadata = IntegrationMetadata.IntentFirst(clientSecret),
            elementsSession = createElementsSession(stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD)
        )

        assertThat(resultMap["intent_type"]).isEqualTo("setup_intent")
        assertThat(resultMap["intent_id"]).isEqualTo(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.id)
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap).doesNotContainKey("currency")
        assertThat(resultMap["setup_future_usage"]).isEqualTo("off_session")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["is_decoupled"]).isEqualTo(false)
        assertThat(resultMap["is_spt"]).isEqualTo(false)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(false)
    }

    @Test
    fun `create returns expected values for deferred payment intent`() = runScenario {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.asDeferred()
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 5000,
                currency = intent.currency!!
            )
        )
        val resultMap = createAnalyticsMetadata(
            initializationMode = InitializationMode.DeferredIntent(intentConfiguration),
            integrationMetadata = IntegrationMetadata.DeferredIntentWithPaymentMethod(intentConfiguration),
            elementsSession = createElementsSession(stripeIntent = intent)
        )

        assertThat(resultMap["intent_type"]).isEqualTo("deferred_payment_intent")
        assertThat(resultMap).doesNotContainKey("intent_id")
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap["currency"]).isEqualTo("usd")
        assertThat(resultMap).doesNotContainKey("setup_future_usage")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["is_decoupled"]).isEqualTo(true)
        assertThat(resultMap["is_spt"]).isEqualTo(false)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(false)
    }

    @Test
    fun `create returns expected values for deferred setup intent`() = runScenario {
        val intent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.asDeferred()
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                currency = "usd"
            )
        )
        val resultMap = createAnalyticsMetadata(
            initializationMode = InitializationMode.DeferredIntent(intentConfiguration),
            integrationMetadata = IntegrationMetadata.DeferredIntentWithConfirmationToken(intentConfiguration),
            elementsSession = createElementsSession(stripeIntent = intent)
        )

        assertThat(resultMap["intent_type"]).isEqualTo("deferred_setup_intent")
        assertThat(resultMap).doesNotContainKey("intent_id")
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap).doesNotContainKey("currency")
        assertThat(resultMap["setup_future_usage"]).isEqualTo("off_session")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["is_decoupled"]).isEqualTo(true)
        assertThat(resultMap["is_spt"]).isEqualTo(false)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(true)
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `create returns expected values for shared payment tokens`() = runScenario {
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.asDeferred()
        val intentConfiguration = PaymentSheet.IntentConfiguration(
            mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                amount = 5000,
                currency = "usd",
            ),
            intentBehavior = PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken(
                sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                    businessName = "Jays Shop",
                    networkId = "",
                    externalId = "",
                )
            )
        )
        val resultMap = createAnalyticsMetadata(
            initializationMode = InitializationMode.DeferredIntent(intentConfiguration),
            integrationMetadata = IntegrationMetadata.DeferredIntentWithSharedPaymentToken(intentConfiguration),
            elementsSession = createElementsSession(stripeIntent = intent)
        )

        assertThat(resultMap["intent_type"]).isEqualTo("deferred_payment_intent")
        assertThat(resultMap).doesNotContainKey("intent_id")
        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(false)
        assertThat(resultMap["currency"]).isEqualTo("usd")
        assertThat(resultMap).doesNotContainKey("setup_future_usage")
        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(false)

        assertThat(resultMap["is_decoupled"]).isEqualTo(true)
        assertThat(resultMap["is_spt"]).isEqualTo(true)
        assertThat(resultMap["is_confirmation_tokens"]).isEqualTo(false)
    }

    @Test
    fun `create returns expected values for default payment methods`() = runScenario {
        val resultMap = createAnalyticsMetadata(
            customerMetadata = createCustomerMetadata(isPaymentMethodSetAsDefaultEnabled = true)
        )

        assertThat(resultMap["set_as_default_enabled"]).isEqualTo(true)
        assertThat(resultMap["has_default_payment_method"]).isEqualTo(false)
    }

    @Test
    fun `create returns false for default payment methods`() = runScenario {
        val resultMap = createAnalyticsMetadata(
            customerMetadata = createCustomerMetadata(isPaymentMethodSetAsDefaultEnabled = false),
            elementsSession = createElementsSession(customer = createElementsSessionCustomer())
        )

        assertThat(resultMap["set_as_default_enabled"]).isEqualTo(false)
        assertThat(resultMap["has_default_payment_method"]).isEqualTo(true)
    }

    @Test
    fun `create returns true for default payment methods`() = runScenario {
        val resultMap = createAnalyticsMetadata(
            customerMetadata = createCustomerMetadata(isPaymentMethodSetAsDefaultEnabled = true),
            elementsSession = createElementsSession(customer = createElementsSessionCustomer())
        )

        assertThat(resultMap["set_as_default_enabled"]).isEqualTo(true)
        assertThat(resultMap["has_default_payment_method"]).isEqualTo(true)
    }

    @Test
    fun `create returns expected values for default billing details collection configuration`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
        )

        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig).containsKey("billing_details_collection_configuration")
        val billingConfig = mpeConfig?.get("billing_details_collection_configuration") as? Map<*, *>

        assertThat(billingConfig?.get("attach_defaults")).isEqualTo(false)
        assertThat(billingConfig?.get("name")).isEqualTo("Automatic")
        assertThat(billingConfig?.get("email")).isEqualTo("Automatic")
        assertThat(billingConfig?.get("phone")).isEqualTo("Automatic")
        assertThat(billingConfig?.get("address")).isEqualTo("Automatic")
        assertThat(billingConfig?.get("allowed_countries")).isNull()
    }

    @Test
    fun `create returns expected values for custom billing details collection configuration`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = true,
                allowedCountries = setOf("US", "CA", "UK", "JP")
            )
        )

        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig).containsKey("billing_details_collection_configuration")
        val billingConfig = mpeConfig?.get("billing_details_collection_configuration") as? Map<*, *>

        assertThat(billingConfig?.get("attach_defaults")).isEqualTo(true)
        assertThat(billingConfig?.get("name")).isEqualTo("Always")
        assertThat(billingConfig?.get("email")).isEqualTo("Never")
        assertThat(billingConfig?.get("phone")).isEqualTo("Always")
        assertThat(billingConfig?.get("address")).isEqualTo("Full")
        assertThat(billingConfig?.get("allowed_countries")).isEqualTo("US,CA,UK,JP")
    }

    @Test
    fun `create returns expected values when link is enabled with passthrough mode`() = runScenario {
        val linkState = createLinkState()
        val resultMap = createAnalyticsMetadata(
            elementsSession = createElementsSession(
                linkSettings = createLinkSettings(linkMode = LinkMode.Passthrough)
            ),
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_mode"]).isEqualTo("passthrough")
        assertThat(resultMap["link_display"]).isEqualTo("automatic")
        assertThat(resultMap).doesNotContainKey("link_disabled_reasons")
        assertThat(resultMap).doesNotContainKey("link_signup_disabled_reasons")
    }

    @Test
    fun `create returns expected values when link is enabled with payment method mode`() = runScenario {
        val linkState = createLinkState()
        val resultMap = createAnalyticsMetadata(
            elementsSession = createElementsSession(
                linkSettings = createLinkSettings(linkMode = LinkMode.LinkPaymentMethod)
            ),
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_mode"]).isEqualTo("payment_method_mode")
    }

    @Test
    fun `create returns expected values when link is enabled with card brand mode`() = runScenario {
        val linkState = createLinkState()
        val resultMap = createAnalyticsMetadata(
            elementsSession = createElementsSession(
                linkSettings = createLinkSettings(linkMode = LinkMode.LinkCardBrand)
            ),
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_mode"]).isEqualTo("link_card_brand")
    }

    @Test
    fun `create returns expected values when link is disabled`() = runScenario {
        val linkDisabledState = LinkDisabledState(
            linkDisabledReasons = listOf(
                LinkDisabledReason.NotSupportedInElementsSession,
                LinkDisabledReason.LinkConfiguration
            )
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkDisabledState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(false)
        assertThat(resultMap["link_disabled_reasons"])
            .isEqualTo("not_supported_in_elements_session,link_configuration")
        assertThat(resultMap).doesNotContainKey("link_signup_disabled_reasons")
    }

    @Test
    fun `create returns expected values when link is disabled due to card brand filtering`() = runScenario {
        val linkDisabledState = LinkDisabledState(
            linkDisabledReasons = listOf(LinkDisabledReason.CardBrandFiltering)
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkDisabledState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(false)
        assertThat(resultMap["link_disabled_reasons"]).isEqualTo("card_brand_filtering")
    }

    @Test
    fun `create returns expected values when link is disabled due to billing details collection`() = runScenario {
        val linkDisabledState = LinkDisabledState(
            linkDisabledReasons = listOf(LinkDisabledReason.BillingDetailsCollection)
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkDisabledState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(false)
        assertThat(resultMap["link_disabled_reasons"]).isEqualTo("billing_details_collection")
    }

    @Test
    fun `create returns expected values when link signup is disabled`() = runScenario {
        val linkState = createLinkState(
            signupModeResult = LinkSignupModeResult.Disabled(
                disabledReasons = listOf(
                    LinkSignupDisabledReason.LinkCardNotSupported,
                    LinkSignupDisabledReason.DisabledInElementsSession
                )
            )
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_signup_disabled_reasons"])
            .isEqualTo("link_card_not_supported,disabled_in_elements_session")
    }

    @Test
    fun `create returns expected values when link signup is disabled due to no email`() = runScenario {
        val linkState = createLinkState(
            signupModeResult = LinkSignupModeResult.Disabled(
                disabledReasons = listOf(LinkSignupDisabledReason.SignupOptInFeatureNoEmailProvided)
            )
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_signup_disabled_reasons"])
            .isEqualTo("signup_opt_in_feature_no_email_provided")
    }

    @Test
    fun `create returns expected values when link signup is disabled due to link used before`() = runScenario {
        val linkState = createLinkState(
            signupModeResult = LinkSignupModeResult.Disabled(
                disabledReasons = listOf(LinkSignupDisabledReason.LinkUsedBefore)
            )
        )
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_enabled"]).isEqualTo(true)
        assertThat(resultMap["link_signup_disabled_reasons"]).isEqualTo("link_used_before")
    }

    @Test
    fun `create returns expected values for link display never`() = runScenario {
        val linkState = createLinkState()
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Test Merchant")
            .link(
                PaymentSheet.LinkConfiguration(
                    display = PaymentSheet.LinkConfiguration.Display.Never
                )
            )
            .build()
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration),
            linkStateResult = linkState,
        )

        assertThat(resultMap["link_display"]).isEqualTo("never")
    }

    @Test
    fun `create returns expected values for FC SDK availability full`() = runScenario {
        val linkState = createLinkState()
        val resultMap = createAnalyticsMetadata(
            linkStateResult = linkState,
        )

        assertThat(resultMap["fc_sdk_availability"]).isEqualTo("FULL")
    }

    @Test
    fun `create returns true for payment method options setup future usage`() = runScenario {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {"card":{"setup_future_usage":"off_session"}}
            """.trimIndent()
        )
        val resultMap = createAnalyticsMetadata(
            elementsSession = createElementsSession(stripeIntent = paymentIntent)
        )

        assertThat(resultMap["payment_method_options_setup_future_usage"]).isEqualTo(true)
    }

    @Test
    fun `create returns true for require cvc recollection`() = runScenario {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodOptionsJsonString = """
                {"card":{"require_cvc_recollection":true}}
            """.trimIndent()
        )

        val resultMap = createAnalyticsMetadata(
            elementsSession = createElementsSession(stripeIntent = paymentIntent)
        )

        assertThat(resultMap["require_cvc_recollection"]).isEqualTo(true)
    }

    @Test
    fun `create returns external payment methods`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
            externalPaymentMethods = listOf("external_pm1", "external_pm2"),
        )
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig?.get("external_payment_methods")).isEqualTo("external_pm1,external_pm2")
    }

    @OptIn(ExperimentalCustomPaymentMethodsApi::class)
    @Test
    fun `create returns custom payment methods`() = runScenario {
        val customPaymentMethod = ElementsSession.CustomPaymentMethod.Available(
            type = "custom_type",
            displayName = "Custom PM",
            logoUrl = "",
        )
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Test Merchant")
            .customPaymentMethods(
                listOf(
                    PaymentSheet.CustomPaymentMethod(
                        id = "custom_pm_1",
                        subtitle = "Empty",
                        disableBillingDetailCollection = false,
                    )
                )
            )
            .build()
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration),
            elementsSession = createElementsSession(customPaymentMethods = listOf(customPaymentMethod))
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig?.get("custom_payment_methods")).isEqualTo("custom_pm_1")
    }

    @Test
    fun `create returns customer access provider`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_123",
                ephemeralKeySecret = "ek_test_123",
            ),
        )
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig?.get("customer_access_provider")).isEqualTo("legacy")
    }

    @Test
    fun `create returns preferred networks`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
            preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
        )
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig?.get("preferred_networks")).isEqualTo("cartes_bancaires,visa")
    }

    @Test
    fun `create returns default appearance values`() = runScenario {
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "Test Merchant",
        )
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig).containsKey("appearance")
        val appearance = mpeConfig?.get("appearance") as? Map<*, *>

        assertThat(appearance?.get("colorsLight")).isEqualTo(false)
        assertThat(appearance?.get("colorsDark")).isEqualTo(false)
        assertThat(appearance?.get("corner_radius")).isEqualTo(false)
        assertThat(appearance?.get("border_width")).isEqualTo(false)
        assertThat(appearance?.get("font")).isEqualTo(false)
        assertThat(appearance?.get("size_scale_factor")).isEqualTo(false)
        assertThat(appearance?.get("usage")).isEqualTo(false)

        assertThat(appearance).containsKey("primary_button")
        val primaryButton = appearance?.get("primary_button") as? Map<*, *>
        assertThat(primaryButton?.get("colorsLight")).isEqualTo(false)
        assertThat(primaryButton?.get("colorsDark")).isEqualTo(false)
        assertThat(primaryButton?.get("corner_radius")).isEqualTo(false)
        assertThat(primaryButton?.get("border_width")).isEqualTo(false)
        assertThat(primaryButton?.get("font")).isEqualTo(false)
    }

    @Test
    fun `create returns customized primary button shape and typography`() = runScenario {
        val configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Test Merchant")
            .appearance(
                PaymentSheet.Appearance(
                    primaryButton = PaymentSheet.PrimaryButton(
                        shape = PaymentSheet.PrimaryButtonShape(
                            cornerRadiusDp = 8.0f,
                            borderStrokeWidthDp = 1.5f,
                        ),
                        typography = PaymentSheet.PrimaryButtonTypography(
                            fontResId = 5
                        )
                    )
                )
            )
            .build()
        val resultMap = createAnalyticsMetadata(
            configuration = PaymentElementLoader.Configuration.PaymentSheet(configuration = configuration)
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        val appearance = mpeConfig?.get("appearance") as? Map<*, *>
        val primaryButton = appearance?.get("primary_button") as? Map<*, *>

        assertThat(primaryButton?.get("corner_radius")).isEqualTo(true)
        assertThat(primaryButton?.get("border_width")).isEqualTo(true)
        assertThat(primaryButton?.get("font")).isEqualTo(true)
        assertThat(appearance?.get("usage")).isEqualTo(true)
    }

    @Test
    fun `create returns default embedded appearance values`() = runScenario(mode = EventReporter.Mode.Embedded) {
        val configuration = PaymentElementLoader.Configuration.Embedded(
            configuration = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName = "Test Merchant")
                .build(),
            isRowSelectionImmediateAction = false,
        )
        val resultMap = createAnalyticsMetadata(
            configuration = configuration
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        assertThat(mpeConfig).containsKey("appearance")
        val appearance = mpeConfig?.get("appearance") as? Map<*, *>

        assertThat(appearance).containsKey("embedded_payment_element")
        val embeddedAppearance = appearance?.get("embedded_payment_element") as? Map<*, *>
        assertThat(embeddedAppearance?.get("style")).isEqualTo(false)
        assertThat(embeddedAppearance?.get("row_style")).isEqualTo("flat_with_radio")

        // usage should be false since embedded appearance is default
        assertThat(appearance?.get("usage")).isEqualTo(false)
    }

    @Test
    fun `create returns customized embedded appearance with floating button style`() = runScenario(
        mode = EventReporter.Mode.Embedded
    ) {
        val configuration = PaymentElementLoader.Configuration.Embedded(
            configuration = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName = "Test Merchant")
                .appearance(
                    PaymentSheet.Appearance(
                        embeddedAppearance = PaymentSheet.Appearance.Embedded(
                            style = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
                        )
                    )
                )
                .build(),
            isRowSelectionImmediateAction = false,
        )
        val resultMap = createAnalyticsMetadata(
            configuration = configuration
        )

        assertThat(resultMap).containsKey("mpe_config")
        val mpeConfig = resultMap["mpe_config"] as? Map<*, *>
        val appearance = mpeConfig?.get("appearance") as? Map<*, *>
        val embeddedAppearance = appearance?.get("embedded_payment_element") as? Map<*, *>

        assertThat(embeddedAppearance?.get("style")).isEqualTo(true)
        assertThat(embeddedAppearance?.get("row_style")).isEqualTo("floating_button")
        assertThat(appearance?.get("usage")).isEqualTo(true)
    }

    @Test
    fun `create returns true for link_native_available when native link is available`() = runScenario {
        val fakeLinkGate = FakeLinkGate().apply {
            setUseNativeLink(true)
        }
        val linkGateFactory = FakeLinkGate.Factory(fakeLinkGate)

        val resultMap = createAnalyticsMetadata(
            linkGateFactory = linkGateFactory
        )

        assertThat(resultMap["link_native_available"]).isEqualTo(true)
    }

    private fun runScenario(
        mode: EventReporter.Mode = EventReporter.Mode.Complete,
        block: Scenario.() -> Unit
    ) {
        val cvcRecollectionHandler = CvcRecollectionHandlerImpl()
        val analyticEventCallbackProvider = Provider<AnalyticEventCallback?> { null }

        Scenario(
            cvcRecollectionHandler = cvcRecollectionHandler,
            analyticEventCallbackProvider = analyticEventCallbackProvider,
            mode = mode,
        ).apply {
            block()
        }
    }

    private data class Scenario(
        val cvcRecollectionHandler: CvcRecollectionHandler,
        val analyticEventCallbackProvider: Provider<AnalyticEventCallback?>,
        val mode: EventReporter.Mode,
    )

    private fun Scenario.createFactory(
        linkGateFactory: FakeLinkGate.Factory = FakeLinkGate.Factory()
    ): DefaultAnalyticsMetadataFactory {
        return DefaultAnalyticsMetadataFactory(
            cvcRecollectionHandler = cvcRecollectionHandler,
            mode = mode,
            analyticEventCallbackProvider = analyticEventCallbackProvider,
            linkGateFactory = linkGateFactory,
        )
    }

    private fun Scenario.createAnalyticsMetadata(
        initializationMode: InitializationMode = InitializationMode.PaymentIntent(
            clientSecret = "pi_test_secret"
        ),
        integrationMetadata: IntegrationMetadata = IntegrationMetadata.IntentFirst(clientSecret = "pi_test_secret"),
        elementsSession: ElementsSession = createElementsSession(),
        isGooglePaySupported: Boolean = false,
        configuration: PaymentElementLoader.Configuration = PaymentElementLoader.Configuration.PaymentSheet(
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Test Merchant",
            )
        ),
        customerMetadata: CustomerMetadata? = null,
        linkStateResult: LinkStateResult? = null,
        linkGateFactory: FakeLinkGate.Factory = FakeLinkGate.Factory(),
    ): Map<String, Any?> {
        val factory = createFactory(linkGateFactory = linkGateFactory)
        return factory.create(
            initializationMode = initializationMode,
            integrationMetadata = integrationMetadata,
            elementsSession = elementsSession,
            isGooglePaySupported = isGooglePaySupported,
            configuration = configuration,
            customerMetadata = customerMetadata,
            linkStateResult = linkStateResult,
        ).paramsMap
    }

    private fun createElementsSession(
        linkSettings: ElementsSession.LinkSettings? = null,
        externalPaymentMethodData: String? = null,
        stripeIntent: StripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        flags: Map<Flag, Boolean> = emptyMap(),
        customer: ElementsSession.Customer? = null,
        cardBrandChoice: ElementsSession.CardBrandChoice? = null,
        isGooglePayEnabled: Boolean = false,
        customPaymentMethods: List<ElementsSession.CustomPaymentMethod> = emptyList(),
    ): ElementsSession {
        return ElementsSession(
            linkSettings = linkSettings,
            paymentMethodSpecs = null,
            externalPaymentMethodData = externalPaymentMethodData,
            stripeIntent = stripeIntent,
            orderedPaymentMethodTypesAndWallets = stripeIntent.paymentMethodTypes,
            flags = flags,
            experimentsData = null,
            customer = customer,
            merchantCountry = null,
            merchantLogoUrl = null,
            cardBrandChoice = cardBrandChoice,
            isGooglePayEnabled = isGooglePayEnabled,
            customPaymentMethods = customPaymentMethods,
            elementsSessionId = "elements_session_1",
            passiveCaptcha = null,
            elementsSessionConfigId = null,
            accountId = null,
            merchantId = null,
        )
    }

    private fun createCustomerMetadata(
        isPaymentMethodSetAsDefaultEnabled: Boolean
    ): CustomerMetadata = CustomerMetadata(
        id = "cus_1234",
        ephemeralKeySecret = "ek_123",
        customerSessionClientSecret = "cuss_132_secret_123",
        isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
        permissions = CustomerMetadata.Permissions(
            removePaymentMethod = PaymentMethodRemovePermission.Full,
            canRemoveLastPaymentMethod = true,
            canRemoveDuplicates = true,
            canUpdateFullPaymentMethodDetails = true,
        )
    )

    private fun createElementsSessionCustomer(): ElementsSession.Customer {
        return ElementsSession.Customer(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            defaultPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.id,
            session = ElementsSession.Customer.Session(
                id = "cuss_123",
                apiKey = "ek_test_1234",
                apiKeyExpiry = 1713890664,
                customerId = "cus_1",
                liveMode = false,
                components = ElementsSession.Customer.Components(
                    mobilePaymentElement = ElementsSession.Customer.Components.MobilePaymentElement.Enabled(
                        isPaymentMethodSaveEnabled = false,
                        paymentMethodRemove =
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                        paymentMethodRemoveLast =
                        ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                        allowRedisplayOverride = PaymentMethod.AllowRedisplay.LIMITED,
                        isPaymentMethodSetAsDefaultEnabled = false,
                    ),
                    customerSheet = ElementsSession.Customer.Components.CustomerSheet.Enabled(
                        paymentMethodRemove =
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled,
                        paymentMethodRemoveLast =
                        ElementsSession.Customer.Components.PaymentMethodRemoveLastFeature.Enabled,
                        isPaymentMethodSyncDefaultEnabled = false,
                    ),
                )
            )
        )
    }

    private fun createLinkSettings(
        linkMode: LinkMode,
    ): ElementsSession.LinkSettings {
        return ElementsSession.LinkSettings(
            linkFundingSources = emptyList(),
            linkPassthroughModeEnabled = linkMode == LinkMode.Passthrough,
            linkMode = linkMode,
            linkFlags = emptyMap(),
            disableLinkSignup = false,
            linkConsumerIncentive = null,
            useAttestationEndpoints = false,
            suppress2faModal = false,
            disableLinkRuxInFlowController = false,
            linkEnableDisplayableDefaultValuesInEce = false,
            linkMobileSkipWalletInFlowController = false,
            linkSignUpOptInFeatureEnabled = false,
            linkSignUpOptInInitialValue = false,
            linkSupportedPaymentMethodsOnboardingEnabled = emptyList(),
        )
    }

    private fun createLinkState(
        signupModeResult: LinkSignupModeResult = LinkSignupModeResult.Enabled(
            LinkSignupMode.InsteadOfSaveForFutureUse
        )
    ): LinkState {
        return LinkState(
            configuration = LinkConfiguration(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                merchantName = "Test Merchant",
                sellerBusinessName = null,
                merchantCountryCode = "US",
                merchantLogoUrl = null,
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = null,
                    email = "test@example.com",
                    phone = null,
                    billingCountryCode = null,
                ),
                shippingDetails = null,
                passthroughModeEnabled = false,
                cardBrandChoice = null,
                cardBrandFilter = DefaultCardBrandFilter,
                financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
                flags = emptyMap(),
                useAttestationEndpointsForLink = false,
                suppress2faModal = false,
                disableRuxInFlowController = false,
                enableDisplayableDefaultValuesInEce = false,
                linkSignUpOptInFeatureEnabled = false,
                linkSignUpOptInInitialValue = false,
                elementsSessionId = "elements_session_1",
                linkMode = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                defaultBillingDetails = null,
                allowDefaultOptIn = false,
                googlePlacesApiKey = null,
                collectMissingBillingDetailsForExistingPaymentMethods = false,
                allowUserEmailEdits = true,
                allowLogOut = true,
                skipWalletInFlowController = false,
                customerId = null,
                linkAppearance = null,
                saveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
                forceSetupFutureUseBehaviorAndNewMandate = false,
                linkSupportedPaymentMethodsOnboardingEnabled = emptyList(),
                clientAttributionMetadata = PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA,
                cardFundingFilter = PaymentSheetCardFundingFilter(PaymentSheet.CardFundingType.entries),
            ),
            loginState = LinkState.LoginState.LoggedOut,
            signupModeResult = signupModeResult,
        )
    }

    private fun PaymentIntent.asDeferred(): PaymentIntent = copy(id = null, clientSecret = null)

    private fun SetupIntent.asDeferred(): SetupIntent = copy(id = null, clientSecret = null)
}
