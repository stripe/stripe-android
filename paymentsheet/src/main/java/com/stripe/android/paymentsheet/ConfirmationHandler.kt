package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import com.stripe.android.model.PaymentMethod as PaymentMethodModel

/**
 * This interface handles the confirmation process of a [StripeIntent] and/or external payment. This interface is
 * intended to run only one confirmation process at a time.
 */
internal interface ConfirmationHandler {
    /**
     * An observable indicating the current confirmation state of the handler.
     */
    val state: StateFlow<State>

    /**
     * Registers all internal confirmation sub-handlers onto the given lifecycle owner.
     *
     * @param activityResultCaller a caller class that can start confirmation activity flows
     * @param lifecycleOwner The owner of an observable lifecycle to attach the handlers to
     */
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)

    /**
     * Starts the confirmation process. Results can be received through [state] or through [awaitIntentResult].
     *
     * @param arguments required set of arguments in order to start the confirmation process
     */
    fun start(arguments: Args)

    /**
     * Awaits for the result of a started confirmation process.
     *
     * @return confirmation result or null if no confirmation process has been started
     */
    suspend fun awaitIntentResult(): Result?

    /**
     * Defines the set of arguments requires for beginning the confirmation process
     */
    @Parcelize
    data class Args(
        /**
         * The [StripeIntent] that is being potentially confirmed by the handler
         */
        val intent: StripeIntent,

        /**
         * The confirmation option used to in order to potentially confirm the intent
         */
        val confirmationOption: Option
    ) : Parcelable

    /**
     * Defines the state types that [ConfirmationHandler] can be in with regards to confirmation.
     */
    sealed interface State {
        /**
         * Indicates that the handler is currently idle.
         */
        data object Idle : State

        /**
         * Indicates the the handler is currently performing pre-confirmation steps before starting confirmation.
         */
        data class Preconfirming(
            val confirmationOption: Option?,
            val inPreconfirmFlow: Boolean,
        ) : State

        /**
         * Indicates the the handler is currently confirming.
         */
        data object Confirming : State

        /**
         * Indicates that the handler has completed confirming and contains a [Result] regarding the confirmation
         * process final result.
         */
        data class Complete(
            val result: Result,
        ) : State
    }

    /**
     * Defines the result types that can be returned after completing a confirmation process.
     */
    sealed interface Result {
        /**
         * Indicates that the confirmation process was canceled by the customer.
         */
        data class Canceled(
            val action: Action,
        ) : Result {
            /**
             * Action to perform if a user cancels a running confirmation process.
             */
            enum class Action {
                /**
                 * This actions means the user has cancels a critical confirmation step and that the user should
                 * be notified of the cancellation if relevant.
                 */
                InformCancellation,

                /**
                 * This action means that the user has asked to modify the payment details of their selected
                 * payment option.
                 */
                ModifyPaymentDetails,

                /**
                 * Means no action should be taken if the user cancels a step in the confirmation process.
                 */
                None,
            }
        }

        /**
         * Indicates that the confirmation process has been successfully completed. A [StripeIntent] with an updated
         * state is returned as part of the result as well.
         */
        data class Succeeded(
            val intent: StripeIntent,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Result

        /**
         * Indicates that the confirmation process has failed. A cause and potentially a resolvable message are
         * returned as part of the result.
         */
        data class Failed(
            val cause: Throwable,
            val message: ResolvableString,
            val type: PaymentConfirmationErrorType,
        ) : Result
    }

    sealed interface Option : Parcelable {
        @Parcelize
        data class GooglePay(
            val initializationMode: PaymentElementLoader.InitializationMode,
            val shippingDetails: AddressDetails?,
            val config: Config,
        ) : Option {
            @Parcelize
            data class Config(
                val environment: PaymentSheet.GooglePayConfiguration.Environment?,
                val merchantName: String,
                val merchantCountryCode: String,
                val merchantCurrencyCode: String?,
                val customAmount: Long?,
                val customLabel: String?,
                val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
                val cardBrandFilter: CardBrandFilter
            ) : Parcelable
        }

        @Parcelize
        data class ExternalPaymentMethod(
            val type: String,
            val billingDetails: PaymentMethodModel.BillingDetails?,
        ) : Option

        @Parcelize
        data class BacsPaymentMethod(
            val initializationMode: PaymentElementLoader.InitializationMode,
            val shippingDetails: AddressDetails?,
            val createParams: PaymentMethodCreateParams,
            val optionsParams: PaymentMethodOptionsParams?,
            val appearance: PaymentSheet.Appearance,
        ) : Option

        sealed interface PaymentMethod : Option {
            val initializationMode: PaymentElementLoader.InitializationMode
            val shippingDetails: AddressDetails?

            @Parcelize
            data class Saved(
                override val initializationMode: PaymentElementLoader.InitializationMode,
                override val shippingDetails: AddressDetails?,
                val paymentMethod: com.stripe.android.model.PaymentMethod,
                val optionsParams: PaymentMethodOptionsParams?,
            ) : PaymentMethod

            @Parcelize
            data class New(
                override val initializationMode: PaymentElementLoader.InitializationMode,
                override val shippingDetails: AddressDetails?,
                val createParams: PaymentMethodCreateParams,
                val optionsParams: PaymentMethodOptionsParams?,
                val shouldSave: Boolean
            ) : PaymentMethod
        }
    }
}
