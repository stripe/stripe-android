package com.stripe.android.payments.bankaccount.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetInstantDebitsResult
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.LinkBankPaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.InstantDebits
import com.stripe.android.payments.bankaccount.di.DaggerCollectBankAccountComponent
import com.stripe.android.payments.bankaccount.domain.AttachFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.CreateFinancialConnectionsSession
import com.stripe.android.payments.bankaccount.domain.RetrieveStripeIntent
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal.InstantDebitsData
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal.USBankAccountData
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Cancelled
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Completed
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("ConstructorParameterNaming", "LongParameterList")
internal class CollectBankAccountViewModel @Inject constructor(
    // bound instances
    private val args: CollectBankAccountContract.Args,
    private val _viewEffect: MutableSharedFlow<CollectBankAccountViewEffect>,
    // injected instances
    private val createFinancialConnectionsSession: CreateFinancialConnectionsSession,
    private val attachFinancialConnectionsSession: AttachFinancialConnectionsSession,
    private val retrieveStripeIntent: RetrieveStripeIntent,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger
) : ViewModel() {

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_HAS_LAUNCHED) == true
        set(value) = savedStateHandle.set(KEY_HAS_LAUNCHED, value)

    val viewEffect: SharedFlow<CollectBankAccountViewEffect> = _viewEffect

    init {
        if (hasLaunched.not()) {
            viewModelScope.launch {
                createFinancialConnectionsSession()
            }
        }
    }

    private suspend fun createFinancialConnectionsSession() {
        when (args) {
            is CollectBankAccountContract.Args.ForDeferredPaymentIntent ->
                createFinancialConnectionsSession.forDeferredIntent(
                    publishableKey = args.publishableKey,
                    stripeAccountId = args.stripeAccountId,
                    hostedSurface = args.hostedSurface,
                    elementsSessionId = args.elementsSessionId,
                    customerId = args.customerId,
                    onBehalfOf = args.onBehalfOf,
                    amount = args.amount,
                    currency = args.currency,
                    instantDebits = args.configuration is InstantDebits
                )

            is CollectBankAccountContract.Args.ForDeferredSetupIntent ->
                createFinancialConnectionsSession.forDeferredIntent(
                    publishableKey = args.publishableKey,
                    stripeAccountId = args.stripeAccountId,
                    hostedSurface = args.hostedSurface,
                    elementsSessionId = args.elementsSessionId,
                    customerId = args.customerId,
                    onBehalfOf = args.onBehalfOf,
                    amount = null,
                    currency = null,
                    instantDebits = args.configuration is InstantDebits
                )

            is CollectBankAccountContract.Args.ForPaymentIntent ->
                createFinancialConnectionsSession.forPaymentIntent(
                    publishableKey = args.publishableKey,
                    stripeAccountId = args.stripeAccountId,
                    hostedSurface = args.hostedSurface,
                    clientSecret = args.clientSecret,
                    configuration = args.configuration
                )

            is CollectBankAccountContract.Args.ForSetupIntent ->
                createFinancialConnectionsSession.forSetupIntent(
                    publishableKey = args.publishableKey,
                    stripeAccountId = args.stripeAccountId,
                    hostedSurface = args.hostedSurface,
                    clientSecret = args.clientSecret,
                    configuration = args.configuration
                )
        }
            .mapCatching { requireNotNull(it.clientSecret) }
            .onSuccess { financialConnectionsSessionSecret: String ->
                logger.debug("Bank account session created! $financialConnectionsSessionSecret.")
                hasLaunched = true

                val elementsSessionContext = args.configuration.retrieveElementsSessionContext()
                _viewEffect.emit(
                    OpenConnectionsFlow(
                        financialConnectionsSessionSecret = financialConnectionsSessionSecret,
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        elementsSessionContext = elementsSessionContext,
                    )
                )
            }
            .onFailure { finishWithError(it) }
    }

    fun onConnectionsForACHResult(result: FinancialConnectionsSheetResult) {
        hasLaunched = false
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetResult.Canceled -> finishWithResult(Cancelled)

                is FinancialConnectionsSheetResult.Failed -> finishWithError(result.error)

                is FinancialConnectionsSheetResult.Completed -> when {
                    args.attachToIntent -> attachSessionToIntent(result.financialConnectionsSession)
                    else -> finishWithSession(result.financialConnectionsSession)
                }
            }
        }
    }

    fun onConnectionsForInstantDebitsResult(result: FinancialConnectionsSheetInstantDebitsResult) {
        hasLaunched = false
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetInstantDebitsResult.Canceled -> {
                    finishWithResult(Cancelled)
                }
                is FinancialConnectionsSheetInstantDebitsResult.Failed -> {
                    finishWithError(result.error)
                }
                is FinancialConnectionsSheetInstantDebitsResult.Completed -> {
                    finishWithPaymentMethodId(result)
                }
            }
        }
    }

    private suspend fun finishWithResult(result: CollectBankAccountResultInternal) {
        _viewEffect.emit(CollectBankAccountViewEffect.FinishWithResult(result))
    }

    private fun finishWithSession(
        financialConnectionsSession: FinancialConnectionsSession
    ) {
        finishWithRefreshedIntent { intent ->
            CollectBankAccountResponseInternal(
                intent = intent,
                usBankAccountData = USBankAccountData(
                    financialConnectionsSession
                ),
                instantDebitsData = null,
            )
        }
    }

    private fun finishWithPaymentMethodId(
        result: FinancialConnectionsSheetInstantDebitsResult.Completed,
    ) {
        finishWithRefreshedIntent { intent ->
            CollectBankAccountResponseInternal(
                intent = intent,
                usBankAccountData = null,
                instantDebitsData = InstantDebitsData(
                    paymentMethod = result.paymentMethod.toPaymentMethod(),
                    last4 = result.last4,
                    bankName = result.bankName,
                ),
            )
        }
    }

    private fun finishWithRefreshedIntent(
        action: (StripeIntent?) -> CollectBankAccountResponseInternal,
    ) {
        viewModelScope.launch {
            val clientSecret = args.clientSecret

            val retrieveIntentResult = if (clientSecret == null) {
                // client secret is null for deferred intents.
                Result.success(null)
            } else {
                retrieveStripeIntent(args.publishableKey, clientSecret)
            }

            retrieveIntentResult.onFailure {
                finishWithError(it)
            }.onSuccess { intent ->
                val response = action(intent)
                finishWithResult(Completed(response))
            }
        }
    }

    private fun attachSessionToIntent(financialConnectionsSession: FinancialConnectionsSession) {
        viewModelScope.launch {
            when (args) {
                is CollectBankAccountContract.Args.ForDeferredPaymentIntent,
                is CollectBankAccountContract.Args.ForDeferredSetupIntent ->
                    error("Attach requires client secret")

                is CollectBankAccountContract.Args.ForPaymentIntent ->
                    attachFinancialConnectionsSession.forPaymentIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        linkedAccountSessionId = financialConnectionsSession.id
                    )

                is CollectBankAccountContract.Args.ForSetupIntent ->
                    attachFinancialConnectionsSession.forSetupIntent(
                        publishableKey = args.publishableKey,
                        stripeAccountId = args.stripeAccountId,
                        clientSecret = args.clientSecret,
                        linkedAccountSessionId = financialConnectionsSession.id
                    )
            }
                .mapCatching { stripeIntent ->
                    Completed(
                        CollectBankAccountResponseInternal(
                            intent = stripeIntent,
                            usBankAccountData = USBankAccountData(financialConnectionsSession),
                            instantDebitsData = null
                        )
                    )
                }
                .onSuccess { result: Completed ->
                    logger.debug("Bank account session attached to intent!!")
                    finishWithResult(result)
                }
                .onFailure { finishWithError(it) }
        }
    }

    private suspend fun finishWithError(throwable: Throwable) {
        logger.error("Error", Exception(throwable))
        finishWithResult(Failed(throwable))
    }

    class Factory(
        private val argsSupplier: () -> CollectBankAccountContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            return DaggerCollectBankAccountComponent.builder()
                .savedStateHandle(savedStateHandle)
                .application(application)
                .viewEffect(MutableSharedFlow())
                .configuration(argsSupplier()).build()
                .viewModel as T
        }
    }

    companion object {
        private const val KEY_HAS_LAUNCHED = "key_has_launched"
    }
}

