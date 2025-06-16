package com.stripe.android.shoppay

import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.webkit.WebViewAssetLoader
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.shoppay.bridge.BridgeHandler
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShopPayViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()

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
    fun `showPopup emits false when popupWebView is null`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.showPopup.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `showPopup emits true when popupWebView is not null`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.showPopup.test {
            assertThat(awaitItem()).isFalse()

            viewModel.setPopupWebView(mock())
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

            // Clear web view
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
    fun `assetLoader creates WebViewAssetLoader with correct domain and path handler`() {
        val viewModel = createViewModel()

        val assetLoader = viewModel.assetLoader(context)

        assertThat(assetLoader).isInstanceOf(WebViewAssetLoader::class.java)
    }

    @Test
    fun `onPageLoaded loads JavaScript bridge from assets for stripe domain`() {
        val viewModel = createViewModel()
        val mockContext = mock<Context>()
        val mockWebView = mock<WebView>()
        val mockAssets = mock<android.content.res.AssetManager>()
        val mockInputStream = "mock js bridge content".byteInputStream()

        whenever(mockWebView.context).thenReturn(mockContext)
        whenever(mockContext.assets).thenReturn(mockAssets)
        whenever(mockAssets.open("www/native.js")).thenReturn(mockInputStream)

        val stripeUrl = "https://pay.stripe.com/test"

        viewModel.onPageLoaded(mockWebView, stripeUrl)

        verify(mockAssets).open("www/native.js")
    }

    @Test
    fun `onPageLoaded loads JavaScript bridge from assets for non-stripe domain`() {
        val viewModel = createViewModel()
        val mockWebView = mock<WebView>()
        val mockContext = mock<Context>()
        val mockAssets = mock<android.content.res.AssetManager>()
        val mockInputStream = "mock js bridge content".byteInputStream()

        whenever(mockWebView.context).thenReturn(mockContext)
        whenever(mockContext.assets).thenReturn(mockAssets)
        whenever(mockAssets.open("www/native.js")).thenReturn(mockInputStream)

        val nonStripeUrl = "https://example.com/test"

        viewModel.onPageLoaded(mockWebView, nonStripeUrl)

        verify(mockAssets).open("www/native.js")
    }

    @Test
    fun `factory creates ViewModel when args are valid`() {
        val mockArgs = ShopPayArgs(
            shopPayConfiguration = createShopPayConfiguration(),
            publishableKey = "pk_test_valid_key",
        )
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle[ShopPayActivity.EXTRA_ARGS] = mockArgs

        val factory = ShopPayViewModel.factory(savedStateHandle)

        // Should create successfully without throwing
        val viewModel = factory.create(
            ShopPayViewModel::class.java,
            createCreationExtras()
        )
        assertThat(viewModel).isNotNull()
    }

    @Test
    fun `factory throws exception when args are null`() {
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        savedStateHandle[ShopPayActivity.EXTRA_ARGS] = null

        val factory = ShopPayViewModel.factory(savedStateHandle)

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            factory.create(ShopPayViewModel::class.java, createCreationExtras())
        }
        assertThat(exception.message).isEqualTo("No args found")
    }

    @Test
    fun `factory throws exception when args are missing from savedStateHandle`() {
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()

        val factory = ShopPayViewModel.factory(savedStateHandle)

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            factory.create(ShopPayViewModel::class.java, createCreationExtras())
        }
        assertThat(exception.message).isEqualTo("No args found")
    }

    private fun createShopPayConfiguration(): com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration {
        return com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration(
            shopId = "test_shop_123",
            billingAddressRequired = true,
            emailRequired = true,
            shippingAddressRequired = false,
            lineItems = listOf(
                com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.LineItem(
                    name = "Test Item",
                    amount = 1000
                )
            ),
            shippingRates = emptyList()
        )
    }

    private fun createCreationExtras(): androidx.lifecycle.viewmodel.CreationExtras {
        val mockOwner = mock<androidx.savedstate.SavedStateRegistryOwner>()
        val mockViewModelStoreOwner = mock<androidx.lifecycle.ViewModelStoreOwner>()
        whenever(mockViewModelStoreOwner.viewModelStore).thenReturn(androidx.lifecycle.ViewModelStore())

        return androidx.lifecycle.viewmodel.MutableCreationExtras().apply {
            set(androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY, mockOwner)
            set(
                androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY,
                ApplicationProvider.getApplicationContext()
            )
            set(androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY, mockViewModelStoreOwner)
            set(androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY, ShopPayViewModel::class.java.name)
        }
    }

    private fun createViewModel(
        bridgeHandler: BridgeHandler = createFakeBridgeHandler()
    ): ShopPayViewModel {
        return ShopPayViewModel(bridgeHandler)
    }

    private fun createFakeBridgeHandler(): FakeBridgeHandler {
        return FakeBridgeHandler()
    }

    private class FakeBridgeHandler : BridgeHandler {

        override fun consoleLog(level: String, message: String, origin: String, url: String) = Unit

        override fun getStripePublishableKey(): String {
            return "pk_test_fake_key"
        }

        override fun ready(message: String) = Unit
    }
}
