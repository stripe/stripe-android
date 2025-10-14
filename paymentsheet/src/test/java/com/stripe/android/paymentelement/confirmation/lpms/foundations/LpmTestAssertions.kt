package com.stripe.android.paymentelement.confirmation.lpms.foundations

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.paymentsheet.PaymentSheet
import kotlin.test.fail

internal suspend fun assertIntentConfirmed(
    activity: LpmNetworkTestActivity,
    params: LpmAssertionParams
): Intent {
    intendingPaymentConfirmationToBeLaunched(params.intent)

    val option = PaymentMethodConfirmationOption.New(
        createParams = params.createParams,
        optionsParams = params.optionsParams,
        extraParams = params.extraParams,
        shouldSave = params.customerRequestedSave,
        passiveCaptchaParams = null,
        attestationRequired = false,
    )

    activity.confirmationHandler.state.test {
        awaitItem().assertIdle()

        activity.confirmationHandler.start(
            ConfirmationHandler.Args(
                confirmationOption = option,
                intent = params.intent,
                initializationMode = params.initializationMode,
                appearance = PaymentSheet.Appearance.Builder().build(),
                shippingDetails = params.shippingDetails,
            )
        )

        val confirming = awaitItem().assertConfirming()

        assertThat(confirming.option).isEqualTo(option)

        intendedPaymentConfirmationToBeLaunched()

        val complete = awaitItem().assertComplete()

        val successResult = complete.result.assertSucceeded()

        assertThat(successResult.intent).isEqualTo(params.intent)

        ensureAllEventsConsumed()
    }

    val recordedIntent = getIntents().first()

    recordedIntent.assertPaymentLauncherName()

    val arguments = recordedIntent.assertPaymentLauncherArgs()

    assertConfirmed(activity, arguments.confirmStripeIntentParams)

    return recordedIntent
}

private fun intendingPaymentConfirmationToBeLaunched(
    intent: StripeIntent,
) {
    intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
        Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            Intent().putExtras(bundleOf("extra_args" to InternalPaymentResult.Completed(intent)))
        )
    )
}

private fun intendedPaymentConfirmationToBeLaunched() {
    intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
}

private fun Intent.assertPaymentLauncherName() {
    assertThat(component?.className).isEqualTo(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)
}

private fun Intent.assertPaymentLauncherArgs(): PaymentLauncherContract.Args.IntentConfirmationArgs {
    @Suppress("DEPRECATION")
    val arguments = extras?.getParcelable<PaymentLauncherContract.Args.IntentConfirmationArgs>("extra_args")

    assertThat(arguments).isNotNull()

    return requireNotNull(arguments)
}

private suspend fun assertConfirmed(
    activity: LpmNetworkTestActivity,
    confirmParams: ConfirmStripeIntentParams
) {
    val result = when (confirmParams) {
        is ConfirmSetupIntentParams -> {
            activity.testClient.confirmSetupIntent(
                confirmParams = confirmParams,
            )
        }
        is ConfirmPaymentIntentParams -> {
            activity.testClient.confirmPaymentIntent(
                confirmParams = confirmParams,
            )
        }
    }

    result.onSuccess { intent ->
        /*
         * Our intent should either be confirmed state or in a state where an action is required. In either case,
         * this indicates to us that the Stripe API accept the provided intent confirmation parameters and
         * therefore can successfully confirm using the provided parameters
         */
        if (!intent.isConfirmed && !intent.requiresAction()) {
            fail(
                intent.lastErrorMessage
                    ?: "Intent was neither confirmed nor requires next action due to unknown error!"
            )
        }
    }.onFailure { failure ->
        fail(failure.message, failure)
    }
}

private const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
    "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
