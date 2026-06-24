package com.stripe.android.checkout

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class CheckoutController(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val resultCallback: ResultCallback,
) {

    private val mutex = Mutex()

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
        // TODO: Fetch session via repository, populate _checkoutSession and other StateFlows.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun applyPromotionCode(promotionCode: String): kotlin.Result<Unit> {
        // TODO: Apply promotion code via repository, update session.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun removePromotionCode(): kotlin.Result<Unit> {
        // TODO: Remove promotion code via repository, update session.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun updateLineItemQuantity(lineItemId: String, quantity: Int): kotlin.Result<Unit> {
        // TODO: Update line item quantity via repository, update session.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun selectShippingOption(id: String): kotlin.Result<Unit> {
        // TODO: Select shipping option via repository, update session.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun updateShippingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        // TODO: Update shipping address via repository, recalculate taxes.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun updateTaxId(type: String, value: String): kotlin.Result<Unit> {
        // TODO: Update tax ID via repository, update session.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun updateBillingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        // TODO: Update billing address via repository, recalculate taxes.
        return kotlin.Result.failure(NotImplementedError())
    }

    suspend fun runServerUpdate(serverUpdate: suspend () -> kotlin.Result<Unit>): kotlin.Result<Unit> {
        // TODO: Run serverUpdate with timeout, then refresh session from server.
        return kotlin.Result.failure(NotImplementedError())
    }

    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        return CheckoutPresenter(activity, this)
    }

    fun destroy() {
        // TODO: Clean up resources, cancel scopes, remove from instance tracking.
    }

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
