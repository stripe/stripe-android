package com.stripe.android.googlepaylauncher

import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.Logger
import com.stripe.android.PaymentConfiguration
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.googlepaylauncher.injection.DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherViewModelSubcomponent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject

internal class GooglePayPaymentMethodLauncherViewModel @Inject constructor(
    private val paymentsClient: PaymentsClient,
    private val requestOptions: ApiRequest.Options,
    private val args: GooglePayPaymentMethodLauncherContract.Args,
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
            isEmailRequired = args.config.isEmailRequired
        )
    }

    @VisibleForTesting
    internal fun createTransactionInfo(
        args: GooglePayPaymentMethodLauncherContract.Args
    ): GooglePayJsonFactory.TransactionInfo {
        return GooglePayJsonFactory.TransactionInfo(
            currencyCode = args.currencyCode,
            totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
            countryCode = args.config.merchantCountryCode,
            transactionId = args.transactionId,
            totalPrice = args.amount,
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

        return runCatching {
            requireNotNull(
                stripeRepository.createPaymentMethod(params, requestOptions)
            )
        }.fold(
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
        private val application: Application,
        private val args: GooglePayPaymentMethodLauncherContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs),
        Injectable<Factory.FallbackInjectionParams> {

        internal data class FallbackInjectionParams(
            val application: Application,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilder:
            GooglePayPaymentMethodLauncherViewModelSubcomponent.Builder

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            val enableLogging = args.injectionParams?.enableLogging ?: false
            val logger = Logger.getInstance(enableLogging)

            args.injectionParams?.let { injectionParams ->
                WeakMapInjectorRegistry.retrieve(injectionParams.injectorKey)?.let { injector ->
                    logger.info(
                        "Injector available, injecting dependencies into " +
                            "GooglePayPaymentMethodLauncherViewModel.Factory"
                    )
                    injector.inject(this)
                    true
                }
            } ?: run {
                logger.info(
                    "Injector unavailable, initializing dependencies of " +
                        "GooglePayPaymentMethodLauncherViewModel.Factory"
                )
                fallbackInitialize(
                    FallbackInjectionParams(
                        application,
                        enableLogging,
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
            }

            return subComponentBuilder
                .args(args)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInjectionParams) {
            DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .googlePayConfig(args.config)
                .build().inject(this)
        }
    }

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
