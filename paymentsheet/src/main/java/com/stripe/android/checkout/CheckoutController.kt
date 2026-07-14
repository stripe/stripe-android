package com.stripe.android.checkout

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions", "UnusedParameter")
class CheckoutController @Inject internal constructor(
    resultCallback: ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val checkoutStateLoader: CheckoutStateLoader,
    private val presenterFactory: CheckoutPresenter.Factory,
    internal val stateHolder: CheckoutControllerStateHolder,
    internal val confirmationStateHolder: CheckoutConfirmationStateHolder,
) {
    val checkoutSession: StateFlow<CheckoutSession?>
        get() = stateHolder.checkoutSession

    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): kotlin.Result<Unit> {
        val configurationState = configuration.build()
        val sessionId = checkoutSessionClientSecret.substringBefore("_secret_")

        return checkoutSessionRepository.init(
            sessionId = sessionId,
            adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
        ).mapCatching { response ->
            checkoutStateLoader.load(
                CheckoutControllerState.defaultState(
                    configuration = configurationState,
                    checkoutSessionResponse = response,
                )
            )
        }
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

    @Suppress("UNUSED_PARAMETER")
    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        return presenterFactory.create(this)
    }

    fun destroy() {
        viewModelScope.cancel()
    }

    fun clearPaymentOption() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
    ) {
        private var resultCallback: ResultCallback = ResultCallback {}

        fun resultCallback(
            resultCallback: ResultCallback
        ): Builder = apply {
            this.resultCallback = resultCallback
        }

        fun build(): CheckoutController {
            val component = DaggerCheckoutControllerComponent.factory().create(
                application = application,
                savedStateHandle = savedStateHandle,
                resultCallback = resultCallback,
            )

            return component.checkoutController
        }
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false
        private var defaultBillingDetails: PaymentSheet.BillingDetails? = null
        private var googlePay: PaymentSheet.GooglePayConfiguration? = null
        private var paymentElementConfiguration: PaymentElement.Configuration = PaymentElement.Configuration()
        private var currencySelectorElementConfiguration: CurrencySelectorElement.Configuration =
            CurrencySelectorElement.Configuration()
        private var shippingAddressElementConfiguration: ShippingAddressElement.Configuration =
            ShippingAddressElement.Configuration()
        private var expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration =
            ExpressCheckoutElement.Configuration()

        fun adaptivePricingAllowed(
            adaptivePricingAllowed: Boolean
        ): Configuration = apply {
            this.adaptivePricingAllowed = adaptivePricingAllowed
        }

        fun defaultBillingDetails(
            defaultBillingDetails: PaymentSheet.BillingDetails?
        ): Configuration = apply {
            this.defaultBillingDetails = defaultBillingDetails
        }

        fun googlePay(
            googlePay: PaymentSheet.GooglePayConfiguration?
        ): Configuration = apply {
            this.googlePay = googlePay
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

        fun expressCheckoutElement(
            configuration: ExpressCheckoutElement.Configuration
        ): Configuration = apply {
            this.expressCheckoutElementConfiguration = configuration
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
            val defaultBillingDetails: PaymentSheet.BillingDetails?,
            val googlePay: PaymentSheet.GooglePayConfiguration?,
            val paymentElementConfiguration: PaymentElement.Configuration.State,
            val currencySelectorElementConfiguration: CurrencySelectorElement.Configuration.State,
            val shippingAddressElementConfiguration: ShippingAddressElement.Configuration.State,
            val expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        ) : Parcelable

        internal fun build(): State = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
            defaultBillingDetails = defaultBillingDetails,
            googlePay = googlePay,
            paymentElementConfiguration = paymentElementConfiguration.build(),
            currencySelectorElementConfiguration = currencySelectorElementConfiguration.build(),
            shippingAddressElementConfiguration = shippingAddressElementConfiguration.build(),
            expressCheckoutElementConfiguration = expressCheckoutElementConfiguration.build(),
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
