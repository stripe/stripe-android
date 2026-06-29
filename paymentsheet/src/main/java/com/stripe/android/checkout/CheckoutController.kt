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
import kotlinx.parcelize.Parcelize

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions", "UnusedParameter")
class CheckoutController(
    application: Application,
    savedStateHandle: SavedStateHandle,
    resultCallback: ResultCallback,
) {
    val checkoutSession: StateFlow<CheckoutSession?> =
        MutableStateFlow<CheckoutSession?>(null).asStateFlow()

    val isLoading: StateFlow<Boolean> =
        MutableStateFlow(false).asStateFlow()

    val paymentOption: StateFlow<PaymentElement.PaymentOptionDisplayData?> =
        MutableStateFlow<PaymentElement.PaymentOptionDisplayData?>(null).asStateFlow()

    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun applyPromotionCode(promotionCode: String): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun removePromotionCode(): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun updateLineItemQuantity(lineItemId: String, quantity: Int): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun selectShippingOption(id: String): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun updateShippingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun updateTaxId(type: String, value: String): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun updateBillingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    suspend fun runServerUpdate(serverUpdate: suspend () -> kotlin.Result<Unit>): kotlin.Result<Unit> {
        TODO("Not yet implemented")
    }

    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        TODO("Not yet implemented")
    }

    fun destroy() {
        TODO("Not yet implemented")
    }

    fun clearPaymentOption() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false
        private var paymentElementConfiguration: PaymentElement.Configuration = PaymentElement.Configuration()
        private var currencySelectorElementConfiguration: CurrencySelectorElement.Configuration =
            CurrencySelectorElement.Configuration()
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

        fun currencySelectorElement(
            configuration: CurrencySelectorElement.Configuration
        ): Configuration = apply {
            this.currencySelectorElementConfiguration = configuration
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
            val currencySelectorElementConfiguration: CurrencySelectorElement.Configuration.State,
            val shippingAddressElementConfiguration: ShippingAddressElement.Configuration.State,
        ) : Parcelable

        internal fun build(): State = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
            paymentElementConfiguration = paymentElementConfiguration.build(),
            currencySelectorElementConfiguration = currencySelectorElementConfiguration.build(),
            shippingAddressElementConfiguration = shippingAddressElementConfiguration.build(),
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Completed internal constructor() : Result

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Canceled internal constructor() : Result

        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Failed internal constructor(val error: Throwable) : Result
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultCallback {
        fun onResult(result: Result)
    }
}
