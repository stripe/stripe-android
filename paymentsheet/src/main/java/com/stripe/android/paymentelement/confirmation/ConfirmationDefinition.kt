package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType

/**
 * Defines a confirmation flow that a user might use during confirmation.
 */
internal interface ConfirmationDefinition<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    > {
    /**
     * Unique identifier for the definition. Used to order the definitions in same manner when registering launchers.
     */
    val key: String

    /**
     * Attempts to cast a given [ConfirmationHandler.Option] to the expected [TConfirmationOption] type of the
     * implemented definition. If successful, it will check if the option can be used for confirmation with
     * [canConfirm].
     * - If the generic option can be cast to the expected type, return the option
     * - else, return null
     *
     * @param confirmationOption the generic [ConfirmationHandler.Option] that may or may not be the expected option
     *   type.
     */
    fun option(
        confirmationOption: ConfirmationHandler.Option,
    ): TConfirmationOption?

    /**
     * After casting to the expected [ConfirmationHandler.Option] type, checks whether the provided option and
     * parameters can be used in order to confirm with this flow. It may be that certain parameters make confirming
     * with flow not possible (ie. a saved payment option is provided by CVC recollection is disabled).
     *
     * @param confirmationOption the expected [ConfirmationHandler.Option] type
     * @param confirmationArgs a set of general confirmation parameters
     */
    fun canConfirm(
        confirmationOption: TConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): Boolean = true

    /**
     * When the confirmation flow can be used after [option] returns the expected [ConfirmationHandler.Option] type
     * and [canConfirm] returns true, we know must decide what action to take with this confirmation flow. You may
     * decide to simply always use the same action here every time or decide based on the provided
     * [ConfirmationHandler.Option] and [ConfirmationHandler.Args] instances.
     *
     * @param confirmationOption the expected [ConfirmationHandler.Option] type
     * @param confirmationArgs a set of general confirmation parameters
     */
    suspend fun action(
        confirmationOption: TConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    ): Action<TLauncherArgs>

    /**
     * When the confirmation flow's action call returns a [Action.Launch], this is called in order to launch into the
     * confirmation flow.
     *
     * @param launcher a launcher that launches the confirmation flow defined in the definition
     * @param arguments a set of launcher arguments that need to be passed to the launcher's launch function
     * @param confirmationOption the expected [ConfirmationHandler.Option] type
     * @param confirmationArgs a set of general confirmation parameters
     */
    fun launch(
        launcher: TLauncher,
        arguments: TLauncherArgs,
        confirmationOption: TConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
    )

