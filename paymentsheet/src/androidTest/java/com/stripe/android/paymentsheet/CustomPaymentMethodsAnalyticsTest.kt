package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.Stripe
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.googlepaylauncher.GooglePayAvailabilityClient
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CustomPaymentMethodResult
import com.stripe.android.paymentelement.CustomPaymentMethodResultHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.utils.IntegrationType
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.assertCompleted
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
@RunWith(AndroidJUnit4::class)
class CustomPaymentMethodsAnalyticsTest {
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 1.seconds, // Analytics requests happen async.
    )
    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = PaymentSheetPage(testRules.compose)

    @Before
    fun setup() {
        Stripe.advancedFraudSignalsEnabled = false
        GooglePayRepository.googlePayAvailabilityClientFactory = createFakeGooglePayAvailabilityClient()
    }

    @After
    fun teardown() {
        Stripe.advancedFraudSignalsEnabled = true
        GooglePayRepository.resetFactory()
    }

    @Test
    fun testSuccessful() = runPaymentSheetTest(
        networkRule = networkRule,
        integrationType = IntegrationType.Compose,
        builder = {
            confirmCustomPaymentMethodCallback { _, _ ->
                CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult(
                    context = applicationContext,
                    customPaymentMethodResult = CustomPaymentMethodResult.completed(),
                )
            }
        },
        resultCallback = ::assertCompleted,
    ) { context ->
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { response ->
            response.testBodyFromFile("elements-sessions-cpms.json")
        }

        validateAnalyticsRequest(
            eventName = "mc_complete_init_default",
            query(urlEncode("mpe_config[custom_payment_methods]"), "cpmt_123")
        )
        validateAnalyticsRequest(eventName = "mc_load_started")
        validateAnalyticsRequest(eventName = "mc_load_succeeded")
        validateAnalyticsRequest(eventName = "mc_complete_sheet_newpm_show")
        validateAnalyticsRequest(eventName = "mc_form_shown")

        context.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = PaymentSheet.Configuration.Builder(merchantDisplayName = "Merchant, Inc.")
                    .customPaymentMethods(
                        listOf(
                            PaymentSheet.CustomPaymentMethod(
                                id = "cpmt_123",
                                subtitle = "Pay now",
                                disableBillingDetailCollection = true,
                            )
                        )
                    )
                    .paymentMethodLayout(PaymentSheet.PaymentMethodLayout.Horizontal)
                    .paymentMethodOrder(listOf("cpmt_123", "card"))
                    .build()
            )
        }

        validateAnalyticsRequest(eventName = "mc_confirm_button_tapped")
        validateAnalyticsRequest(
            eventName = "paymentsheet.custom_payment_method.launch_success",
            query("custom_payment_method_type", "cpmt_123")
        )
        validateAnalyticsRequest(
            eventName = "mc_complete_payment_newpm_success",
            query("selected_lpm", "cpmt_123")
        )

        page.clickPrimaryButton()
    }

    private fun validateAnalyticsRequest(
        eventName: String,
        vararg requestMatchers: RequestMatcher
    ) {
        networkRule.validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("PaymentSheet"),
            requestMatchers = requestMatchers,
        )
    }

    private fun createFakeGooglePayAvailabilityClient(): GooglePayAvailabilityClient.Factory {
        return object : GooglePayAvailabilityClient.Factory {
            override fun create(paymentsClient: PaymentsClient): GooglePayAvailabilityClient {
                return object : GooglePayAvailabilityClient {
                    override suspend fun isReady(request: IsReadyToPayRequest): Boolean {
                        return true
                    }
                }
            }
        }
    }
}