private fun CollectBankAccountConfiguration.retrieveElementsSessionContext(): ElementsSessionContext? {
    return (this as? InstantDebits)?.elementsSessionContext
}

// TODO: Rename to LinkBankPaymentMethod
private fun LinkBankPaymentMethod.toPaymentMethod(): PaymentMethod {
    val type = PaymentMethod.Type.Link

    return PaymentMethod.Builder()
        .setId(id)
        .setCode(type.code)
        .setType(type)
        .setAllowRedisplay(
            allowRedisplay?.let { value ->
                PaymentMethod.AllowRedisplay.entries.find { it.value == value }
            }
        )
        .setCreated(created)
        .setCustomerId(customer)
        .setBillingDetails(
            billingDetails?.let {
                PaymentMethod.BillingDetails(
                    address = it.address?.let { address ->
                        Address(
                            line1 = address.line1,
                            line2 = address.line2,
                            postalCode = address.postalCode,
                            city = address.city,
                            state = address.state,
                            country = address.country,
                        )
                    },
                    email = it.email,
                    name = it.name,
                    phone = it.phone,
                )
            }
        ) // TODO This will be filled in later
        .setLiveMode(livemode)
        .setCard(
            card?.let {
                PaymentMethod.Card(
                    brand = it.brand,
                    checks = it.checks?.let { checks ->
                        PaymentMethod.Card.Checks(
                            addressLine1Check = checks.addressLine1Check,
                            addressPostalCodeCheck = checks.addressPostalCodeCheck,
                            cvcCheck = checks.cvcCheck,
                        )
                    },
                    country = it.country,
                    expiryMonth = it.expiryMonth,
                    expiryYear = it.expiryYear,
                    fingerprint = it.fingerprint,
                    funding = it.funding,
                    last4 = it.last4,
                    threeDSecureUsage = it.threeDSecureUsage?.let { usage ->
                        PaymentMethod.Card.ThreeDSecureUsage(
                            isSupported = usage.isSupported,
                        )
                    },
                    networks = it.networks?.let { networks ->
                        PaymentMethod.Card.Networks(
                            available = networks.available,
                            preferred = networks.preferred,
                        )
                    },
                )
            }
        )
        .build()
}