    /**
     * Creates the launcher used to launch into the primary confirmation flow defined by the definition.
     *
     * @param activityResultCaller caller used to create & register activity result launchers onto the Android
     *   lifecycle provider
     * @param onResult the launcher result callback to provide when registering the activity result launcher if needed
     */
    fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (TLauncherResult) -> Unit,
    ): TLauncher

    /**
     * Unregisters a launcher if needed when the Android lifecycle requires it.
     *
     * @param launcher the registered launcher instance
     */
    fun unregister(
        launcher: TLauncher,
    ) {}

    /**
     * After receiving a result from the activity, the launcher result is converted into a general confirmation result
     * that can be understood by the [ConfirmationHandler] consumer.
     *
     * @param confirmationOption the expected [ConfirmationHandler.Option] type used during confirmation
     * @param confirmationArgs a set of general confirmation parameters using during confirmation
     * @param deferredIntentConfirmationType DO NOT USE OUTSIDE OF INTENT CONFIRMATION
     * @param result the launcher result received after the confirmation flow was closed.
     */
    fun toResult(
        confirmationOption: TConfirmationOption,
        confirmationArgs: ConfirmationHandler.Args,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        isConfirmationToken: Boolean,
        result: TLauncherResult,
    ): Result

    fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {}

    /**
     * The general result a [ConfirmationDefinition] may return after launching into a confirmation flow
     */
    sealed interface Result {
        /**
         * Indicates that customer exited the confirmation flow manually without completing it.
         */
        data class Canceled(
            /**
             * The action that the consumer should take if the customer manually cancels.
             */
            val action: ConfirmationHandler.Result.Canceled.Action,
        ) : Result

        /**
         * Indicates that the customer has successfully completed the confirmation flow
         */
        data class Succeeded(
            /**
             * The [StripeIntent] that may or may not have been confirmed
             */
            val intent: StripeIntent,
            /**
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
            /**
             * Indicates if the result was from a confirmation token flow
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val isConfirmationToken: Boolean,
            /**
             * Indicates if the full payment flow was completed by the handler. Can be used to decide if internal
             * product state needs to be reset. Useful for confirmation flows that are handed off to merchants to
             * complete and require no action by Stripe flows afterwards.
             */
            val completedFullPaymentFlow: Boolean = true,
        ) : Result

        /**
         * Indicates that the customer has successfully completed the confirmation flow but requires another step
         * be taken in the whole confirmation process
         */
        data class NextStep(
            /**
             * A newly generated confirmation option that was created by the definition after receiving a result
             * from the launcher confirmation flow
             */
            val confirmationOption: ConfirmationHandler.Option,
            /**
             * A set of general confirmation parameters. Should normally be the same as what was provided by the
             * user
             */
            val arguments: ConfirmationHandler.Args,
        ) : Result

        /**
         * Indicates that the customer has encountered an error during the confirmation flow and the flow could not be
         * successfully completed because of it.
         */
        data class Failed(
            /**
             * The error that occurred during the confirmation flow run.
             */
            val cause: Throwable,
            /**
             * A customer-friendly message to show when the flow fails.
             */
            val message: ResolvableString,
            /**
             * The error type that occurred during the confirmation flow run.
             */
            val type: ConfirmationHandler.Result.Failed.ErrorType,
        ) : Result
    }

    /**
     * The possible actions that can be taken by the confirmation flow.
     */
    sealed interface Action<TLauncherArgs> {
        /**
         * A complete action indicating that the confirmation flow was able to be completed when determining on an
         * action. Returning this action will avoid launching the confirmation flow and complete the confirmation
         * process.
         */
        data class Complete<TLauncherArgs>(
            /**
             * The [StripeIntent] that may or may not have been confirmed
             */
            val intent: StripeIntent,
            /**
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
            /**
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val isConfirmationToken: Boolean,
            /**
             * Indicates if the full payment flow was completed by the handler. Can be used to decide if internal
             * product state needs to be reset. Useful for confirmation flows that are handed off to merchants to
             * complete and require no action by Stripe flows afterwards.
             */
            val completedFullPaymentFlow: Boolean,
        ) : Action<TLauncherArgs>

        /**
         * A failed action indicating that the confirmation flow encountered an error when determining an action.
         */
        data class Fail<TLauncherArgs>(
            /**
             * The error that occurred when determining what action the confirmation flow could take.
             */
            val cause: Throwable,
            /**
             * A customer-friendly message to show when the flow fails.
             */
            val message: ResolvableString,
            /**
             * The error type that occurred during the confirmation flow run.
             */
            val errorType: ConfirmationHandler.Result.Failed.ErrorType,
        ) : Action<TLauncherArgs>

        /**
         * A launch action indicating that the definition has determined the need to launch into the primary
         * activity flow in order to complete the confirmation flow.
         */
        data class Launch<TLauncherArgs>(
            /**
             * A set of launcher arguments required by the [TLauncher] instance in order to properly launch into the
             * the primary confirmation activity flow.
             */
            val launcherArguments: TLauncherArgs,
            /**
             * Indicates if the result is received within the process of the application. This is used during process
             * death to decide if we should continue to wait for a result after re-initializing the confirmation
             * state.
             * - This should be `false` if the launched confirmation activity flow is a full-screen activity that
             * covers the merchant's application (ie. Link native/web flow)
             * - This should be `true` if the launched confirmation activity flow does not cover or only partially
             * covers the merchant's application (ie. the Google Pay or Bacs Mandate sheets).
             */
            val receivesResultInProcess: Boolean,
            /**
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
            /**
             * DO NOT USE OUTSIDE OF INTENT CONFIRMATION
             */
            val isConfirmationToken: Boolean,
        ) : Action<TLauncherArgs>
    }
}
