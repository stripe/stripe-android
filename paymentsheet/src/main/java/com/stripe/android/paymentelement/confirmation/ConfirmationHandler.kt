package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize

/**
 * This interface handles the confirmation process of a [StripeIntent] and/or external payment. This interface is
 * intended to run only one confirmation process at a time.
 */
internal interface ConfirmationHandler {
    /**
     * Indicates if this handler has been reloaded from process death. This occurs if the handler was confirming
     * an intent before did not complete the process before process death.
     */
    val hasReloadedFromProcessDeath: Boolean

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
     * Starts the confirmation process. Results can be received through [state] or through [awaitResult].
     *
     * @param arguments required set of arguments in order to start the confirmation process
     */
    fun start(arguments: Args)

    /**
     * Awaits for the result of a started confirmation process.
     *
     * @return confirmation result or null if no confirmation process has been started
     */
    suspend fun awaitResult(): Result?

    /**
     * A factory for creating a [ConfirmationHandler] instance using a provided [CoroutineScope]. This scope is
     * used to launch confirmation tasks.
     */
    fun interface Factory {
        fun create(scope: CoroutineScope): ConfirmationHandler
    }

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
        val confirmationOption: Option,

        /**
         * Appearance values to be used when styling the launched activities
         */
        val appearance: PaymentSheet.Appearance,

        /**
         * The mode that a Payment Element product was initialized with
         */
        val initializationMode: PaymentElementLoader.InitializationMode,

        /**
         * The shipping details of the customer that can be attached during the confirmation flow
         */
        val shippingDetails: AddressDetails?
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
         * Indicates the the handler is currently confirming.
         */
        data class Confirming(
            val option: Option,
        ) : State

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
    @Parcelize
    sealed interface Result : Parcelable {
        /**
         * Indicates that the confirmation process was canceled by the customer.
         */
        @Parcelize
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
        @Parcelize
        data class Succeeded(
            val intent: StripeIntent,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Result

        /**
         * Indicates that the confirmation process has failed. A cause and potentially a resolvable message are
         * returned as part of the result.
         */
        @Parcelize
        data class Failed(
            val cause: Throwable,
            val message: ResolvableString,
            val type: ErrorType,
        ) : Result {
            /**
             * Types of errors that can occur when confirming a payment.
             */
            @Parcelize
            sealed interface ErrorType : Parcelable {
                /**
                 * Fatal confirmation error that occurred while confirming a payment. This should never happen.
                 */
                @Parcelize
                data object Fatal : ErrorType

                /**
                 * Indicates an error when processing a payment during the confirmation process.
                 */
                @Parcelize
                data object Payment : ErrorType

                /**
                 * Indicates an internal process error occurred during the confirmation process.
                 */
                @Parcelize
                data object Internal : ErrorType

                /**
                 * Indicates a merchant integration error occurred during the confirmation process.
                 */
                @Parcelize
                data object MerchantIntegration : ErrorType

                /**
                 * Indicates an error occurred when confirming with external payment methods
                 */
                @Parcelize
                data object ExternalPaymentMethod : ErrorType

                /**
                 * Indicates an error occurred when confirming with Google Pay
                 */
                @Parcelize
                data class GooglePay(val errorCode: Int) : ErrorType
            }
        }
    }

    interface Option : Parcelable
}
