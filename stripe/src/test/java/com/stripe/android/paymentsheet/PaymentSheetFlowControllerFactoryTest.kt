package com.stripe.android.paymentsheet

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSessionPrefs
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PaymentSheetFlowControllerFactoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()

    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val factory: PaymentSheetFlowControllerFactory by lazy {
        createFactory(PAYMENT_INTENT)
    }

    private lateinit var activityScenario: ActivityScenario<*>
    private lateinit var activity: ComponentActivity

    @BeforeTest
    fun before() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        Dispatchers.setMain(testDispatcher)

        activityScenarioFactory
            .createAddPaymentMethodActivity()
            .use { activityScenario ->
                this.activityScenario = activityScenario
                activityScenario.onActivity { activity ->
                    this.activity = activity
                }
            }
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `create() with customer config should create object with expected properties`() {
        var result: PaymentSheet.FlowController.Result? = null
        factory.create(
            "client_secret",
            PaymentSheetFixtures.CONFIG_CUSTOMER
        ) {
            result = it
        }

        val successResult =
            result as PaymentSheet.FlowController.Result.Success
        val flowController =
            successResult.flowController as DefaultPaymentSheetFlowController

        assertThat(flowController.paymentMethodTypes)
            .containsExactly(PaymentMethod.Type.Card)
        assertThat(flowController.paymentMethods)
            .hasSize(1)
    }

    @Test
    fun `create() when PaymentIntent has invalid status should return failure result`() {
        var result: PaymentSheet.FlowController.Result? = null
        createFactory(
            paymentIntent = PAYMENT_INTENT.copy(
                status = StripeIntent.Status.Succeeded
            )
        ).create(
            "client_secret",
            PaymentSheetFixtures.CONFIG_CUSTOMER
        ) {
            result = it
        }

        assertThat(result)
            .isInstanceOf(PaymentSheet.FlowController.Result.Failure::class.java)
    }

    @Test
    fun `create() when PaymentIntent has invalid confirmationMethod should return failure result`() {
        var result: PaymentSheet.FlowController.Result? = null
        createFactory(
            paymentIntent = PAYMENT_INTENT.copy(
                confirmationMethod = PaymentIntent.ConfirmationMethod.Manual
            )
        ).create(
            "client_secret",
            PaymentSheetFixtures.CONFIG_CUSTOMER
        ) {
            result = it
        }

        assertThat(result)
            .isInstanceOf(PaymentSheet.FlowController.Result.Failure::class.java)
    }

    @Test
    fun `result should not be dispatched if activity is destroyed first`() {
        val factory = createFactory(
            object : AbsFakeStripeRepository() {
                override suspend fun retrievePaymentIntent(
                    clientSecret: String,
                    options: ApiRequest.Options,
                    expandFields: List<String>
                ): PaymentIntent {
                    delay(1000)
                    error("Failure")
                }

                override suspend fun getPaymentMethods(
                    listPaymentMethodsParams: ListPaymentMethodsParams,
                    publishableKey: String,
                    productUsageTokens: Set<String>,
                    requestOptions: ApiRequest.Options
                ): List<PaymentMethod> {
                    delay(1000)
                    error("Failure")
                }
            }
        )

        testDispatcher.advanceTimeBy(500)

        var result: PaymentSheet.FlowController.Result? = null
        val job = factory.create(
            "client_secret",
            PaymentSheetFixtures.CONFIG_CUSTOMER
        ) {
            result = it
        }

        activityScenario.moveToState(Lifecycle.State.DESTROYED)
        job.cancel()

        assertThat(result)
            .isNull()
    }

    private fun createFactory(
        paymentIntent: PaymentIntent = PAYMENT_INTENT
    ) = createFactory(
        FakeStripeRepository(paymentIntent)
    )

    private fun createFactory(
        stripeRepository: StripeRepository = FakeStripeRepository(PAYMENT_INTENT)
    ): PaymentSheetFlowControllerFactory {
        return PaymentSheetFlowControllerFactory(
            activity,
            stripeRepository,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            null,
            FakePaymentSessionPrefs(),
            testDispatcher
        )
    }

    private class FakePaymentSessionPrefs : PaymentSessionPrefs {
        override fun getPaymentMethodId(customerId: String?): String? = "pm_123"

        override fun savePaymentMethodId(customerId: String, paymentMethodId: String?) {
        }
    }

    private class FakeStripeRepository(
        private val paymentIntent: PaymentIntent
    ) : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = paymentIntent

        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            return listOf(
                PaymentMethodFixtures.createPaymentMethod(
                    listPaymentMethodsParams.paymentMethodType
                )
            )
        }
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    }
}
