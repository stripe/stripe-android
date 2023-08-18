package com.stripe.android.googlepaylauncher

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.injection.DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherViewModelSubcomponent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject

internal class GooglePayPaymentMethodLauncherViewModel @Inject constructor(
    private val paymentsClient: PaymentsClient,
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayPaymentMethodLauncherContractV2.Args,
    private val stripeRepository: StripeRepository,
    private val googlePayJsonFactory: GooglePayJsonFactory,
    private val googlePayRepository: GooglePayRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    /**
     * [hasLaunched] indicates whether Google Pay has already been launched, and must be persisted
     * across process death in case the Activity and ViewModel are destroyed while the user is
     * interacting with Google Pay.
     */
    internal var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    private val _googleResult = MutableLiveData<GooglePayPaymentMethodLauncher.Result>()
    internal val googlePayResult = _googleResult.distinctUntilChanged()

    fun updateResult(result: GooglePayPaymentMethodLauncher.Result) {
        _googleResult.value = result
    }

    @VisibleForTesting
    suspend fun isReadyToPay(): Boolean {
        return googlePayRepository.isReady().first()
    }

    @VisibleForTesting
    fun createPaymentDataRequest(): JSONObject {
        return googlePayJsonFactory.createPaymentDataRequest(
            transactionInfo = createTransactionInfo(args),
            merchantInfo = GooglePayJsonFactory.MerchantInfo(
                merchantName = args.config.merchantName
            ),
            billingAddressParameters = args.config.billingAddressConfig.convert(),
            isEmailRequired = args.config.isEmailRequired,
            allowCreditCards = args.config.allowCreditCards
        )
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        args: GooglePayPaymentMethodLauncherContractV2.Args
    ): GooglePayJsonFactory.TransactionInfo {
        return GooglePayJsonFactory.TransactionInfo(
            currencyCode = args.currencyCode,
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
            countryCode = args.config.merchantCountryCode,
            transactionId = args.transactionId,
            totalPrice = args.amount,
            totalPriceLabel = null,
            checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
        )
    }

    suspend fun createLoadPaymentDataTask(): Task<PaymentData> {
        check(isReadyToPay()) {
            "Google Pay is unavailable."
        }
        return paymentsClient.loadPaymentData(
            PaymentDataRequest.fromJson(createPaymentDataRequest().toString())
        )
    }

    suspend fun createPaymentMethod(
        paymentData: PaymentData
    ): GooglePayPaymentMethodLauncher.Result {
        val paymentDataJson = JSONObject(paymentData.toJson())

        val params = PaymentMethodCreateParams.createFromGooglePay(paymentDataJson)

        return stripeRepository.createPaymentMethod(params, requestOptions).fold(
            onSuccess = {
                GooglePayPaymentMethodLauncher.Result.Completed(it)
            },
            onFailure = {
                GooglePayPaymentMethodLauncher.Result.Failed(
                    it,
                    when (it) {
                        is APIConnectionException -> GooglePayPaymentMethodLauncher.NETWORK_ERROR
                        is InvalidRequestException -> GooglePayPaymentMethodLauncher.DEVELOPER_ERROR
                        else -> GooglePayPaymentMethodLauncher.INTERNAL_ERROR
                    }
                )
            }
        )
    }

    internal class Factory(
        private val args: GooglePayPaymentMethodLauncherContractV2.Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInjectionParams> {

        internal data class FallbackInjectionParams(
            val application: Application,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>,
        )

        @Inject
        lateinit var subComponentBuilder:
            GooglePayPaymentMethodLauncherViewModelSubcomponent.Builder

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            injectWithFallback(
                injectorKey = args.injectionParams?.injectorKey,
                fallbackInitializeParam = FallbackInjectionParams(
                    application,
                    args.injectionParams?.enableLogging ?: false,
                    args.injectionParams?.publishableKey
                        ?: PaymentConfiguration.getInstance(application).publishableKey,
                    if (args.injectionParams != null) {
                        args.injectionParams.stripeAccountId
                    } else {
                        PaymentConfiguration.getInstance(application).stripeAccountId
                    },
                    args.injectionParams?.productUsage
                        ?: setOf(GooglePayPaymentMethodLauncher.PRODUCT_USAGE_TOKEN)
                )
            )

            return subComponentBuilder
                .args(args)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInjectionParams): Injector? {
            DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .googlePayConfig(args.config)
                .build().inject(this)
            return null
        }
    }

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
