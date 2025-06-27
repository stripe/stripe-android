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
    fun `popupWebView initial state is null`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.popupWebView.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `showPopup emits false when popupWebView is null and true when not null`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()

        viewModel.showPopup.test {
            assertThat(awaitItem()).isFalse()

            viewModel.setPopupWebView(mockWebView)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `setPopupWebView updates popupWebView state flow`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()

        viewModel.popupWebView.test {
            assertThat(awaitItem()).isNull()

            viewModel.setPopupWebView(mockWebView)
            assertThat(awaitItem()).isEqualTo(mockWebView)

            viewModel.setPopupWebView(null)
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `closePopup sets popupWebView to null`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()

        viewModel.popupWebView.test {
            assertThat(awaitItem()).isNull()

            viewModel.setPopupWebView(mockWebView)
            assertThat(awaitItem()).isEqualTo(mockWebView)

            viewModel.closePopup()
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `setWebView loads correct URL`() {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()

        viewModel.setWebView(mockWebView)

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
    fun `paymentResult emits Failed when confirmation state is Failure`() = runTest(dispatcher) {
        val exception = RuntimeException("Test error")
        val bridgeHandler = FakeShopPayBridgeHandler()
        val viewModel = createViewModel(bridgeHandler = bridgeHandler)

        viewModel.paymentResult.test {
            bridgeHandler.confirmationState.value = ShopPayConfirmationState.Failure(exception)

            val result = awaitItem()
            assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
            assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
        }
    }

    @Test
    fun `paymentResult emits nothing when confirmation state is Pending`() = runTest(dispatcher) {
        val bridgeHandler = FakeShopPayBridgeHandler(
            confirmationState = MutableStateFlow(Pending)
        )
        val viewModel = createViewModel(bridgeHandler = bridgeHandler)

        viewModel.paymentResult.test {
            expectNoEvents()
        }
    }

    @Test
    fun `paymentResult emits Completed when confirmation state is Success and payment method creation succeeds`() =
        runTest(dispatcher) {
            val billingDetails = createTestBillingDetails()
            val confirmationState = ShopPayConfirmationState.Success(
                externalSourceId = "test_external_id",
                billingDetails = billingDetails
            )
            val bridgeHandler = FakeShopPayBridgeHandler()
            val stripeRepository = FakeStripeRepository(Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
            val viewModel = createViewModel(
                bridgeHandler = bridgeHandler,
                stripeApiRepository = stripeRepository
            )

            viewModel.paymentResult.test {
                bridgeHandler.confirmationState.value = confirmationState

                val result = awaitItem()
                assertThat(result).isEqualTo(ShopPayActivityResult.Completed)
            }
        }

    @Test
    fun `paymentResult emits Failed when confirmation state is Success but payment method creation fails`() =
        runTest(dispatcher) {
            val billingDetails = createTestBillingDetails()
            val confirmationState = ShopPayConfirmationState.Success(
                externalSourceId = "test_external_id",
                billingDetails = billingDetails
            )
            val bridgeHandler = FakeShopPayBridgeHandler()
            val exception = RuntimeException("Payment method creation failed")
            val stripeRepository = FakeStripeRepository(Result.failure(exception))
            val viewModel = createViewModel(
                bridgeHandler = bridgeHandler,
                stripeApiRepository = stripeRepository
            )

            viewModel.paymentResult.test {
                bridgeHandler.confirmationState.value = confirmationState

                val result = awaitItem()
                assertThat(result).isInstanceOf<ShopPayActivityResult.Failed>()
                assertThat((result as ShopPayActivityResult.Failed).error).isEqualTo(exception)
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
            billingDetails = billingDetails
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
        paymentMethodHandler: PreparePaymentMethodHandler = PreparePaymentMethodHandler { _, _ -> },
        stripeApiRepository: StripeRepository = FakeStripeRepository()
    ): ShopPayViewModel {
        PaymentElementCallbacks.Builder()
            .shopPayHandlers(
                shopPayHandlers = ShopPayHandlers(
                    shippingMethodUpdateHandler = { null },
                    shippingContactHandler = { null }
                )
            ).preparePaymentMethodHandler(
                handler = { _, _ -> }
            ).build()
        return ShopPayViewModel(
            bridgeHandler,
            stripeApiRepository = stripeApiRepository,
            requestOptions = ApiRequest.Options("pk_123"),
            paymentMethodHandler = paymentMethodHandler,
            workContext = dispatcher
        )
    }

    private fun createFakeBridgeHandler(): FakeShopPayBridgeHandler {
        return FakeShopPayBridgeHandler()
    }

    private class FakeShopPayBridgeHandler(
        override val confirmationState: MutableStateFlow<ShopPayConfirmationState> = MutableStateFlow(Pending)
    ) : ShopPayBridgeHandler {
        override fun consoleLog(level: String, message: String, origin: String, url: String) = Unit
        override fun getStripePublishableKey(): String = "pk_test_fake_key"
        override fun handleECEClick(message: String): String = ""
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
        return ECEBillingDetails(
            name = "Test User",
            email = "test@example.com",
            phone = "+1234567890",
            address = ECEFullAddress(
                line1 = "123 Main St",
                line2 = null,
                city = "Anytown",
                state = "CA",
                postalCode = "12345",
                country = "US"
            )
        )
    }
}
