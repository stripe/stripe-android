package com.stripe.android.checkout

import android.app.Application
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.injection.CheckoutControllerComponent
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.analytics.PaymentSheetEvent
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptionsFactory
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class CheckoutController(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val resultCallback: ResultCallback,
) {

    private val mutex = Mutex()

    private var configuredState: ConfiguredState? = null

    private val _checkoutSession = MutableStateFlow<CheckoutSession?>(null)
    val checkoutSession: StateFlow<CheckoutSession?> = _checkoutSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _paymentOption = MutableStateFlow<PaymentElement.PaymentOptionDisplayData?>(null)
    val paymentOption: StateFlow<PaymentElement.PaymentOptionDisplayData?> = _paymentOption.asStateFlow()

    var state: State?
        get() {
            // TODO: Capture full internal state into a restorable State object.
            return null
        }
        set(value) {
            // TODO: Restore internal state from the provided State, populate StateFlows.
        }

    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): kotlin.Result<Unit> {
        val configurationState = configuration.build()
        val component = DaggerCheckoutControllerComponent.factory().create(application)
        val sessionId = checkoutSessionClientSecret.substringBefore("_secret_")
        val adaptivePricingAllowed = configurationState.adaptivePricingAllowed
        return component.checkoutSessionRepository.init(
            sessionId = sessionId,
            adaptivePricingAllowed = adaptivePricingAllowed,
        ).map { response ->
            val flagImages = prefetchFlagImages(response, component)
            configuredState = ConfiguredState(
                component = component,
                sessionId = sessionId,
                adaptivePricingAllowed = adaptivePricingAllowed,
                flagImages = flagImages,
                checkoutSessionResponse = response,
            )
            _checkoutSession.value = response.toCheckoutSession(flagImages)
        }
    }

    suspend fun applyPromotionCode(promotionCode: String): kotlin.Result<Unit> {
        return withMutation { state ->
            state.component.checkoutSessionRepository.applyPromotionCode(
                state.sessionId,
                promotionCode.trim(),
            )
        }
    }

    suspend fun removePromotionCode(): kotlin.Result<Unit> {
        return withMutation { state ->
            state.component.checkoutSessionRepository.applyPromotionCode(state.sessionId, "")
        }
    }

    suspend fun updateLineItemQuantity(lineItemId: String, quantity: Int): kotlin.Result<Unit> {
        return withMutation { state ->
            state.component.checkoutSessionRepository.updateLineItemQuantity(
                state.sessionId,
                lineItemId,
                quantity,
            )
        }
    }

    suspend fun selectShippingOption(id: String): kotlin.Result<Unit> {
        return withMutation { state ->
            state.component.checkoutSessionRepository.selectShippingRate(state.sessionId, id)
        }
    }

    suspend fun updateShippingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        val built = address.build()
        return withMutation { state ->
            state.component.checkoutSessionRepository.updateTaxRegion(state.sessionId, built)
        }
    }

    suspend fun updateTaxId(type: String, value: String): kotlin.Result<Unit> {
        return withMutation { state ->
            state.component.checkoutSessionRepository.updateTaxId(
                state.sessionId,
                type.trim(),
                value.trim(),
            )
        }
    }

    suspend fun updateBillingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        val built = address.build()
        return withMutation { state ->
            state.component.checkoutSessionRepository.updateTaxRegion(state.sessionId, built)
        }
    }

    suspend fun runServerUpdate(serverUpdate: suspend () -> kotlin.Result<Unit>): kotlin.Result<Unit> {
        return withMutation { state ->
            withTimeout(SERVER_UPDATE_TIMEOUT_MS) { serverUpdate() }.fold(
                onSuccess = {
                    state.component.checkoutSessionRepository.init(
                        sessionId = state.sessionId,
                        adaptivePricingAllowed = state.adaptivePricingAllowed,
                    )
                },
                onFailure = { kotlin.Result.failure(it) },
            )
        }
    }

    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        return CheckoutPresenter(activity, this)
    }

    fun destroy() {
        // TODO: Clean up resources, cancel scopes, remove from instance tracking.
    }

    private suspend fun withMutation(
        block: suspend (ConfiguredState) -> kotlin.Result<CheckoutSessionResponse>,
    ): kotlin.Result<Unit> {
        val state = configuredState
            ?: return kotlin.Result.failure(IllegalStateException("CheckoutController is not configured."))
        return mutex.withLock {
            _isLoading.value = true
            val result = runCatching {
                block(state).getOrThrow()
            }.map { response ->
                state.checkoutSessionResponse = response
                _checkoutSession.value = response.toCheckoutSession(state.flagImages)
            }
            _isLoading.value = false
            result
        }
    }

    private suspend fun prefetchFlagImages(
        response: CheckoutSessionResponse,
        component: CheckoutControllerComponent,
    ): Map<String, Bitmap>? {
        val adaptivePricingInfo = response.adaptivePricingInfo ?: return null
        val localOption = adaptivePricingInfo.localCurrencyOptions.firstOrNull() ?: return null
        val flagImageRepository = FlagImageRepository(
            imageLoader = DefaultStripeImageLoader(application),
            displayDensity = application.resources.displayMetrics.density,
        )
        val result = flagImageRepository.fetch(
            integrationCurrencyCode = adaptivePricingInfo.integrationCurrency,
            localCurrencyCode = localOption.currency,
        )
        for (failure in result.failures) {
            val event = PaymentSheetEvent.AdaptivePricingFlagImageLoadFailed(
                countryCode = failure.countryCode,
                url = failure.url,
            )
            component.analyticsRequestExecutor.executeAsync(
                component.paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.params,
                )
            )
        }
        return result.images
    }

    private class ConfiguredState(
        val component: CheckoutControllerComponent,
        val sessionId: String,
        val adaptivePricingAllowed: Boolean,
        val flagImages: Map<String, Bitmap>?,
        var checkoutSessionResponse: CheckoutSessionResponse,
    )

    // --- Result ---

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        class Completed internal constructor() : Result

        class Canceled internal constructor() : Result

        @Poko
        class Failed internal constructor(val error: Throwable) : Result
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    // --- State ---

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val placeholder: String,
    ) : Parcelable

    // --- Configuration ---

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false
        private var paymentElementConfiguration: PaymentElement.Configuration = PaymentElement.Configuration()
        private var shippingAddressElementConfiguration: ShippingAddressElement.Configuration =
            ShippingAddressElement.Configuration()

        fun adaptivePricingAllowed(
            adaptivePricingAllowed: Boolean
        ): Configuration = apply {
            this.adaptivePricingAllowed = adaptivePricingAllowed
        }

        fun paymentElement(
            configuration: PaymentElement.Configuration
        ): Configuration = apply {
            this.paymentElementConfiguration = configuration
        }

        fun shippingAddressElement(
            configuration: ShippingAddressElement.Configuration
        ): Configuration = apply {
            this.shippingAddressElementConfiguration = configuration
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
            val paymentElementConfiguration: PaymentElement.Configuration.State,
            val shippingAddressElementConfiguration: ShippingAddressElement.Configuration.State,
        ) : Parcelable

        internal fun build(): State = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
            paymentElementConfiguration = paymentElementConfiguration.build(),
            shippingAddressElementConfiguration = shippingAddressElementConfiguration.build(),
        )
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.toCheckoutSession(
    flagImages: Map<String, Bitmap>?,
): CheckoutSession {
    return CheckoutSession(
        id = id,
        status = status.toStatus(),
        liveMode = liveMode,
        currency = currency,
        customerEmail = customerEmail,
        tax = taxStatus.toTax(),
        totalSummary = totalSummary?.toTotalSummary(),
        lineItems = lineItems.map { it.toLineItem() },
        shippingOptions = shippingOptions.map { it.toShippingRate() },
        currencySelectorOptions = CurrencySelectorOptionsFactory.create(
            adaptivePricingInfo = adaptivePricingInfo,
            flagImages = flagImages,
        ),
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.Status.toStatus(): CheckoutSession.Status {
    return when (this) {
        CheckoutSessionResponse.Status.OPEN -> CheckoutSession.Status.Open
        CheckoutSessionResponse.Status.COMPLETE -> CheckoutSession.Status.Complete
        CheckoutSessionResponse.Status.EXPIRED -> CheckoutSession.Status.Expired
        CheckoutSessionResponse.Status.UNKNOWN -> CheckoutSession.Status.Unknown
    }
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxStatus.toTax(): CheckoutSession.Tax {
    val status = when (this) {
        CheckoutSessionResponse.TaxStatus.READY -> CheckoutSession.Tax.Status.Ready
        CheckoutSessionResponse.TaxStatus.REQUIRES_SHIPPING_ADDRESS -> CheckoutSession.Tax.Status.RequiresShippingAddress
        CheckoutSessionResponse.TaxStatus.REQUIRES_BILLING_ADDRESS -> CheckoutSession.Tax.Status.RequiresBillingAddress
        CheckoutSessionResponse.TaxStatus.UNKNOWN -> CheckoutSession.Tax.Status.Unknown
    }
    return CheckoutSession.Tax(status = status)
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TotalSummaryResponse.toTotalSummary(): CheckoutSession.TotalSummary {
    return CheckoutSession.TotalSummary(
        subtotal = subtotal,
        totalDueToday = totalDueToday,
        totalAmountDue = totalAmountDue,
        discountAmounts = discountAmounts.map { it.toDiscountAmount() },
        taxAmounts = taxAmounts.map { it.toTaxAmount() },
        shippingRate = shippingRate?.toShippingRate(),
        appliedBalance = appliedBalance,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.DiscountAmount.toDiscountAmount(): CheckoutSession.DiscountAmount {
    return CheckoutSession.DiscountAmount(
        amount = amount,
        displayName = displayName,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.TaxAmount.toTaxAmount(): CheckoutSession.TaxAmount {
    return CheckoutSession.TaxAmount(
        amount = amount,
        inclusive = inclusive,
        displayName = displayName,
        percentage = percentage,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.ShippingRate.toShippingRate(): CheckoutSession.ShippingRate {
    return CheckoutSession.ShippingRate(
        id = id,
        amount = amount,
        displayName = displayName,
        deliveryEstimate = deliveryEstimate,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun CheckoutSessionResponse.LineItem.toLineItem(): CheckoutSession.LineItem {
    return CheckoutSession.LineItem(
        id = id,
        name = name,
        quantity = quantity,
        unitAmount = unitAmount,
        subtotal = subtotal,
        total = total,
    )
}
