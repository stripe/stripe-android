package com.stripe.android.googlepaylauncher

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.stripe.android.BuildConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.injection.DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.Locale
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

    private val _googleResult = MutableStateFlow<GooglePayPaymentMethodLauncher.Result?>(null)
    internal val googlePayResult = _googleResult.asStateFlow()

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
        return if (shouldHidePrice(args)) {
            GooglePayJsonFactory.TransactionInfo(
                currencyCode = args.currencyCode,
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.NotCurrentlyKnown,
                countryCode = args.config.merchantCountryCode,
                transactionId = args.transactionId,
                totalPriceLabel = args.label,
                checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
            )
        } else {
            GooglePayJsonFactory.TransactionInfo(
                currencyCode = args.currencyCode,
                totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                countryCode = args.config.merchantCountryCode,
                transactionId = args.transactionId,
                totalPrice = args.amount,
                totalPriceLabel = args.label,
                checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
            )
        }
    }

    /**
     * Only hide the price when the merchant is collecting the payment details for future use.
     */
    private fun shouldHidePrice(args: GooglePayPaymentMethodLauncherContractV2.Args): Boolean {
        return (
            args.config.merchantCountryCode.equals(Locale.US.country, ignoreCase = true) ||
                args.config.merchantCountryCode.equals(Locale.CANADA.country, ignoreCase = true)
            ) && args.amount == 0L
    }

    suspend fun loadPaymentData(): Task<PaymentData> {
        check(isReadyToPay()) {
            "Google Pay is unavailable."
        }
        return paymentsClient.loadPaymentData(
            PaymentDataRequest.fromJson(createPaymentDataRequest().toString())
        ).awaitTask()
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
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val subComponentBuilder = DaggerGooglePayPaymentMethodLauncherViewModelFactoryComponent.builder()
                .context(application)
                .enableLogging(BuildConfig.DEBUG)
                .publishableKeyProvider {
                    PaymentConfiguration.getInstance(application).publishableKey
                }
                .stripeAccountIdProvider {
                    PaymentConfiguration.getInstance(application).stripeAccountId
                }
                .productUsage(setOf(GooglePayPaymentMethodLauncher.PRODUCT_USAGE_TOKEN))
                .googlePayConfig(args.config)
                .cardBrandFilter(args.cardBrandFilter)
                .build().subcomponentBuilder

            return subComponentBuilder
                .args(args)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }
    }

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
