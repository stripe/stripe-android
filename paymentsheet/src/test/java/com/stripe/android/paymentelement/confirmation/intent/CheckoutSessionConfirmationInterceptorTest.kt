package com.stripe.android.paymentelement.confirmation.intent

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.challenge.passive.PassiveChallengeActivityContract
import com.stripe.android.challenge.passive.PassiveChallengeActivityResult
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.doesNotContainBodyPartsWithPrefix
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.CONFIRMATION_PARAMETERS
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeIsEligibleForConfirmationChallenge
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.challenge.PassiveChallengeConfirmationDefinition
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.CheckoutStripeRepositoryFactory
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.FakeFraudDetectionDataRepository
import com.stripe.android.utils.FakePassiveChallengeWarmer
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@Suppress("LargeClass")
@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestParameterInjector::class)
class CheckoutSessionConfirmationInterceptorTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `successful confirmation ignores an earlier payment error`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("status", "open").put("payment_status", "unpaid")
                json.getJSONObject("payment_response").put(
                    "last_payment_error",
                    JSONObject().put("message", "An earlier decline").put("code", "card_declined"),
                )
            }
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Complete
        val response = requireNotNull(result.metadata[CheckoutSessionResponseKey])

        assertThat(response.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(response.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.PAID)
    }

    @Test
    fun `requires action without an intent fails explicitly`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("payment_response", JSONObject().put("status", "requires_action"))
            }
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Fail

        assertThat(result.cause.message).isEqualTo("CheckoutClient requires an action without a supported Intent.")
    }

    @Test
    fun `expired confirmation fails explicitly`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("status", "expired")
                json.remove("payment_response")
            }
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Fail

        assertThat(result.cause.message).isEqualTo("Checkout Session unexpectedly expired after confirmation.")
    }

    @Test
    fun `requires payment method without a payment error fails explicitly`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("payment_response", JSONObject().put("status", "requires_payment_method"))
            }
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Fail

        assertThat(result.cause.message).isEqualTo("Checkout requires a payment method.")
    }

    @Test
    fun `new card challenge token is moved to Checkout confirm and attestation is preserved`(
        @TestParameter(value = ["captcha_token", "", "null"]) token: String?,
    ) = runScenario {
        val option = completeChallenge(
            option = NEW_PM_OPTION.copy(
                createParams = NEW_PM_OPTION.createParams.copy(
                    radarOptions = RadarOptions(
                        hCaptchaToken = null,
                        androidVerificationObject = AndroidVerificationObject(
                            appId = "com.stripe.test",
                            androidVerificationToken = "attestation_token",
                        ),
                    ),
                ),
            ),
            token = token,
        ) as PaymentMethodConfirmationOption.New
        assertThat(option.createParams.radarOptions?.toParamMap()?.get("hcaptcha_token")).isEqualTo(token)
        assertThat(option.confirmationChallengeState.passiveChallengeComplete).isTrue()

        networkRule.checkoutConfirm(
            captchaMatcher(token),
            not(hasBodyPart("payment_method_data[radar_options][hcaptcha_token]")),
            bodyPart("payment_method_data[radar_options][android_verification_object][app_id]", "com.stripe.test"),
            bodyPart(
                "payment_method_data[radar_options][android_verification_object][android_verification_token]",
                "attestation_token",
            ),
            bodyPart("payment_method_data[card][number]", "4242424242424242"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptor.intercept(PaymentIntentFactory.create(), option, shippingValues = null)

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `saved and transformed card challenge tokens reach Checkout confirm`(
        @TestParameter transformed: Boolean,
        @TestParameter(value = ["captcha_token", "", "null"]) token: String?,
    ) = runScenario {
        val option = completeChallenge(
            option = SAVED_PM_OPTION.copy(newPMTransformedForConfirmation = transformed),
            token = token,
        ) as PaymentMethodConfirmationOption.Saved
        assertThat(option.confirmationChallengeState.hCaptchaToken).isEqualTo(token)
        assertThat(option.confirmationChallengeState.passiveChallengeComplete).isTrue()

        networkRule.checkoutConfirm(
            captchaMatcher(token),
            bodyPart("payment_method", option.paymentMethod.id),
            doesNotContainBodyPartsWithPrefix("payment_method_data"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptor.intercept(PaymentIntentFactory.create(), option, shippingValues = null)

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `confirmation sends actual payment method type`(
        @TestParameter saved: Boolean,
        @TestParameter(value = ["Card", "Link", "SepaDebit"]) type: PaymentMethod.Type,
        @TestParameter setup: Boolean,
    ) {
        val paymentMethod = when (type) {
            PaymentMethod.Type.Card -> PaymentMethodFixtures.CARD_PAYMENT_METHOD
            PaymentMethod.Type.Link -> PaymentMethodFixtures.LINK_PAYMENT_METHOD
            PaymentMethod.Type.SepaDebit -> PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
            else -> error("Unexpected test type: $type")
        }
        val createParams = when (type) {
            PaymentMethod.Type.Card -> PaymentMethodCreateParamsFixtures.DEFAULT_CARD.copy(
                card = PaymentMethodCreateParamsFixtures.CARD_WITH_ATTRIBUTION,
            )
            PaymentMethod.Type.Link -> PaymentMethodCreateParams.createLink(
                paymentDetailsId = "payment_details_123",
                consumerSessionClientSecret = "consumer_session_secret",
                clientAttributionMetadata = ClientAttributionMetadata(
                    elementsSessionConfigId = "test_session_id",
                    paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                    paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
                    checkoutSessionId = null,
                ),
            )
            PaymentMethod.Type.SepaDebit -> PaymentMethodCreateParamsFixtures.DEFAULT_SEPA_DEBIT
            else -> error("Unexpected test type: $type")
        }

        val paymentMethodMatchers = createPaymentMethodMatchers(
            saved = saved,
            type = type,
            paymentMethod = paymentMethod,
            createParams = createParams,
        )

        runScenario {
            networkRule.checkoutConfirm(
                *paymentMethodMatchers.toTypedArray(),
                bodyPart("expected_payment_method_type", type.code),
                *commonRequestMatchers(),
            ) { response ->
                response.testBodyFromFile(
                    if (setup) "checkout-session-confirm-setup.json" else "checkout-session-confirm.json"
                )
            }

            val intent = if (setup) SetupIntentFactory.create() else PaymentIntentFactory.create()
            val result = if (saved) {
                interceptor.intercept(
                    intent = intent,
                    confirmationOption = SAVED_PM_OPTION.copy(paymentMethod = paymentMethod),
                    shippingValues = null,
                )
            } else {
                interceptor.intercept(
                    intent = intent,
                    confirmationOption = NEW_PM_OPTION.copy(createParams = createParams),
                    shippingValues = null,
                )
            }

            assertThat(result)
                .isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
        }
    }

    @Test
    fun `web Link confirmation without email sends Link type`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("payment_method", PaymentMethodFixtures.LINK_PAYMENT_METHOD.id),
            bodyPart("expected_payment_method_type", "link"),
            doesNotContainBodyPartsWithPrefix("collected_information"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = SAVED_PM_OPTION.copy(
                paymentMethod = PaymentMethodFixtures.LINK_PAYMENT_METHOD,
                originatedFromWallet = true,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `Link passthrough confirmation without email sends card type`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("payment_method", PaymentMethodFixtures.CARD_PAYMENT_METHOD.id),
            bodyPart("expected_payment_method_type", "card"),
            doesNotContainBodyPartsWithPrefix("collected_information"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptor.intercept(
            intent = PaymentIntentFactory.create(),
            confirmationOption = SAVED_PM_OPTION.copy(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(isLinkPassthroughMode = true),
                originatedFromWallet = true,
            ),
            shippingValues = null,
        )

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `saved payment method confirmation completes without expected type when unavailable`(
        @TestParameter unrecognized: Boolean,
    ) {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            type = null,
            code = if (unrecognized) "unrecognized_payment_method" else null,
        )

        runScenario {
            networkRule.checkoutConfirm(
                bodyPart("payment_method", paymentMethod.id),
                not(hasBodyPart("expected_payment_method_type")),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            val result = interceptor.intercept(
                intent = PaymentIntentFactory.create(),
                confirmationOption = SAVED_PM_OPTION.copy(paymentMethod = paymentMethod),
                shippingValues = null,
            )

            assertThat(result)
                .isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
        }
    }

    @Test
    fun `confirmation sends collected email`(
        @TestParameter saved: Boolean,
        @TestParameter setup: Boolean,
    ) = runScenario(collectedEmail = "local+checkout@example.com") {
        networkRule.checkoutConfirm(
            bodyPart("collected_information[email]", "local+checkout@example.com"),
        ) { response ->
            response.testBodyFromFile(
                if (setup) "checkout-session-confirm-setup.json" else "checkout-session-confirm.json"
            )
        }
        val intent = if (setup) SetupIntentFactory.create() else PaymentIntentFactory.create()
        if (saved) interceptSavedPm(intent = intent) else interceptNewPm(intent = intent)
    }

    @Test
    fun `confirmation omits collected email`(
        @TestParameter saved: Boolean,
        @TestParameter reason: EmailOmissionReason,
    ) = runScenario(
        collectedEmail = "local@example.com".takeUnless { reason == EmailOmissionReason.Absent },
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(
            customerEmail = "fixed@example.com".takeIf { reason == EmailOmissionReason.Session },
        ).let { response ->
            if (reason == EmailOmissionReason.Customer) {
                response.copy(
                    customer = CheckoutSessionResponse.Customer(
                        id = "cus_test",
                        email = "fixed@example.com",
                        paymentMethods = emptyList(),
                        canDetachPaymentMethod = false,
                    ),
                )
            } else {
                response
            }
        },
    ) {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("collected_information"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }
        if (saved) interceptSavedPm() else interceptNewPm()
    }

    enum class EmailOmissionReason { Absent, Session, Customer }

    @Test
    fun `intercept with succeeded payment intent returns Complete action`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("status", "complete")
            }
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
        assertThat((completeAction.intent as PaymentIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.metadata[DeferredIntentConfirmationTypeKey])
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(completeAction.metadata[CheckoutSessionResponseKey]?.status)
            .isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with requires_action payment intent returns Launch action`() = runScenario {
        networkRule.checkoutConfirm(
                intentResponseFactory = { response ->
                    response.testBodyFromFile("checkout-payment-intent.json") { json ->
                        json.put("status", "requires_action")
                    }
                },
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

        val launchAction = result as ConfirmationDefinition.Action.Launch
        val nextAction = launchAction.launcherArguments as IntentConfirmationDefinition.Args.NextAction
        assertThat(nextAction.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `intercept fails when checkout session confirm fails`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("expected_payment_method_type", "card"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    @Test
    fun `intercept with saved payment method fails when updating the billing tax region fails`() = runScenario(
        checkoutSessionResponse = AUTOMATIC_TAX_RESPONSE,
    ) {
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid billing address"}}""")
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()
        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.cause.message).contains("Invalid billing address")
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    @Test
    fun `intercept with saved payment method fails when updating the billing tax region changes the total`() =
        runScenario(
            checkoutSessionResponse = AUTOMATIC_TAX_RESPONSE,
        ) {
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONArray("items").getJSONObject(0)
                        .getJSONObject("one_time_price").getJSONArray("items").getJSONObject(0)
                        .getJSONObject("amount_details")
                        .put("total", 5399)
                }
            }

            val result = interceptSavedPm()

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()
            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isInstanceOf<LocalStripeException>()
            assertThat(failAction.cause.message)
                .isEqualTo(applicationContext.getString(R.string.stripe_something_went_wrong))
            val error = failAction.cause as LocalStripeException
            assertThat(error.analyticsValue()).isEqualTo("checkoutSessionTotalChanged")
            assertThat(error.stripeError?.code).isEqualTo("checkout_session_total_changed")
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
        }

    @Test
    fun `intercept with saved payment method confirms when billing tax update keeps total unchanged`() =
        runScenario(
            checkoutSessionResponse = AUTOMATIC_TAX_RESPONSE,
        ) {
            networkRule.checkoutUpdate(
                bodyPart("tax_region[country]", "US"),
                bodyPart("tax_region[line1]", "1234 Main Street"),
                bodyPart("tax_region[postal_code]", "94111"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }
            networkRule.checkoutConfirm { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            val result = interceptSavedPm()

            assertThat(result)
                .isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
        }

    @Test
    fun `intercept with new payment method skips billing tax region update`() = runScenario(
        checkoutSessionResponse = AUTOMATIC_TAX_RESPONSE,
    ) {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()
    }

    @Test
    fun `intercept fails when confirm response has no intent`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-init.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.cause).isInstanceOf<IllegalStateException>()
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    @Test
    fun `intercept with succeeded setup intent returns Complete action`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<SetupIntent>()
        assertThat((completeAction.intent as SetupIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with both intents prefers paymentIntent`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                // Synthetic boundary case: recorded confirmations return only one intent type.
                json.getJSONObject("payment_response").put(
                    "setup_intent",
                    JSONObject()
                        .put("id", "seti_123")
                        .put("client_secret", "seti_123_secret_example"),
                )
            }
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
    }

    @Test
    fun `intercept with requires_action setup intent returns Launch action`() = runScenario {
        networkRule.checkoutConfirm(
                intentResponseFactory = { response ->
                    response.testBodyFromFile("checkout-setup-intent.json") { json ->
                        json.put("status", "requires_action")
                    }
                },
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm-setup.json")
            }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

        val launchAction = result as ConfirmationDefinition.Action.Launch
        assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
        assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(launchAction.receivesResultInProcess).isFalse()
    }

    @Test
    fun `intercept with saved payment method and succeeded payment intent returns Complete action`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.put("status", "complete")
            }
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
        assertThat((completeAction.intent as PaymentIntent).status).isEqualTo(StripeIntent.Status.Succeeded)
        assertThat(completeAction.metadata[DeferredIntentConfirmationTypeKey])
            .isEqualTo(DeferredIntentConfirmationType.Server)
        assertThat(completeAction.metadata[CheckoutSessionResponseKey]?.status)
            .isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(completeAction.completedFullPaymentFlow).isTrue()
    }

    @Test
    fun `intercept with saved payment method and requires_action payment intent returns Launch action`() =
        runScenario {
            networkRule.checkoutConfirm(
                intentResponseFactory = { response ->
                    response.testBodyFromFile("checkout-payment-intent.json") { json ->
                        json.put("status", "requires_action")
                    }
                },
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            val result = interceptSavedPm()

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Launch<IntentConfirmationDefinition.Args>>()

            val launchAction = result as ConfirmationDefinition.Action.Launch
            assertThat(launchAction.launcherArguments).isInstanceOf<IntentConfirmationDefinition.Args.NextAction>()
            assertThat(launchAction.launcherArguments.deferredIntentConfirmationType)
                .isEqualTo(DeferredIntentConfirmationType.Server)
            assertThat(launchAction.receivesResultInProcess).isFalse()
        }

    @Test
    fun `intercept with saved payment method fails when checkout session confirm fails`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("expected_payment_method_type", "card"),
        ) { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    @Test
    fun `intercept with new payment method passes shouldSave true when save is enabled and checkbox checked`() =
        runScenario(
            customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
        ) {
            networkRule.checkoutConfirm(
                bodyPart("collected_information[save_payment_method]", "true"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm(shouldSave = true)
        }

    @Test
    fun `intercept with new payment method passes shouldSave false when save is enabled and checkbox unchecked`() =
        runScenario(
            customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
        ) {
            networkRule.checkoutConfirm(
                bodyPart("collected_information[save_payment_method]", "false"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptNewPm(shouldSave = false)
        }

    @Test
    fun `intercept with new payment method omits savePaymentMethod when save is disabled`() = runScenario(
        customerMetadata = SAVE_DISABLED_CUSTOMER_METADATA,
    ) {
        networkRule.checkoutConfirm(
            not(hasBodyPart("collected_information[save_payment_method]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept with new payment method omits savePaymentMethod for guest`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("collected_information[save_payment_method]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept with new payment method passes expectedAmount from checkout session`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 5099L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(intent = PaymentIntentFactory.create(amount = 1000L))
    }

    @Test
    fun `intercept with saved payment method passes expectedAmount from checkout session`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 5099L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(intent = PaymentIntentFactory.create(amount = 1000L))
    }

    @Test
    fun `intercept passes expectedAmount when payment intent amount is null`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 5099L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(intent = PaymentIntentFactory.create().copy(amount = null))
    }

    @Test
    fun `intercept with new payment method passes zero expectedAmount from checkout session`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 0L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "0"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(intent = PaymentIntentFactory.create(amount = 5099L))
    }

    @Test
    fun `intercept with saved payment method passes zero expectedAmount from checkout session`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 0L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "0"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(intent = PaymentIntentFactory.create(amount = 5099L))
    }

    @Test
    fun `intercept passes expectedAmount from checkout session for setup intent`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 5099L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        interceptNewPm(intent = SetupIntentFactory.create())
    }

    @Test
    fun `intercept passes zero expectedAmount and omits saving for setup intent`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 0L),
        customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "0"),
            not(hasBodyPart("collected_information[save_payment_method]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        interceptNewPm(intent = SetupIntentFactory.create(), shouldSave = true)
    }

    @Test
    fun `intercept with saved payment method passes zero expectedAmount for setup intent`() = runScenario(
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 0L),
    ) {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "0"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        interceptSavedPm(intent = SetupIntentFactory.create())
    }

    @Test
    fun `intercept with saved payment method passes null for savePaymentMethod`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("collected_information[save_payment_method]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm()
    }

    @Test
    fun `intercept with saved payment method passes shipping information`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Jenny Rosen"),
            bodyPart("shipping[address][line1]", "510 Townsend St"),
            bodyPart("shipping[address][postal_code]", "94103"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingInformation = SHIPPING_INFORMATION)
    }

    @Test
    fun `intercept with saved payment method omits shipping information without a name`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingInformation = SHIPPING_INFORMATION_WITHOUT_NAME)
    }

    @Test
    fun `intercept with saved payment method omits shipping information with an empty name`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingInformation = EMPTY_NAME_SHIPPING_INFORMATION)
    }

    @Test
    fun `intercept with new payment method passes controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "Controller City"),
            bodyPart("shipping[address][state]", "NY"),
            bodyPart("shipping[address][postal_code]", "10001"),
            bodyPart("shipping[address][country]", "CA"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(shippingValues = CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with new payment method omits controller shipping with an empty name`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(shippingValues = EMPTY_NAME_CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with new payment method omits empty controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(shippingValues = EMPTY_CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with saved payment method falls back to controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "Controller City"),
            bodyPart("shipping[address][state]", "NY"),
            bodyPart("shipping[address][postal_code]", "10001"),
            bodyPart("shipping[address][country]", "CA"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingValues = CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with saved payment method omits controller shipping with an empty name`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingValues = EMPTY_NAME_CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with saved payment method omits empty controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            doesNotContainBodyPartsWithPrefix("shipping["),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingValues = EMPTY_CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with saved payment method falls back from addressless option shipping`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "Controller City"),
            bodyPart("shipping[address][state]", "NY"),
            bodyPart("shipping[address][postal_code]", "10001"),
            bodyPart("shipping[address][country]", "CA"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(
            shippingInformation = ADDRESSLESS_SHIPPING_INFORMATION,
            shippingValues = CONTROLLER_SHIPPING,
        )
    }

    @Test
    fun `intercept with saved payment method falls back when shipping information name is null`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "Controller City"),
            bodyPart("shipping[address][state]", "NY"),
            bodyPart("shipping[address][postal_code]", "10001"),
            bodyPart("shipping[address][country]", "CA"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(
            shippingInformation = SHIPPING_INFORMATION_WITHOUT_NAME,
            shippingValues = CONTROLLER_SHIPPING,
        )
    }

    @Test
    fun `intercept with saved payment method falls back when shipping information name is empty`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "Controller City"),
            bodyPart("shipping[address][state]", "NY"),
            bodyPart("shipping[address][postal_code]", "10001"),
            bodyPart("shipping[address][country]", "CA"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(
            shippingInformation = EMPTY_NAME_SHIPPING_INFORMATION,
            shippingValues = CONTROLLER_SHIPPING,
        )
    }

    @Test
    fun `intercept with saved payment method prefers option shipping over controller shipping`() =
        runScenario {
            networkRule.checkoutConfirm(
                bodyPart("shipping[name]", "Jenny Rosen"),
                bodyPart("shipping[address][line1]", "510 Townsend St"),
                bodyPart("shipping[address][line2]", "Floor 3"),
                bodyPart("shipping[address][city]", "San Francisco"),
                bodyPart("shipping[address][state]", "CA"),
                bodyPart("shipping[address][postal_code]", "94103"),
                bodyPart("shipping[address][country]", "US"),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            interceptSavedPm(
                shippingInformation = SHIPPING_INFORMATION,
                shippingValues = CONTROLLER_SHIPPING,
            )
        }

    @Test
    fun `intercept with new payment method omits shipping when none is provided`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("shipping[name]")),
            not(hasBodyPart("shipping[address][line1]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept with saved payment method omits shipping when none is provided`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("shipping[name]")),
            not(hasBodyPart("shipping[address][line1]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm()
    }

    @Test
    fun `intercept propagates intent retrieval failure`() = runScenario {
        networkRule.checkoutConfirm(
            intentResponseFactory = { response ->
                response.setResponseCode(400)
                response.setBody("""{"error":{"message":"Intent retrieval failed"}}""")
            },
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Fail

        assertThat(result.cause.message).isEqualTo("Intent retrieval failed")
        assertThat(result.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    @Test
    fun `intercept propagates CheckoutClient payment error`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.getJSONObject("payment_response").put("status", "requires_payment_method").put(
                    "last_payment_error",
                    JSONObject().put("message", "Card declined").put("code", "card_declined"),
                )
            }
        }

        val result = interceptNewPm() as ConfirmationDefinition.Action.Fail

        assertThat(result.cause.message).isEqualTo("Card declined")
        assertThat(result.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Transaction())
    }

    private fun completeChallenge(
        option: PaymentMethodConfirmationOption,
        token: String?,
    ): PaymentMethodConfirmationOption {
        val errorReporter = FakeErrorReporter()
        val warmer = FakePassiveChallengeWarmer()
        val definition = PassiveChallengeConfirmationDefinition(
            errorReporter = errorReporter,
            passiveChallengeWarmer = warmer,
            productUsage = emptySet(),
            isEligibleForConfirmationChallenge = FakeIsEligibleForConfirmationChallenge(),
        )
        val result = definition.toResult(
            confirmationOption = option,
            confirmationArgs = CONFIRMATION_PARAMETERS,
            launcherArgs = PassiveChallengeActivityContract.Args(
                passiveCaptchaParams = requireNotNull(
                    CONFIRMATION_PARAMETERS.paymentMethodMetadata.passiveCaptchaParams
                ),
                apiConfiguration = DEFAULT_API_CONFIG,
                productUsage = emptySet(),
            ),
            result = token?.let { PassiveChallengeActivityResult.Success(it) }
                ?: PassiveChallengeActivityResult.Failed(IllegalStateException("Challenge failed")),
        ) as ConfirmationDefinition.Result.NextStep
        errorReporter.ensureAllEventsConsumed()
        warmer.ensureAllEventsConsumed()
        return result.confirmationOption as PaymentMethodConfirmationOption
    }

    private fun captchaMatcher(token: String?): RequestMatcher = if (token == null) {
        not(hasBodyPart("passive_captcha_token"))
    } else {
        bodyPart("passive_captcha_token", token)
    }

    private fun commonRequestMatchers(): Array<RequestMatcher> = arrayOf(
        bodyPart("client_secret", "cs_test_abc123_secret_test"),
        bodyPart("locale", CheckoutStripeRepositoryFactory.clientParams().locale),
        not(hasBodyPart("timezone")),
        bodyPart("mobile_session_id", AnalyticsRequestFactory.sessionId.toString()),
        bodyPart("mobile_app_id", "com.stripe.android.paymentsheet.test"),
    )

    private fun runScenario(
        customerMetadata: CustomerMetadata? = null,
        collectedEmail: String? = null,
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        block: suspend Scenario.() -> Unit,
    ) {
        val checkoutSessionRepository = CheckoutSessionRepository(
            stripeRepository = CheckoutStripeRepositoryFactory.create(),
            elementsSessionClientParams = CheckoutStripeRepositoryFactory.clientParams(),
            fraudDetectionDataRepository = FakeFraudDetectionDataRepository.create(),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            apiRequestOptionsProvider = {
                ApiRequest.Options(
                    apiKey = DEFAULT_API_CONFIG.publishableKey,
                    stripeAccount = DEFAULT_API_CONFIG.stripeAccountId,
                )
            },
        )

        val interceptor = CheckoutSessionConfirmationInterceptor(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                collectedEmail = collectedEmail,
                id = checkoutSessionResponse.id,
                instancesKey = "test_key",
                checkoutSessionResponse = checkoutSessionResponse,
            ),
            customerMetadata = customerMetadata,
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = "test_session_id",
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
                checkoutSessionId = null,
            ),
            context = applicationContext,
            checkoutSessionRepository = checkoutSessionRepository,
            checkoutSessionTaxRegionUpdater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository),
        )

        runTest {
            val scenario = Scenario(
                interceptor = interceptor,
            )

            scenario.block()
        }
    }

    private fun createPaymentMethodMatchers(
        saved: Boolean,
        type: PaymentMethod.Type,
        paymentMethod: PaymentMethod,
        createParams: PaymentMethodCreateParams,
    ): List<RequestMatcher> = if (saved) {
        listOf(bodyPart("payment_method", paymentMethod.id))
    } else {
        listOf(
            when (type) {
                PaymentMethod.Type.Card -> bodyPart("payment_method_data[card][number]", "4242424242424242")
                PaymentMethod.Type.Link -> bodyPart(
                    "payment_method_data[link][payment_details_id]",
                    "payment_details_123",
                )
                PaymentMethod.Type.SepaDebit -> bodyPart("payment_method_data[sepa_debit][iban]", "my_iban")
                else -> error("Unexpected test type: $type")
            },
            bodyPart(
                "payment_method_data[payment_user_agent]",
                setOf("stripe-android/${StripeSdkVersion.VERSION_NAME}", "checkout_elements")
                    .plus(createParams.attribution)
                    .joinToString(";"),
            ),
            bodyPart("payment_method_data[guid]", "test_guid"),
            bodyPart("payment_method_data[muid]", "test_muid"),
            bodyPart("payment_method_data[sid]", "test_sid"),
        )
    }

    private data class Scenario(
        val interceptor: CheckoutSessionConfirmationInterceptor,
    ) {
        suspend fun interceptNewPm(
            shouldSave: Boolean = false,
            intent: StripeIntent = PaymentIntentFactory.create(),
            shippingValues: ConfirmPaymentIntentParams.Shipping? = null,
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = interceptor.intercept(
            intent = intent,
            confirmationOption = NEW_PM_OPTION.copy(shouldSave = shouldSave),
            shippingValues = shippingValues,
        )

        suspend fun interceptSavedPm(
            intent: StripeIntent = PaymentIntentFactory.create(),
            shippingInformation: ShippingInformation? = null,
            shippingValues: ConfirmPaymentIntentParams.Shipping? = null,
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            interceptor.intercept(
                intent = intent,
                confirmationOption = SAVED_PM_OPTION.copy(
                    shippingInformation = shippingInformation,
                ),
                shippingValues = shippingValues,
            )
    }

    private companion object {
        val AUTOMATIC_TAX_RESPONSE = CheckoutSessionResponseFactory.create(
            amount = 5099L,
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        val NEW_PM_OPTION = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )

        val SAVED_PM_OPTION = PaymentMethodConfirmationOption.Saved(
            shippingInformation = null,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )

        val SHIPPING_INFORMATION = ShippingInformation(
            name = "Jenny Rosen",
            phone = "1-800-555-1234",
            address = Address(
                line1 = "510 Townsend St",
                line2 = "Floor 3",
                city = "San Francisco",
                state = "CA",
                postalCode = "94103",
                country = "US",
            ),
        )

        val CONTROLLER_SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = Address(
                line1 = "123 Controller Street",
                line2 = "Unit 4",
                city = "Controller City",
                state = "NY",
                postalCode = "10001",
                country = "CA",
            ),
            name = "Controller Shipping",
            phone = "1-800-555-0000",
        )

        val EMPTY_CONTROLLER_SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = Address(),
            name = "Controller Shipping",
        )

        val EMPTY_NAME_CONTROLLER_SHIPPING = ConfirmPaymentIntentParams.Shipping(
            address = CONTROLLER_SHIPPING.getAddress(),
            name = "",
        )

        val SHIPPING_INFORMATION_WITHOUT_NAME = ShippingInformation(
            address = SHIPPING_INFORMATION.address,
            name = null,
        )

        val EMPTY_NAME_SHIPPING_INFORMATION = ShippingInformation(
            address = SHIPPING_INFORMATION.address,
            name = "",
        )

        val ADDRESSLESS_SHIPPING_INFORMATION = ShippingInformation(
            address = Address(),
            name = "Option Shipping",
        )

        val SAVE_ENABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val SAVE_DISABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
        )
    }
}
