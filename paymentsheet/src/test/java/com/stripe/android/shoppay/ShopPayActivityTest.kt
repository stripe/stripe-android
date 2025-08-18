package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.shoppay.bridge.ECEBillingDetails
import com.stripe.android.shoppay.bridge.ECEShippingAddressData
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
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

    @get:Rule
    val intentsTestRule = IntentsRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ShopPayActivity>()

    @Before
    fun setup() {
        setupPaymentElementCallbackReferences()
    }

    @Test
    fun `finishes with failed result when ViewModel factory fails`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ShopPayActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<ShopPayActivity>(intent)

        assertThat(scenario.result.resultCode).isEqualTo(ShopPayActivity.RESULT_COMPLETE)

        val resultIntent = scenario.result.resultData
        val result = resultIntent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivityContract.EXTRA_RESULT, ShopPayActivityResult::class.java)
        }

        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        val failedResult = result as ShopPayActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("Failed to create ShopPayViewModel")
        assertNoUnverifiedIntents()
    }

    @Test
    fun `createIntent creates correct intent with args`() {
        val intent = ShopPayActivity.createIntent(context, ShopPayTestFactory.SHOP_PAY_ARGS)

        assertThat(intent.component?.className).isEqualTo(ShopPayActivity::class.java.name)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivity.EXTRA_ARGS, ShopPayArgs::class.java)
        }
        assertThat(intentArgs?.shopPayConfiguration).isEqualTo(SHOP_PAY_CONFIGURATION)
        assertThat(intentArgs?.publishableKey).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.publishableKey)
    }

    @Test
    fun `getArgs returns correct args from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle[ShopPayActivity.EXTRA_ARGS] = ShopPayTestFactory.SHOP_PAY_ARGS

        val retrievedArgs = ShopPayActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS)
        assertThat(retrievedArgs?.shopPayConfiguration).isEqualTo(SHOP_PAY_CONFIGURATION)
        assertThat(retrievedArgs?.publishableKey).isEqualTo(ShopPayTestFactory.SHOP_PAY_ARGS.publishableKey)
    }

    @Test
    fun `getArgs returns null when no args in SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        val retrievedArgs = ShopPayActivity.getArgs(savedStateHandle)

        assertThat(retrievedArgs).isNull()
    }

    @Test
    fun `finishes with Completed result when payment succeeds`() = runTest(dispatcher) {
        val confirmationState = MutableStateFlow<ShopPayConfirmationState>(ShopPayConfirmationState.Pending)
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val stripeRepository = createTestStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        confirmationState.value = ShopPayConfirmationState.Success(
            externalSourceId = "test_id",
            billingDetails = createTestBillingDetails(),
            shippingAddressData = null
        )

        advanceUntilIdle()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(63636)
        val result = getResultFromIntent(shadowActivity.resultIntent)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Completed>()
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with Failed result when payment fails`() = runTest(dispatcher) {
        val confirmationState = MutableStateFlow<ShopPayConfirmationState>(ShopPayConfirmationState.Pending)
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val exception = RuntimeException("Payment failed")
        val stripeRepository = createTestStripeRepository(Result.failure(exception))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        confirmationState.value = ShopPayConfirmationState.Success(
            externalSourceId = "test_id",
            billingDetails = createTestBillingDetails(),
            shippingAddressData = null
        )

        advanceUntilIdle()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(63636)
        val result = getResultFromIntent(shadowActivity.resultIntent)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with Failed result when confirmation state fails`() = runTest(dispatcher) {
        val confirmationState = MutableStateFlow<ShopPayConfirmationState>(ShopPayConfirmationState.Pending)
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val stripeRepository = createTestStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

        val activity = setupActivityController(bridgeHandler, stripeRepository)

        val exception = RuntimeException("Confirmation failed")
        confirmationState.value = ShopPayConfirmationState.Failure(exception)

        advanceUntilIdle()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(63636)
        val result = getResultFromIntent(shadowActivity.resultIntent)
        assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
        assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
        assertNoUnverifiedIntents()
    }

    @Test
    fun `finishes with Completed result when payment succeeds with shipping address data`() = runTest(dispatcher) {
        val (fakeHandler, activity) = testShopPayConfirmation(
            shippingAddressData = ShopPayTestFactory.SHIPPING_ADDRESS_DATA,
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            stripeRepositoryResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        advanceUntilIdle()

        assertShopPayResult(
            activity = activity,
            fakeHandler = fakeHandler,
            expectedResult = ShopPayActivityResult.Completed::class.java
        ) { capturedAddress ->
            assertThat(capturedAddress).isNotNull()
            assertThat(capturedAddress?.name).isEqualTo("Jane Smith")
            assertThat(capturedAddress?.address?.line1).isEqualTo("456 Shipping Ave")
            assertThat(capturedAddress?.address?.line2).isEqualTo("Unit 2B")
            assertThat(capturedAddress?.address?.city).isEqualTo("Shipping City")
            assertThat(capturedAddress?.address?.state).isEqualTo("NY")
            assertThat(capturedAddress?.address?.postalCode).isEqualTo("10002")
            assertThat(capturedAddress?.address?.country).isEqualTo("US")
        }
    }

    @Test
    fun `finishes with Completed result when payment succeeds with null shipping address data`() = runTest(dispatcher) {
        val (fakeHandler, activity) = testShopPayConfirmation(
            shippingAddressData = null,
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            stripeRepositoryResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        advanceUntilIdle()

        assertShopPayResult(
            activity = activity,
            fakeHandler = fakeHandler,
            expectedResult = ShopPayActivityResult.Completed::class.java
        ) { capturedAddress ->
            assertThat(capturedAddress).isNull()
        }
    }

    @Test
    fun `finishes with Completed result when shipping address data has null address`() = runTest(dispatcher) {
        val (fakeHandler, activity) = testShopPayConfirmation(
            shippingAddressData = ShopPayTestFactory.SHIPPING_ADDRESS_DATA_WITH_NULL_ADDRESS,
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            stripeRepositoryResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )

        advanceUntilIdle()

        assertShopPayResult(
            activity = activity,
            fakeHandler = fakeHandler,
            expectedResult = ShopPayActivityResult.Completed::class.java
        ) { capturedAddress ->
            assertThat(capturedAddress).isNotNull()
            assertThat(capturedAddress?.name).isEqualTo("Jane Smith")
            assertThat(capturedAddress?.address).isNull()
        }
    }

    @Test
    fun `finishes with Completed result when payment succeeds with international shipping address`() =
        runTest(dispatcher) {
            val internationalShippingData = ECEShippingAddressData(
                name = "John Smith",
                address = ShopPayTestFactory.INTERNATIONAL_ADDRESS
            )

            val (fakeHandler, activity) = testShopPayConfirmation(
                shippingAddressData = internationalShippingData,
                billingDetails = ShopPayTestFactory.INTERNATIONAL_BILLING_DETAILS,
                stripeRepositoryResult = Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )

            advanceUntilIdle()

            assertShopPayResult(
                activity = activity,
                fakeHandler = fakeHandler,
                expectedResult = ShopPayActivityResult.Completed::class.java
            ) { capturedAddress ->
                assertThat(capturedAddress).isNotNull()
                assertThat(capturedAddress?.name).isEqualTo("John Smith")
                assertThat(capturedAddress?.address?.line1).isEqualTo("10 Downing Street")
                assertThat(capturedAddress?.address?.line2).isNull()
                assertThat(capturedAddress?.address?.city).isEqualTo("London")
                assertThat(capturedAddress?.address?.state).isNull()
                assertThat(capturedAddress?.address?.postalCode).isEqualTo("SW1A 2AA")
                assertThat(capturedAddress?.address?.country).isEqualTo("GB")
            }
        }

    @Test
    fun `finishes with Failed result when payment fails with shipping address data`() = runTest(dispatcher) {
        val exception = RuntimeException("Payment failed with shipping")

        val (fakeHandler, activity) = testShopPayConfirmation(
            shippingAddressData = ShopPayTestFactory.SHIPPING_ADDRESS_DATA,
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            stripeRepositoryResult = Result.failure(exception),
        )

        advanceUntilIdle()

        assertShopPayResult(
            activity = activity,
            fakeHandler = fakeHandler,
            expectedResult = ShopPayActivityResult.Failed::class.java,
            expectedError = exception
        ) { capturedAddress ->
            // When payment method creation fails, the PreparePaymentMethodHandler is not called
            // so shipping address is not captured
            assertThat(capturedAddress).isNull()
        }
    }

    private fun createTestBridgeHandler(
        confirmationState: MutableStateFlow<ShopPayConfirmationState>
    ): ShopPayBridgeHandler {
        return object : ShopPayBridgeHandler {
            override val confirmationState = confirmationState
            override fun setOnECEClickCallback(callback: () -> Unit) = Unit
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
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): Result<PaymentMethod> = result
        }
    }

    private fun createTestViewModelFactory(
        bridgeHandler: ShopPayBridgeHandler,
        stripeRepository: StripeRepository,
        preparePaymentMethodHandler: PreparePaymentMethodHandler
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShopPayViewModel(
                    bridgeHandler = bridgeHandler,
                    stripeApiRepository = stripeRepository,
                    requestOptions = ApiRequest.Options("pk_test"),
                    preparePaymentMethodHandlerProvider = {
                        preparePaymentMethodHandler
                    },
                    workContext = dispatcher,
                    errorReporter = FakeErrorReporter(),
                    eventReporter = FakeEventReporter(),
                ) as T
            }
        }
    }

    private fun createTestBillingDetails(): ECEBillingDetails {
        return ShopPayTestFactory.BILLING_DETAILS
    }

    private fun getResultFromIntent(intent: Intent?): ShopPayActivityResult? {
        return intent?.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivityContract.EXTRA_RESULT, ShopPayActivityResult::class.java)
        }
    }

    private fun setupActivityController(
        bridgeHandler: ShopPayBridgeHandler,
        stripeRepository: StripeRepository,
        preparePaymentMethodHandler: PreparePaymentMethodHandler = PreparePaymentMethodHandler { _, _ -> }
    ): ShopPayActivity {
        val intent = ShopPayActivity.createIntent(context, ShopPayTestFactory.SHOP_PAY_ARGS)

        val activityController = Robolectric.buildActivity(ShopPayActivity::class.java, intent)

        activityController.get().viewModelFactory = createTestViewModelFactory(
            bridgeHandler,
            stripeRepository,
            preparePaymentMethodHandler
        )
        return activityController.setup().get()
    }

    private class FakePreparePaymentMethodHandler : PreparePaymentMethodHandler {
        var capturedShippingAddress: AddressDetails? = null
        var capturedPaymentMethod: PaymentMethod? = null

        override suspend fun onPreparePaymentMethod(
            paymentMethod: PaymentMethod,
            shippingAddress: AddressDetails?
        ) {
            capturedPaymentMethod = paymentMethod
            capturedShippingAddress = shippingAddress
        }
    }

    private fun testShopPayConfirmation(
        shippingAddressData: ECEShippingAddressData?,
        billingDetails: ECEBillingDetails,
        stripeRepositoryResult: Result<PaymentMethod>,
    ): Pair<FakePreparePaymentMethodHandler, ShopPayActivity> {
        val confirmationState = MutableStateFlow<ShopPayConfirmationState>(ShopPayConfirmationState.Pending)
        val bridgeHandler = createTestBridgeHandler(confirmationState)
        val stripeRepository = createTestStripeRepository(stripeRepositoryResult)

        val fakeHandler = FakePreparePaymentMethodHandler()
        val activity = setupActivityController(
            bridgeHandler = bridgeHandler,
            stripeRepository = stripeRepository,
            preparePaymentMethodHandler = fakeHandler
        )

        confirmationState.value = ShopPayConfirmationState.Success(
            externalSourceId = "test_id",
            billingDetails = billingDetails,
            shippingAddressData = shippingAddressData
        )

        return fakeHandler to activity
    }

    private fun assertShopPayResult(
        activity: ShopPayActivity,
        fakeHandler: FakePreparePaymentMethodHandler,
        expectedResult: Class<out ShopPayActivityResult>,
        expectedError: Throwable? = null,
        shippingAddressAssertion: (AddressDetails?) -> Unit
    ) {
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(63636)
        val result = getResultFromIntent(shadowActivity.resultIntent)
        assertThat(result).isInstanceOf(expectedResult)

        if (expectedError != null && result is ShopPayActivityResult.Failed) {
            assertThat(result.error).isEqualTo(expectedError)
        }

        shippingAddressAssertion(fakeHandler.capturedShippingAddress)

        assertNoUnverifiedIntents()
    }

    private fun setupPaymentElementCallbackReferences() {
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
}
