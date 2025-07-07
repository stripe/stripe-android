package com.stripe.android.shoppay

import android.content.Context
import android.content.res.AssetManager
import android.webkit.WebView
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
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
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.shoppay.bridge.ECEBillingDetails
import com.stripe.android.shoppay.bridge.ECEFullAddress
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState.Pending
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFailsWith

@OptIn(SharedPaymentTokenSessionPreview::class, ShopPayPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class ShopPayViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `closePopup emits canceled result`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.paymentResult.test {
            viewModel.closePopup()

            assertThat(awaitItem()).isEqualTo(ShopPayActivityResult.Canceled)
        }
    }

    @Test
    fun `loadUrl loads correct URL`() {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()

        viewModel.loadUrl(mockWebView)

        verify(mockWebView).loadUrl("https://pay.stripe.com/assets/www/index.html")
    }

    @Test
    fun `onPageLoaded loads JavaScript bridge from assets for stripe domain`() {
        testOnPageLoaded("https://pay.stripe.com/test")
    }

    @Test
    fun `onPageLoaded loads JavaScript bridge from assets for non-stripe domain`() {
        testOnPageLoaded("https://example.com/test")
    }

    @OptIn(ShopPayPreview::class)
    @Test
    fun `factory creates ViewModel when args are valid`() {
        val savedStateHandle = createSavedStateHandleWithValidArgs()
        val factory = ShopPayViewModel.factory(savedStateHandle)

        PaymentElementCallbackReferences["paymentElementCallbackIdentifier"] = PaymentElementCallbacks.Builder()
            .shopPayHandlers(
                shopPayHandlers = ShopPayHandlers(
                    shippingMethodUpdateHandler = { null },
                    shippingContactHandler = { null }
                )
            )
            .preparePaymentMethodHandler(
                handler = { _, _ -> }
            )
            .build()
        val viewModel = factory.create(ShopPayViewModel::class.java, createCreationExtras())

        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `factory throws exception when args are null`() {
        val savedStateHandle = SavedStateHandle().apply {
            set(ShopPayActivity.EXTRA_ARGS, null)
        }

        testFactoryWithInvalidArgs(savedStateHandle)
    }

    @Test
    fun `factory throws exception when args are missing from savedStateHandle`() {
        val savedStateHandle = SavedStateHandle()

        testFactoryWithInvalidArgs(savedStateHandle)
    }

    @Test
    fun `paymentResult emits nothing when confirmation state is Pending`() = runTest(dispatcher) {
        createViewModel().paymentResult.test {
            expectNoEvents()
        }
    }

    @Test
    fun `paymentResult emits Completed when confirmation state is Success and payment method creation succeeds`() =
        runTest(dispatcher) {
            val billingDetails = createTestBillingDetails()
            val confirmationState = ShopPayConfirmationState.Success(
                externalSourceId = "test_external_id",
                billingDetails = billingDetails,
                shippingAddressData = null
            )
            val bridgeHandler = FakeShopPayBridgeHandler()
            val stripeRepository = FakeStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))

            testPaymentResultWithConfirmationState(
                bridgeHandler = bridgeHandler,
                stripeRepository = stripeRepository,
                confirmationState = confirmationState,
                expectedResult = { result ->
                    assertThat(result).isEqualTo(ShopPayActivityResult.Completed)
                }
            )
        }

    @Test
    fun `paymentResult emits Failed when confirmation state is Success but payment method creation fails`() =
        runTest(dispatcher) {
            val exception = RuntimeException("Payment method creation failed")
            val confirmationState = createSuccessConfirmationState()

            testPaymentResultWithConfirmationState(
                stripeRepository = FakeStripeRepository(Result.failure(exception)),
                confirmationState = confirmationState,
                expectedResult = { result ->
                    assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
                    assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
                }
            )
        }

    @Test
    fun `paymentResult emits Failed when preparePaymentMethodHandler is unavailable`() = runTest(dispatcher) {
        val confirmationState = createSuccessConfirmationState()

        testPaymentResultWithConfirmationState(
            preparePaymentMethodHandler = null,
            confirmationState = confirmationState,
            expectedResult = { result ->
                assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
                val error = (result as ShopPayActivityResult.Failed).error
                assertThat(error).isInstanceOf<IllegalStateException>()
                assertThat(error.message).contains("PreparePaymentMethodHandler is required for ShopPay")
            }
        )
    }

    @Test
    fun `ECE click callback is set up correctly when ViewModel is created`() = runTest(dispatcher) {
        val mockEventReporter = mock<EventReporter>()
        val bridgeHandler = FakeShopPayBridgeHandler()
        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            eventReporter = mockEventReporter
        )

        bridgeHandler.handleECEClick("{}")

        viewModel.closePopup()

        verify(mockEventReporter).onShopPayWebViewCancelled(true)
    }

    @Test
    fun `analytics tracks cancellation without ECE click when closePopup is called`() {
        val mockEventReporter = mock<EventReporter>()
        val viewModel = createViewModel(eventReporter = mockEventReporter)

        viewModel.closePopup()

        verify(mockEventReporter).onShopPayWebViewCancelled(false)
    }

    @Test
    fun `analytics tracks cancellation with ECE click when closePopup is called after ECE click`() =
        runTest(dispatcher) {
            val mockEventReporter = mock<EventReporter>()
            val bridgeHandler = FakeShopPayBridgeHandler()
            val viewModel = createViewModel(
                bridgeHandler = bridgeHandler,
                eventReporter = mockEventReporter
            )

            bridgeHandler.handleECEClick("{}")

            viewModel.closePopup()

            verify(mockEventReporter).onShopPayWebViewCancelled(true)
        }

    @Test
    fun `analytics tracks confirm success when payment completes successfully`() = runTest(dispatcher) {
        val mockEventReporter = mock<EventReporter>()
        val bridgeHandler = FakeShopPayBridgeHandler()
        val confirmationState = createSuccessConfirmationState()

        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            eventReporter = mockEventReporter
        )

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = confirmationState
            awaitItem()

            verify(mockEventReporter).onShopPayWebViewConfirmSuccess()
        }
    }

    @Test
    fun `handleSuccessfulPayment creates correct PaymentMethodCreateParams`() = runTest(dispatcher) {
        val address = ECEFullAddress(
            line1 = "123 Test St",
            line2 = "Apt 4B",
            city = "New York",
            state = "NY",
            postalCode = "10001",
            country = "US"
        )
        val billingDetails = ECEBillingDetails(
            name = "John Doe",
            email = "john@example.com",
            phone = "+1234567890",
            address = address
        )
        val confirmationState = ShopPayConfirmationState.Success(
            externalSourceId = "test_external_id",
            billingDetails = billingDetails,
            shippingAddressData = null
        )
        val bridgeHandler = FakeShopPayBridgeHandler()

        var capturedParams: PaymentMethodCreateParams? = null
        val stripeRepository = object : AbsFakeStripeRepository() {
            override suspend fun createPaymentMethod(
                paymentMethodCreateParams: PaymentMethodCreateParams,
                options: ApiRequest.Options
            ): Result<PaymentMethod> {
                capturedParams = paymentMethodCreateParams
                return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            }
        }

        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            stripeApiRepository = stripeRepository
        )

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = confirmationState

            awaitItem()

            assertThat(capturedParams).isNotNull()
            assertThat(capturedParams?.typeCode).isEqualTo("shop_pay")
            assertThat(capturedParams?.billingDetails?.name).isEqualTo("John Doe")
            assertThat(capturedParams?.billingDetails?.email).isEqualTo("john@example.com")
            assertThat(capturedParams?.billingDetails?.phone).isEqualTo("+1234567890")
            assertThat(capturedParams?.billingDetails?.address?.line1).isEqualTo("123 Test St")
            assertThat(capturedParams?.billingDetails?.address?.line2).isEqualTo("Apt 4B")
            assertThat(capturedParams?.billingDetails?.address?.city).isEqualTo("New York")
            assertThat(capturedParams?.billingDetails?.address?.state).isEqualTo("NY")
            assertThat(capturedParams?.billingDetails?.address?.postalCode).isEqualTo("10001")
            assertThat(capturedParams?.billingDetails?.address?.country).isEqualTo("US")
        }
    }

    @Test
    fun `handleSuccessfulPayment uses shipping address data when available`() = runTest(dispatcher) {
        val confirmationState = ShopPayConfirmationState.Success(
            externalSourceId = "test_external_id",
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            shippingAddressData = ShopPayTestFactory.SHIPPING_ADDRESS_DATA
        )
        val bridgeHandler = FakeShopPayBridgeHandler()

        var capturedShippingAddress: AddressDetails? = null
        val mockHandler = PreparePaymentMethodHandler { _, shippingAddress ->
            capturedShippingAddress = shippingAddress
        }

        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            preparePaymentMethodHandler = mockHandler
        )

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = confirmationState

            awaitItem()

            assertThat(capturedShippingAddress).isNotNull()
            assertThat(capturedShippingAddress?.name).isEqualTo("Jane Smith")
            assertThat(capturedShippingAddress?.address?.line1).isEqualTo("456 Shipping Ave")
            assertThat(capturedShippingAddress?.address?.line2).isEqualTo("Unit 2B")
            assertThat(capturedShippingAddress?.address?.city).isEqualTo("Shipping City")
            assertThat(capturedShippingAddress?.address?.state).isEqualTo("NY")
            assertThat(capturedShippingAddress?.address?.postalCode).isEqualTo("10002")
            assertThat(capturedShippingAddress?.address?.country).isEqualTo("US")
        }
    }

    @Test
    fun `handleSuccessfulPayment passes null shipping address when shippingAddressData is null`() =
        runTest(dispatcher) {
            val confirmationState = ShopPayConfirmationState.Success(
                externalSourceId = "test_external_id",
                billingDetails = createTestBillingDetails(),
                shippingAddressData = null
            )
            val bridgeHandler = FakeShopPayBridgeHandler()

            var capturedShippingAddress: AddressDetails? = null
            val mockHandler = PreparePaymentMethodHandler { _, shippingAddress ->
                capturedShippingAddress = shippingAddress
            }

            val viewModel = createViewModel(
                bridgeHandler = bridgeHandler,
                preparePaymentMethodHandler = mockHandler
            )

            viewModel.paymentResult.test {
                bridgeHandler.confirmationState.value = confirmationState

                awaitItem()

                assertThat(capturedShippingAddress).isNull()
            }
        }

    @Test
    fun `handleSuccessfulPayment handles shipping address data with null address`() = runTest(dispatcher) {
        val confirmationState = ShopPayConfirmationState.Success(
            externalSourceId = "test_external_id",
            billingDetails = ShopPayTestFactory.BILLING_DETAILS,
            shippingAddressData = ShopPayTestFactory.SHIPPING_ADDRESS_DATA_WITH_NULL_ADDRESS
        )
        val bridgeHandler = FakeShopPayBridgeHandler()

        var capturedShippingAddress: AddressDetails? = null
        val mockHandler = PreparePaymentMethodHandler { _, shippingAddress ->
            capturedShippingAddress = shippingAddress
        }

        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            preparePaymentMethodHandler = mockHandler
        )

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = confirmationState

            awaitItem()

            assertThat(capturedShippingAddress).isNotNull()
            assertThat(capturedShippingAddress?.name).isEqualTo("Jane Smith")
            assertThat(capturedShippingAddress?.address).isNull()
        }
    }

    private fun testOnPageLoaded(url: String) {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()
        val mockContext = mock<Context>()
        val mockAssets = mock<AssetManager>()
        val mockInputStream = "mock js bridge content".byteInputStream()

        whenever(mockWebView.context).thenReturn(mockContext)
        whenever(mockContext.assets).thenReturn(mockAssets)
        whenever(mockAssets.open("www/native.js")).thenReturn(mockInputStream)

        viewModel.onPageLoaded(mockWebView, url)

        verify(mockAssets).open("www/native.js")
    }

    private fun testFactoryWithInvalidArgs(savedStateHandle: SavedStateHandle) {
        val factory = ShopPayViewModel.factory(savedStateHandle)

        val exception = assertFailsWith<IllegalArgumentException> {
            factory.create(ShopPayViewModel::class.java, createCreationExtras())
        }

        assertThat(exception.message).isEqualTo("No args found")
    }

    private fun createSavedStateHandleWithValidArgs(): SavedStateHandle {
        return SavedStateHandle().apply {
            set(
                ShopPayActivity.EXTRA_ARGS,
                ShopPayTestFactory.SHOP_PAY_ARGS
            )
        }
    }

    private fun createCreationExtras(): CreationExtras {
        val mockOwner = mock<SavedStateRegistryOwner>()
        val mockViewModelStoreOwner = mock<ViewModelStoreOwner>()
        whenever(mockViewModelStoreOwner.viewModelStore).thenReturn(ViewModelStore())

        return MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, mockOwner)
            set(
                ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY,
                ApplicationProvider.getApplicationContext()
            )
            set(VIEW_MODEL_STORE_OWNER_KEY, mockViewModelStoreOwner)
            set(ViewModelProvider.VIEW_MODEL_KEY, ShopPayViewModel::class.java.name)
        }
    }

    private fun createViewModel(
        bridgeHandler: ShopPayBridgeHandler = createFakeBridgeHandler(),
        preparePaymentMethodHandler: PreparePaymentMethodHandler? = PreparePaymentMethodHandler { _, _ -> },
        stripeApiRepository: StripeRepository = FakeStripeRepository(),
        eventReporter: EventReporter = mock()
    ): ShopPayViewModel {
        return ShopPayViewModel(
            bridgeHandler,
            stripeApiRepository = stripeApiRepository,
            requestOptions = ApiRequest.Options("pk_123"),
            preparePaymentMethodHandlerProvider = { preparePaymentMethodHandler },
            eventReporter = eventReporter,
            workContext = dispatcher
        )
    }

    private fun createFakeBridgeHandler(): FakeShopPayBridgeHandler {
        return FakeShopPayBridgeHandler()
    }

    private fun createSuccessConfirmationState() = ShopPayConfirmationState.Success(
        externalSourceId = "test_external_id",
        billingDetails = createTestBillingDetails(),
        shippingAddressData = null
    )

    private suspend fun testPaymentResultWithConfirmationState(
        bridgeHandler: FakeShopPayBridgeHandler = FakeShopPayBridgeHandler(),
        stripeRepository: FakeStripeRepository = FakeStripeRepository(),
        preparePaymentMethodHandler: PreparePaymentMethodHandler? = mock(),
        confirmationState: ShopPayConfirmationState.Success,
        expectedResult: (ShopPayActivityResult) -> Unit
    ) {
        val viewModel = createViewModel(
            bridgeHandler = bridgeHandler,
            stripeApiRepository = stripeRepository,
            preparePaymentMethodHandler = preparePaymentMethodHandler
        )

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = confirmationState
            val result = awaitItem()
            expectedResult(result)
        }
    }

    private class FakeShopPayBridgeHandler(
        override val confirmationState: MutableStateFlow<ShopPayConfirmationState> = MutableStateFlow(Pending)
    ) : ShopPayBridgeHandler {
        private var onECEClickCallback: (() -> Unit)? = null

        override fun setOnECEClickCallback(callback: () -> Unit) {
            onECEClickCallback = callback
        }

        override fun consoleLog(level: String, message: String, origin: String, url: String) = Unit
        override fun getStripePublishableKey(): String = "pk_test_fake_key"
        override fun handleECEClick(message: String): String {
            onECEClickCallback?.invoke()
            return ""
        }

        override fun getShopPayInitParams(): String = ""
        override fun calculateShipping(message: String) = null
        override fun calculateShippingRateChange(message: String) = null
        override fun confirmPayment(message: String): String = ""
        override fun ready(message: String) = Unit
    }

    private class FakeStripeRepository(
        private val createPaymentMethodResult: Result<PaymentMethod> = Result.success(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
    ) : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): Result<PaymentMethod> = createPaymentMethodResult
    }

    private fun createTestBillingDetails(): ECEBillingDetails {
        return ShopPayTestFactory.BILLING_DETAILS
    }
}
