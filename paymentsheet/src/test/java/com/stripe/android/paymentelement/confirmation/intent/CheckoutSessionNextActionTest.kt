package com.stripe.android.paymentelement.confirmation.intent

import android.os.Parcel
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class CheckoutSessionNextActionTest {
    @Test
    fun `successful authentication finalizes once with the original response`() = runScenario {
        val authenticatedIntent = PaymentIntentFactory.create(status = StripeIntent.Status.Succeeded)
        val result = definition.toResult(
            option,
            arguments,
            launcherArgs,
            InternalPaymentResult.Completed(authenticatedIntent),
        ) as ConfirmationDefinition.Result.NextStep
        val finalization = scope.async { definition.action(result.confirmationOption, result.arguments) }
        assertThat(pollCalls.awaitItem()).isEqualTo(response.id)
        assertThat(finalization.isCompleted).isFalse()
        pollRelease.complete(Unit)
        val action = finalization.await() as ConfirmationDefinition.Action.Complete
        assertThat(action.intent).isEqualTo(authenticatedIntent)
        val session = requireNotNull(action.metadata[CheckoutSessionResponseKey])
        assertThat(session.status).isEqualTo(CheckoutSessionResponse.Status.COMPLETE)
        assertThat(session.paymentStatus).isEqualTo(CheckoutSessionResponse.PaymentStatus.PAID)
        assertThat(session.customerEmail).isEqualTo(response.customerEmail)
        assertThat(session.checkoutItems).isEqualTo(response.checkoutItems)
    }

    @Test
    fun `authentication failure and cancellation carry the response without polling`(
        @TestParameter canceled: Boolean,
    ) = runScenario {
        val error = IllegalStateException("Authentication failed")
        val authResult = if (canceled) InternalPaymentResult.Canceled else InternalPaymentResult.Failed(error)
        when (val result = definition.toResult(option, arguments, launcherArgs, authResult)) {
            is ConfirmationDefinition.Result.Canceled -> {
                assertThat(result.metadata[CheckoutSessionResponseKey]).isSameInstanceAs(response)
                assertThat(result.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.InformCancellation)
            }
            is ConfirmationDefinition.Result.Failed -> {
                assertThat(result.metadata[CheckoutSessionResponseKey]).isSameInstanceAs(response)
                assertThat(result.cause).isSameInstanceAs(error)
            }
            else -> error("Unexpected result: $result")
        }
    }

    @Test
    fun `checkout launcher arguments retain the original response through parceling`() = runScenario {
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(launcherArgs, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val restored = parcel.readParcelable<IntentConfirmationDefinition.Args.CheckoutNextAction>(
                IntentConfirmationDefinition.Args.CheckoutNextAction::class.java.classLoader
            )
            assertThat(restored?.response).isEqualTo(response)
            assertThat(restored?.intent).isEqualTo(launcherArgs.intent)
            assertThat(restored?.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Server)
        } finally {
            parcel.recycle()
        }
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario(this)
        scenario.block()
        scenario.pollCalls.ensureAllEventsConsumed()
    }

    private class Scenario(val scope: kotlinx.coroutines.test.TestScope) {
        val pollCalls = Turbine<String>()
        val pollRelease = CompletableDeferred<Unit>()
        val response = CheckoutSessionResponseFactory.create(customerEmail = "server@example.com")
        val option = PaymentMethodConfirmationOption.New(
            createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            optionsParams = null,
            extraParams = null,
            shouldSave = false,
        )
        val arguments = ConfirmationHandler.Args(
            confirmationOption = option,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                integrationMetadata = IntegrationMetadata.CheckoutSession(response.id, "test", response),
            ),
            statusBarColor = null,
        )
        val launcherArgs = IntentConfirmationDefinition.Args.CheckoutNextAction(
            intent = PaymentIntentFactory.create(status = StripeIntent.Status.RequiresAction),
            response = response,
        )
        val definition = IntentConfirmationDefinition(
            intentConfirmationInterceptorFactory = object : IntentConfirmationInterceptor.Factory {
                override suspend fun create(
                    integrationMetadata: IntegrationMetadata,
                    customerMetadata: CustomerMetadata?,
                    clientAttributionMetadata: ClientAttributionMetadata,
                    isLiveMode: Boolean,
                ): IntentConfirmationInterceptor = error("Finalization must not repeat confirmation")
            },
            paymentLauncherFactory = { _, _, _ -> error("Finalization must not launch authentication again") },
            checkoutSessionFinalizer = CheckoutSessionConfirmationFinalizer(
                poller = CheckoutSessionPoller {
                    pollCalls.add(it)
                    pollRelease.await()
                    CheckoutSessionPoller.Outcome.COMPLETED
                },
                retrieveSession = { error("Unexpected retrieval") },
                genericErrorMessage = "Generic payment error",
            ),
        )
    }
}
