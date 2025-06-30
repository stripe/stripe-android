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
import com.stripe.android.shoppay.ShopPayActivity.Companion.getArgs
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ShopPayConfirmationState
import com.stripe.android.shoppay.di.DaggerShopPayComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class ShopPayViewModel @Inject constructor(
    val bridgeHandler: ShopPayBridgeHandler,
    private val stripeApiRepository: StripeRepository,
    private val requestOptions: ApiRequest.Options,
    private val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?>,
    @UIContext workContext: CoroutineContext = Dispatchers.Main
) : ViewModel(CoroutineScope(workContext + SupervisorJob())) {
    private val _popupWebView = MutableStateFlow<WebView?>(null)
    val popupWebView: StateFlow<WebView?> = _popupWebView

    val showPopup: StateFlow<Boolean> = _popupWebView.mapLatest {
        it != null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    private val _paymentResult = MutableSharedFlow<ShopPayActivityResult>()
    val paymentResult: Flow<ShopPayActivityResult> = _paymentResult

    init {
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
                    _paymentResult.emit(handleSuccessfulPayment(confirmationState))
                }
            }
        }
    }

    private suspend fun handleSuccessfulPayment(
        confirmationState: ShopPayConfirmationState.Success
    ): ShopPayActivityResult {
        val address = confirmationState.billingDetails.address
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
            )
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
                shippingAddress = AddressDetails(
                    name = confirmationState.billingDetails.name,
                    address = PaymentSheet.Address(
                        city = address?.city,
                        country = address?.country,
                        line1 = address?.line1,
                        line2 = address?.line2,
                        postalCode = address?.postalCode,
                        state = address?.state,
                    )
                )
            )
            ShopPayActivityResult.Completed
        }.getOrElse {
            ShopPayActivityResult.Failed(it)
        }
    }

    fun setWebView(webView: WebView) {
        webView.loadUrl("https://pay.stripe.com/assets/www/index.html")
    }

    fun setPopupWebView(webView: WebView?) {
        _popupWebView.value = webView
    }

    fun closePopup() {
        _popupWebView.value = null
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

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: ShopPayArgs = getArgs(handle) ?: throw IllegalArgumentException("No args found")
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
}
