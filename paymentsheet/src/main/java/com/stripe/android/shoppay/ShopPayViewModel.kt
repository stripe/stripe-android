package com.stripe.android.shoppay

import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.webkit.WebViewAssetLoader
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.shoppay.ShopPayActivity.Companion.getArgs
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.shoppay.di.DaggerShopPayComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class ShopPayViewModel @Inject constructor(
    private val shopPayArgs: ShopPayArgs,
    val bridgeHandler: ShopPayBridgeHandler,
    private val stripeApiRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    private val eventReporter: EventReporter,
    @UIContext workContext: CoroutineContext = Dispatchers.Main,
) : ViewModel(CoroutineScope(workContext + SupervisorJob())) {

    private val _paymentResult = MutableSharedFlow<ShopPayActivityResult>()
    val paymentResult: Flow<ShopPayActivityResult> = _paymentResult

    private var didReceiveECEClick: Boolean = false

    val sheetHeightRatio = shopPayArgs.shopPayConfiguration.sheetRatio

    init {
        bridgeHandler.setOnECEClickCallback {
            didReceiveECEClick = true
        }

        viewModelScope.launch {
            listenToConfirmationState()
        }
    }

    private suspend fun listenToConfirmationState() {
        bridgeHandler.confirmationState.collectLatest { confirmationState ->
            when (confirmationState) {
                is ShopPayConfirmationState.Failure -> {
                    _paymentResult.emit(ShopPayActivityResult.Failed(confirmationState.cause))
                }
                ShopPayConfirmationState.Pending -> Unit
                is ShopPayConfirmationState.Success -> {
                    val result = handleSuccessfulPayment(confirmationState)
                    if (result is ShopPayActivityResult.Completed) {
                        eventReporter.onShopPayWebViewConfirmSuccess()
                    }
                    _paymentResult.emit(result)
                }
            }
        }
    }

    private suspend fun handleSuccessfulPayment(
        confirmationState: ShopPayConfirmationState.Success
    ): ShopPayActivityResult {
        val address = confirmationState.billingDetails.address
        val shippingAddressData = confirmationState.shippingAddressData
        val paymentMethodCreateParams = PaymentMethodCreateParams.createShopPay(
            externalSourceId = confirmationState.externalSourceId,
            billingDetails = PaymentMethod.BillingDetails(
                name = confirmationState.billingDetails.name,
                email = confirmationState.billingDetails.email,
                phone = confirmationState.billingDetails.phone,
                address = Address(
                    city = address?.city,
                    country = address?.country,
                    line1 = address?.line1,
                    line2 = address?.line2,
                    postalCode = address?.postalCode,
                    state = address?.state,
                )
            ),
        )
        return stripeApiRepository.createPaymentMethod(
            paymentMethodCreateParams = paymentMethodCreateParams,
            options = requestOptions
        ).map { paymentMethod ->
            val paymentMethodHandler = preparePaymentMethodHandlerProvider.get()
                ?: return@map ShopPayActivityResult.Failed(
                    error = IllegalStateException("PreparePaymentMethodHandler is required for ShopPay")
                )
            paymentMethodHandler.onPreparePaymentMethod(
                paymentMethod = paymentMethod,
                shippingAddress = shippingAddressData?.let {
                    AddressDetails(
                        name = shippingAddressData.name,
                        address = shippingAddressData.address?.let { address ->
                            PaymentSheet.Address(
                                city = address.city,
                                country = address.country,
                                line1 = address.line1,
                                line2 = address.line2,
                                postalCode = address.postalCode,
                                state = address.state,
                            )
                        }
                    )
                }
            )
            ShopPayActivityResult.Completed
        }.getOrElse {
            ShopPayActivityResult.Failed(it)
        }
    }

    fun closePopup() {
        eventReporter.onShopPayWebViewCancelled(didReceiveECEClick)
        viewModelScope.launch {
            _paymentResult.emit(ShopPayActivityResult.Canceled)
        }
    }

    fun assetLoader(context: Context): WebViewAssetLoader {
        return WebViewAssetLoader.Builder()
            .setDomain("pay.stripe.com")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }

    fun onPageLoaded(view: WebView, url: String) {
        val jsBridge = view.context.assets.open("www/native.js")
            .bufferedReader()
            .use(BufferedReader::readText)

        view.evaluateJavascript(jsBridge, null)

        if (url.contains("pay.stripe.com")) {
            view.evaluateJavascript("initializeApp()") {
                Log.d("WebViewBridge", "initializeApp() => $it")
            }
        }
    }

    fun loadUrl(webView: WebView) {
        eventReporter.onShopPayWebViewLoadAttempt()
        webView.loadUrl("https://pay.stripe.com/assets/www/index.html")
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: ShopPayArgs = getArgs(handle) ?: throw NoArgsException()
                DaggerShopPayComponent
                    .builder()
                    .context(app)
                    .paymentElementCallbackIdentifier(args.paymentElementCallbackIdentifier)
                    .shopPayArgs(args)
                    .stripeAccountIdProvider {
                        args.stripeAccountId
                    }
                    .publishableKeyProvider {
                        args.publishableKey
                    }
                    .build()
                    .viewModel
            }
        }
    }

    class NoArgsException : IllegalArgumentException("No args found")
}
