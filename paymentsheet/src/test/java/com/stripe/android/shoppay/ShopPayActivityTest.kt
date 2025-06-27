package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.shoppay.bridge.ECEBillingDetails
import com.stripe.android.shoppay.bridge.ECEFullAddress
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(SharedPaymentTokenSessionPreview::class, ShopPayPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class ShopPayActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dispatcher = StandardTestDispatcher()
    private lateinit var confirmationState: MutableStateFlow<ShopPayConfirmationState>

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Before
    fun setUp() {
        confirmationState = MutableStateFlow(ShopPayConfirmationState.Pending)
        setupPaymentElementCallbacks()
    }

    private fun setupPaymentElementCallbacks() {
        PaymentElementCallbackReferences["paymentElementCallbackIdentifier"] = PaymentElementCallbacks.Builder()
            .shopPayHandlers(
                shopPayHandlers = ShopPayHandlers(
                    shippingMethodUpdateHandler = { null },
                    shippingContactHandler = { null }
                )
            )
            .preparePaymentMethodHandler { _, _ -> }
            .build()
    }

    @Test
    fun `finishes with failed result when ViewModel factory fails`() {
        val intent = Intent(context, ShopPayActivity::class.java)
        val scenario = ActivityScenario.launchActivityForResult<ShopPayActivity>(intent)

        assertThat(scenario.result.resultCode).isEqualTo(ShopPayActivity.RESULT_COMPLETE)

        val result = getResultFromActivityScenario(scenario)

        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        val failedResult = result as ShopPayActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Failed to create ShopPayViewModel")
        assertNoUnverifiedIntents()
    }

    @Test
    fun `createIntent creates correct intent with args`() {
        val intent = ShopPayActivity.createIntent(context, ShopPayTestFactory.SHOP_PAY_ARGS)

        assertThat(intent.component?.className).isEqualTo(ShopPayActivity::class.java.name)
        val intentArgs = getArgsFromIntent(intent)

        assertThat(intentArgs?.shopPayConfiguration).isEqualTo(SHOP_PAY_CONFIGURATION)
        assertThat(intentArgs?.publishableKey).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.publishableKey)
    }

    @Test
    fun `getArgs returns correct args from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(ShopPayActivity.EXTRA_ARGS, ShopPayTestFactory.SHOP_PAY_ARGS)
        }

        val retrievedArgs = ShopPayActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS)
        assertThat(retrievedArgs?.shopPayConfiguration).isEqualTo(SHOP_PAY_CONFIGURATION)
        assertThat(retrievedArgs?.publishableKey).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.publishableKey)
    }

    @Test
    fun `getArgs returns null when no args in SavedStateHandle`() {
        val retrievedArgs = ShopPayActivity.getArgs(SavedStateHandle())
        assertThat(retrievedArgs).isNull()
    }

    @Test
    fun `finishes with Completed result when payment succeeds`() = runTest(dispatcher) {
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val stripeRepository = createTestStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        confirmationState.value = ShopPayConfirmationState.Success(
            externalSourceId = "test_id",
            billingDetails = createTestBillingDetails()
        )

        advanceUntilIdle()

        val result = verifyActivityFinishedAndGetResult(activity)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Completed>()
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with Failed result when payment fails`() = runTest(dispatcher) {
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val exception = RuntimeException("Payment failed")
        val stripeRepository = createTestStripeRepository(Result.failure(exception))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        confirmationState.value = ShopPayConfirmationState.Success(
            externalSourceId = "test_id",
            billingDetails = createTestBillingDetails()
        )

        advanceUntilIdle()

        val result = verifyActivityFinishedAndGetResult(activity)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with Failed result when confirmation state fails`() = runTest(dispatcher) {
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val stripeRepository = createTestStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        val exception = RuntimeException("Confirmation failed")
        confirmationState.value = ShopPayConfirmationState.Failure(exception)

        advanceUntilIdle()

        val result = verifyActivityFinishedAndGetResult(activity)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
        assertNoUnverifiedIntents()
    }

    private fun getResultFromActivityScenario(scenario: ActivityScenario<ShopPayActivity>): ShopPayActivityResult? {
        return scenario.result.resultData.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivityContract.EXTRA_RESULT, ShopPayActivityResult::class.java)
        }
    }

    private fun getArgsFromIntent(intent: Intent): ShopPayArgs? {
        return intent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivity.EXTRA_ARGS, ShopPayArgs::class.java)
        }
    }

    private fun createTestBridgeHandler(
        confirmationState: MutableStateFlow<ShopPayConfirmationState>
    ): ShopPayBridgeHandler {
        return object : ShopPayBridgeHandler {
            override val confirmationState = confirmationState
            override fun consoleLog(level: String, message: String, origin: String, url: String) = Unit
            override fun getStripePublishableKey(): String = "pk_test_fake_key"
            override fun handleECEClick(message: String): String = ""
            override fun getShopPayInitParams(): String = ""
            override fun calculateShipping(message: String) = null
            override fun calculateShippingRateChange(message: String) = null
            override fun confirmPayment(message: String): String = ""
            override fun ready(message: String) = Unit
        }
    }

    private fun createTestStripeRepository(result: Result<PaymentMethod>): StripeRepository {
        return object : AbsFakeStripeRepository() {
            override suspend fun createPaymentMethod(
                paymentMethodCreateParams: com.stripe.android.model.PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): Result<PaymentMethod> = result
        }
    }

    private fun createTestBillingDetails(): ECEBillingDetails {
        return ECEBillingDetails(
            name = "Test User",
            email = "test@example.com",
            phone = "+1234567890",
            address = ECEFullAddress(
                line1 = "123 Main St",
                line2 = null,
                city = "Test City",
                state = "TS",
                postalCode = "12345",
                country = "US"
            )
        )
    }

    private fun getResultFromIntent(intent: Intent?): ShopPayActivityResult? {
        return intent?.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivityContract.EXTRA_RESULT, ShopPayActivityResult::class.java)
        }
    }

    private fun verifyActivityFinishedAndGetResult(activity: ShopPayActivity): ShopPayActivityResult? {
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(63636)
        return getResultFromIntent(shadowActivity.resultIntent)
    }

    private fun setupActivityController(
        bridgeHandler: ShopPayBridgeHandler,
        stripeRepository: StripeRepository
    ): ShopPayActivity {
        val intent = ShopPayActivity.createIntent(context, ShopPayTestFactory.SHOP_PAY_ARGS)
        val activityController = Robolectric.buildActivity(ShopPayActivity::class.java, intent)

        activityController.get().viewModelFactory = createTestViewModelFactory(bridgeHandler, stripeRepository)
        return activityController.setup().get()
    }

    private fun createTestViewModelFactory(
        bridgeHandler: ShopPayBridgeHandler,
        stripeRepository: StripeRepository
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShopPayViewModel(
                    bridgeHandler = bridgeHandler,
                    stripeApiRepository = stripeRepository,
                    requestOptions = ApiRequest.Options("pk_test"),
                    paymentMethodHandler = { _, _ -> },
                    workContext = dispatcher,
                ) as T
            }
        }
    }
}
