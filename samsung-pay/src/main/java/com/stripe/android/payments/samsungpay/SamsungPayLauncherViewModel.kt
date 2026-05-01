package com.stripe.android.payments.samsungpay

import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.payment.CardInfo
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo
import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

@Suppress("LongParameterList")
internal class SamsungPayLauncherViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val stripeIntentRepository: StripeIntentRepository,
    private val config: SamsungPayLauncher.Config,
    private val tokenExchangeHandler: TokenExchangeHandler,
    private val credentialParser: SamsungPayCredentialParser,
    private val context: Application,
    private val workContext: CoroutineContext,
) : ViewModel() {

    private val _result = MutableStateFlow<SamsungPayLauncher.Result?>(null)
    val result: StateFlow<SamsungPayLauncher.Result?> = _result.asStateFlow()

    private var paymentManager: PaymentManager? = null

    private var hasLaunched: Boolean
        get() = savedStateHandle[HAS_LAUNCHED_KEY] ?: false
        set(value) { savedStateHandle[HAS_LAUNCHED_KEY] = value }

    fun startPayment(args: SamsungPayLauncherContract.Args) {
        if (hasLaunched) return
        hasLaunched = true

        viewModelScope.launch(workContext) {
            try {
                val (amount, currency) = fetchIntentDetails(args)
                val paymentInfo = buildPaymentInfo(args, amount, currency)
                val partnerInfo = buildPartnerInfo(config)

                val manager = PaymentManager(context, partnerInfo)
                paymentManager = manager

                manager.startInAppPayWithCustomSheet(
                    paymentInfo,
                    createTransactionListener(args)
                )
            } catch (e: Exception) {
                _result.value = SamsungPayLauncher.Result.Failed(
                    SamsungPayException(errorCode = -1, message = e.message)
                )
            }
        }
    }

    private suspend fun fetchIntentDetails(
        args: SamsungPayLauncherContract.Args
    ): Pair<Long, String> {
        return when (args) {
            is SamsungPayLauncherContract.Args.PaymentIntentArgs -> {
                val intent = stripeIntentRepository.retrievePaymentIntent(args.clientSecret)
                val amount = intent.amount ?: error("PaymentIntent has no amount")
                val currency = intent.currency ?: error("PaymentIntent has no currency")
                amount to currency.uppercase()
            }
            is SamsungPayLauncherContract.Args.SetupIntentArgs -> {
                0L to args.currencyCode.uppercase()
            }
        }
    }

    private fun buildPaymentInfo(
        args: SamsungPayLauncherContract.Args,
        amount: Long,
        currency: String,
    ): CustomSheetPaymentInfo {
        val brandList = config.allowedCardBrands.mapNotNull { it.toSpaySdkBrand() }

        val amountBox = AmountBoxControl("amount", currency)
        amountBox.addItem(
            "itemId", "Total",
            amount.toDouble() / 100.0,
            ""
        )

        val sheet = CustomSheet().apply {
            addControl(amountBox)
        }

        val addressType = when (config.addressConfig.format) {
            AddressConfig.Format.None ->
                CustomSheetPaymentInfo.AddressInPaymentSheet.DO_NOT_SHOW
            AddressConfig.Format.BillingOnly ->
                CustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_SEND_SHIPPING
            AddressConfig.Format.ShippingOnly ->
                CustomSheetPaymentInfo.AddressInPaymentSheet.NEED_SHIPPING_SPAY
            AddressConfig.Format.BillingAndShipping ->
                CustomSheetPaymentInfo.AddressInPaymentSheet.NEED_BILLING_AND_SHIPPING
        }

        return CustomSheetPaymentInfo.Builder()
            .setMerchantName(config.merchantName)
            .setOrderNumber(args.clientSecret.substringBefore("_secret_"))
            .setAllowedCardBrands(brandList)
            .setCardHolderNameEnabled(config.cardHolderNameEnabled)
            .setAddressInPaymentSheet(addressType)
            .setCustomSheet(sheet)
            .build()
    }

    private fun createTransactionListener(
        args: SamsungPayLauncherContract.Args
    ) = object : PaymentManager.CustomSheetTransactionInfoListener {

        override fun onCardInfoUpdated(
            selectedCardInfo: CardInfo,
            sheet: CustomSheet
        ) {
            paymentManager?.updateSheet(sheet)
        }

        override fun onSuccess(
            response: CustomSheetPaymentInfo,
            paymentCredential: String,
            extraPaymentData: Bundle
        ) {
            viewModelScope.launch(workContext) {
                handlePaymentCredential(args, paymentCredential)
            }
        }

        override fun onFailure(errorCode: Int, errorData: Bundle?) {
            val reason = errorData?.getInt(SpaySdk.EXTRA_ERROR_REASON)
            val message = errorData?.getString(SpaySdk.EXTRA_ERROR_REASON_MESSAGE)
            _result.value = SamsungPayLauncher.Result.Failed(
                SamsungPayException(errorCode, reason, message)
            )
        }
    }

    private suspend fun handlePaymentCredential(
        args: SamsungPayLauncherContract.Args,
        paymentCredential: String,
    ) {
        try {
            val tokenRequest = credentialParser.parse(paymentCredential)
            val stripeTokenId = tokenExchangeHandler.exchangeForToken(tokenRequest)

            val card = PaymentMethodCreateParams.Card.create(token = stripeTokenId)
            val paymentMethodParams = PaymentMethodCreateParams.create(card = card)

            when (args) {
                is SamsungPayLauncherContract.Args.PaymentIntentArgs -> {
                    stripeIntentRepository.confirmPaymentIntent(
                        ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                            paymentMethodCreateParams = paymentMethodParams,
                            clientSecret = args.clientSecret,
                        )
                    )
                }
                is SamsungPayLauncherContract.Args.SetupIntentArgs -> {
                    stripeIntentRepository.confirmSetupIntent(
                        ConfirmSetupIntentParams.create(
                            paymentMethodCreateParams = paymentMethodParams,
                            clientSecret = args.clientSecret,
                        )
                    )
                }
            }
            _result.value = SamsungPayLauncher.Result.Completed
        } catch (e: Exception) {
            _result.value = SamsungPayLauncher.Result.Failed(
                SamsungPayException(errorCode = -1, message = e.message)
            )
        }
    }

    private fun buildPartnerInfo(config: SamsungPayLauncher.Config): PartnerInfo {
        val serviceId = config.productId
        val bundle = Bundle().apply {
            putString(
                SpaySdk.PARTNER_SERVICE_TYPE,
                SpaySdk.ServiceType.INAPP_PAYMENT.toString()
            )
        }
        return PartnerInfo(serviceId, bundle)
    }

    class Factory(
        private val args: SamsungPayLauncherContract.Args,
        private val tokenExchangeHandler: TokenExchangeHandler?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val handler = tokenExchangeHandler
                ?: throw IllegalStateException(
                    "TokenExchangeHandler is null. This can happen after process death. " +
                        "Samsung Pay cannot recover from process death."
                )

            return SamsungPayLauncherViewModel(
                savedStateHandle = savedStateHandle,
                stripeIntentRepository = DefaultStripeIntentRepository(application),
                config = args.config,
                tokenExchangeHandler = handler,
                credentialParser = SamsungPayCredentialParser,
                context = application,
                workContext = Dispatchers.IO,
            ) as T
        }
    }

    companion object {
        @VisibleForTesting
        const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
