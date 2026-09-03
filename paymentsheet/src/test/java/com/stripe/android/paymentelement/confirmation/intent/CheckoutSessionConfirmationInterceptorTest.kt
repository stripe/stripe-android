package com.stripe.android.paymentelement.confirmation.intent

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkouttesting.checkoutConfirm
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.Address
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.hasBodyPart
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.EmptyConfirmationLauncherArgs
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.repositories.ElementsSessionClientParams
import com.stripe.android.paymentsheet.repositories.TotalSummaryResponseFactory
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.SetupIntentFactory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
class CheckoutSessionConfirmationInterceptorTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

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
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm.json") { json ->
                json.getJSONObject("payment_intent").put("status", "requires_action")
            }
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
    fun `intercept fails when payment method creation fails`() {
        val error = RuntimeException("Payment method creation failed")

        runScenario(
            createPaymentMethodResult = Result.failure(error),
        ) {
            val result = interceptNewPm()

            assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

            val failAction = result as ConfirmationDefinition.Action.Fail
            assertThat(failAction.cause).isEqualTo(error)
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
        }
    }

    @Test
    fun `intercept fails when checkout session confirm fails`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept with saved payment method fails when updating the billing tax region changes the total`() =
        runScenario(
            checkoutSessionResponse = AUTOMATIC_TAX_RESPONSE,
        ) {
            networkRule.checkoutUpdate { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONObject("total_summary").put("total", 5399)
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
            assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
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
            response.testBodyFromFile("checkout-session-confirm-both-intents.json")
        }

        val result = interceptNewPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Complete<IntentConfirmationDefinition.Args>>()

        val completeAction = result as ConfirmationDefinition.Action.Complete
        assertThat(completeAction.intent).isInstanceOf<PaymentIntent>()
    }

    @Test
    fun `intercept with requires_action setup intent returns Launch action`() = runScenario {
        networkRule.checkoutConfirm { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json") { json ->
                json.getJSONObject("setup_intent").put("status", "requires_action")
            }
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
            networkRule.checkoutConfirm { response ->
                response.testBodyFromFile("checkout-session-confirm.json") { json ->
                    json.getJSONObject("payment_intent").put("status", "requires_action")
                }
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
        networkRule.checkoutConfirm { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Checkout session confirmation failed"}}""")
        }

        val result = interceptSavedPm()

        assertThat(result).isInstanceOf<ConfirmationDefinition.Action.Fail<IntentConfirmationDefinition.Args>>()

        val failAction = result as ConfirmationDefinition.Action.Fail
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Payment)
    }

    @Test
    fun `intercept with new payment method passes shouldSave true when save is enabled and checkbox checked`() =
        runScenario(
            customerMetadata = SAVE_ENABLED_CUSTOMER_METADATA,
        ) {
            networkRule.checkoutConfirm(
                bodyPart("save_payment_method", "true"),
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
                bodyPart("save_payment_method", "false"),
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
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept with new payment method omits savePaymentMethod for guest`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("save_payment_method")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm()
    }

    @Test
    fun `intercept passes expectedAmount from payment intent`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("expected_amount", "5099"),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(intent = PaymentIntentFactory.create(amount = 5099L))
    }

    @Test
    fun `intercept omits expectedAmount for setup intent`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("expected_amount")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm-setup.json")
        }

        interceptNewPm(intent = SetupIntentFactory.create())
    }

    @Test
    fun `intercept with saved payment method passes null for savePaymentMethod`() = runScenario {
        networkRule.checkoutConfirm(
            not(hasBodyPart("save_payment_method")),
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
    fun `intercept with new payment method passes controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "San Francisco"),
            bodyPart("shipping[address][state]", "CA"),
            bodyPart("shipping[address][postal_code]", "94111"),
            bodyPart("shipping[address][country]", "US"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptNewPm(shippingValues = CONTROLLER_SHIPPING)
    }

    @Test
    fun `intercept with saved payment method falls back to controller shipping`() = runScenario {
        networkRule.checkoutConfirm(
            bodyPart("shipping[name]", "Controller Shipping"),
            bodyPart("shipping[address][line1]", "123 Controller Street"),
            bodyPart("shipping[address][line2]", "Unit 4"),
            bodyPart("shipping[address][city]", "San Francisco"),
            bodyPart("shipping[address][state]", "CA"),
            bodyPart("shipping[address][postal_code]", "94111"),
            bodyPart("shipping[address][country]", "US"),
            not(hasBodyPart("shipping[phone]")),
        ) { response ->
            response.testBodyFromFile("checkout-session-confirm.json")
        }

        interceptSavedPm(shippingValues = CONTROLLER_SHIPPING)
    }

    @Test
    fun `Google Pay result shipping takes precedence over controller shipping at Checkout confirmation`() =
        runScenario {
            networkRule.checkoutConfirm(
                bodyPart("shipping[name]", "Google Pay Shipping"),
                bodyPart("shipping[address][line1]", "510 Townsend St"),
                bodyPart("shipping[address][line2]", "Floor 3"),
                bodyPart("shipping[address][city]", "San Francisco"),
                bodyPart("shipping[address][state]", "CA"),
                bodyPart("shipping[address][postal_code]", "94103"),
                bodyPart("shipping[address][country]", "US"),
                not(bodyPart("shipping[name]", "Controller Shipping")),
                not(bodyPart("shipping[address][line1]", "123 Controller Street")),
                not(bodyPart("shipping[address][line2]", "Unit 4")),
                not(bodyPart("shipping[address][city]", "Controller City")),
                not(bodyPart("shipping[address][state]", "NY")),
                not(bodyPart("shipping[address][postal_code]", "10001")),
                not(bodyPart("shipping[address][country]", "CA")),
                not(hasBodyPart("shipping[phone]")),
            ) { response ->
                response.testBodyFromFile("checkout-session-confirm.json")
            }

            val confirmationArgs = ConfirmationHandler.Args(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = PaymentIntentFactory.create(),
                    integrationMetadata = integrationMetadata,
                    shippingDetails = CONTROLLER_SHIPPING_DETAILS,
                ),
                statusBarColor = null,
            )
            val googlePayResult = GooglePayConfirmationDefinition(
                instanceId = "test",
                context = applicationContext,
                googlePayPaymentMethodLauncherFactory = mock(),
                userFacingLogger = null,
            ).toResult(
                confirmationOption = GOOGLE_PAY_CONFIRMATION_OPTION,
                confirmationArgs = confirmationArgs,
                launcherArgs = EmptyConfirmationLauncherArgs,
                result = com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher.Result.Completed(
                    paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    shippingInformation = GOOGLE_PAY_SHIPPING_INFORMATION,
                ),
            )

            assertThat(googlePayResult).isInstanceOf<ConfirmationDefinition.Result.NextStep>()
            val nextStep = googlePayResult as ConfirmationDefinition.Result.NextStep
            assertThat(nextStep.arguments.paymentMethodMetadata.shippingDetails)
                .isEqualTo(CONTROLLER_SHIPPING_DETAILS)

            IntentConfirmationDefinition(
                intentConfirmationInterceptorFactory = object : IntentConfirmationInterceptor.Factory {
                    override suspend fun create(
                        integrationMetadata: IntegrationMetadata,
                        customerMetadata: CustomerMetadata?,
                        clientAttributionMetadata: ClientAttributionMetadata,
                    ): IntentConfirmationInterceptor = interceptor
                },
                paymentLauncherFactory = { _, _ -> error("No payment launcher is needed") },
            ).action(
                confirmationOption = nextStep.confirmationOption as PaymentMethodConfirmationOption.Saved,
                confirmationArgs = nextStep.arguments,
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

    private fun runScenario(
        createPaymentMethodResult: Result<PaymentMethod> = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        customerMetadata: CustomerMetadata? = null,
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        block: suspend Scenario.() -> Unit,
    ) {
        val stripeRepository = FakeCreatePaymentMethodRepository(
            createPaymentMethodResult = createPaymentMethodResult,
        )

        val checkoutSessionRepository = CheckoutSessionRepository(
            clientParams = ElementsSessionClientParams(
                mobileAppId = "com.stripe.android.test",
                mobileSessionIdProvider = { "test_session" },
            ),
            stripeNetworkClient = DefaultStripeNetworkClient(),
            analyticsRequestExecutor = FakeAnalyticsRequestExecutor(),
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                publishableKey = "pk_test_123",
            ),
            publishableKeyProvider = { "pk_test_123" },
            stripeAccountIdProvider = { null },
        )

        val integrationMetadata = IntegrationMetadata.CheckoutSession(
            id = checkoutSessionResponse.id,
            instancesKey = "test_key",
            checkoutSessionResponse = checkoutSessionResponse,
        )
        val interceptor = CheckoutSessionConfirmationInterceptor(
            integrationMetadata = integrationMetadata,
            customerMetadata = customerMetadata,
            clientAttributionMetadata = ClientAttributionMetadata(
                elementsSessionConfigId = "test_session_id",
                paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
                paymentMethodSelectionFlow = PaymentMethodSelectionFlow.MerchantSpecified,
                checkoutSessionId = null,
            ),
            context = applicationContext,
            stripeRepository = stripeRepository,
            checkoutSessionRepository = checkoutSessionRepository,
            checkoutSessionTaxRegionUpdater = CheckoutSessionTaxRegionUpdater(checkoutSessionRepository),
            requestOptions = ApiRequest.Options(apiKey = "pk_test_123"),
        )

        runTest {
            val scenario = Scenario(
                interceptor = interceptor,
                integrationMetadata = integrationMetadata,
            )

            scenario.block()
        }
    }

    private data class Scenario(
        val interceptor: CheckoutSessionConfirmationInterceptor,
        val integrationMetadata: IntegrationMetadata.CheckoutSession,
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
            originatedFromWallet: Boolean = false,
        ): ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> =
            interceptor.intercept(
                intent = intent,
                confirmationOption = SAVED_PM_OPTION.copy(
                    shippingInformation = shippingInformation,
                    originatedFromWallet = originatedFromWallet,
                ),
                shippingValues = shippingValues,
            )
    }

    private class FakeCreatePaymentMethodRepository(
        private val createPaymentMethodResult: Result<PaymentMethod> =
            Result.failure(NotImplementedError()),
    ) : AbsFakeStripeRepository() {

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): Result<PaymentMethod> {
            return createPaymentMethodResult
        }
    }

    private companion object {
        val AUTOMATIC_TAX_RESPONSE = CheckoutSessionResponseFactory.create(
            totalSummary = TotalSummaryResponseFactory.create(totalAmountDue = 5099L),
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
                city = "San Francisco",
                state = "CA",
                postalCode = "94111",
                country = "US",
            ),
            name = "Controller Shipping",
            phone = "1-800-555-0000",
        )

        val CONTROLLER_SHIPPING_DETAILS = AddressDetails(
            name = "Controller Shipping",
            address = PaymentSheet.Address(
                line1 = "123 Controller Street",
                line2 = "Unit 4",
                city = "Controller City",
                state = "NY",
                postalCode = "10001",
                country = "CA",
            ),
            phoneNumber = "1-800-555-0000",
        )

        val GOOGLE_PAY_CONFIRMATION_OPTION = GooglePayConfirmationOption(
            config = GooglePayConfirmationOption.Config(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                merchantName = "Test merchant",
                merchantCountryCode = "US",
                merchantCurrencyCode = "USD",
                customAmount = null,
                customLabel = null,
                billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                cardBrandFilter = DefaultCardBrandFilter,
                cardFundingFilter = DefaultCardFundingFilter,
            ),
        )

        val GOOGLE_PAY_SHIPPING_INFORMATION = ShippingInformation(
            name = "Google Pay Shipping",
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

        val SAVE_ENABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Enabled,
        )

        val SAVE_DISABLED_CUSTOMER_METADATA = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA.copy(
            saveConsent = PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
        )
    }
}
