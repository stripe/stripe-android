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
import com.stripe.android.common.model.SHOP_PAY_CONFIGURATION
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState.Pending
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
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
                ShopPayArgs(
                    shopPayConfiguration = SHOP_PAY_CONFIGURATION,
                    publishableKey = "pk_test_valid_key",
                    paymentElementCallbackIdentifier = "paymentElementCallbackIdentifier",
                    customerSessionClientSecret = "customer_secret",
                    businessName = "Example Inc"
                )
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
        bridgeHandler: ShopPayBridgeHandler = createFakeBridgeHandler()
    ): ShopPayViewModel {
        return ShopPayViewModel(bridgeHandler)
    }

    private fun createFakeBridgeHandler(): FakeShopPayBridgeHandler {
        return FakeShopPayBridgeHandler()
    }

    private class FakeShopPayBridgeHandler(
        override val confirmationState: StateFlow<ShopPayConfirmationState> = stateFlowOf(Pending)
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
}
