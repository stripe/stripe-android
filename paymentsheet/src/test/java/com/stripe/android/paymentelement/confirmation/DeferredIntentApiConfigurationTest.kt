package com.stripe.android.paymentelement.confirmation

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.ConfirmationTokenJsonParser
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import javax.inject.Provider

@OptIn(com.stripe.android.SharedPaymentTokenSessionPreview::class)
@RunWith(RobolectricTestParameterInjector::class)
internal class DeferredIntentApiConfigurationTest {
    @Suppress("LongMethod")
    @Test
    fun `post callback requests and launcher use snapshot`(
        @TestParameter tokenCallback: Boolean,
        @TestParameter saved: Boolean,
        @TestParameter setup: Boolean,
        @TestParameter override: Override,
        @TestParameter outcome: Outcome,
    ) = runTest {
        val original = ApiConfiguration.State("pk_test_original", "acct_original")
        val builder = ApiConfiguration("pk_test_override").stripeAccountId(
            "acct_override".takeIf { override == Override.Replace }
        )
        val expected = if (override == Override.Absent) original else builder.build()
        val intent = intent(setup, outcome)
        val repository = RecordingRepository(intent, outcome == Outcome.RetrievalFailure)
        val callbacks = Turbine<Unit>()
        val interceptor = createIntentConfirmationInterceptor(
            integrationMetadata = integration(tokenCallback, setup),
            stripeRepository = repository,
            publishableKeyProvider = { original.publishableKey },
            stripeAccountId = original.stripeAccountId,
            intentCreationCallbackProvider = Provider {
                CreateIntentCallback { _, _ -> callbacks.add(Unit); result(intent, override, builder, outcome) }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback {
                    callbacks.add(Unit)
                    result(intent, override, builder, outcome)
                }
            },
        )
        val action = when (val option = option(saved)) {
            is PaymentMethodConfirmationOption.New -> interceptor.intercept(intent, option, null)
            is PaymentMethodConfirmationOption.Saved -> interceptor.intercept(intent, option, null)
        }
        if (tokenCallback || !saved) {
            val preparation = repository.preparation.awaitItem()
            assertThat(preparation.apiKey).isEqualTo(original.publishableKey)
            assertThat(preparation.stripeAccount).isEqualTo(original.stripeAccountId)
        }
        callbacks.awaitItem()
        if (outcome != Outcome.CallbackFailure && outcome != Outcome.Sentinel) {
            val retrieval = repository.retrieval.awaitItem()
            assertThat(retrieval.apiKey).isEqualTo(expected.publishableKey)
            assertThat(retrieval.stripeAccount).isEqualTo(expected.stripeAccountId)
        }
        when (outcome) {
            Outcome.Client, Outcome.ServerAction -> {
                val args = (action as ConfirmationDefinition.Action.Launch).launcherArguments
                assertThat(args.apiConfiguration).isEqualTo(expected)
                assertThat(args is IntentConfirmationDefinition.Args.Confirm).isEqualTo(outcome == Outcome.Client)
            }
            Outcome.Completed, Outcome.Sentinel -> assertThat(action).isInstanceOf(
                ConfirmationDefinition.Action.Complete::class.java
            )
            Outcome.CallbackFailure, Outcome.RetrievalFailure -> assertThat(action).isInstanceOf(
                ConfirmationDefinition.Action.Fail::class.java
            )
        }
        repository.ensureConsumed()
        callbacks.ensureAllEventsConsumed()
    }

    @Test
    fun `invalid override fails without sending retrieval`(
        @TestParameter tokenCallback: Boolean,
    ) = runTest {
        val intent = intent(false, Outcome.Client)
        val repository = RecordingRepository(intent, false)
        val callbacks = Turbine<Unit>()
        val interceptor = createIntentConfirmationInterceptor(
            integrationMetadata = integration(tokenCallback, false),
            stripeRepository = repository,
            intentCreationCallbackProvider = Provider {
                CreateIntentCallback { _, _ ->
                    callbacks.add(Unit)
                    CreateIntentResult.Success(requireNotNull(intent.clientSecret))
                        .apiConfiguration(ApiConfiguration(""))
                }
            },
            intentCreationConfirmationTokenCallbackProvider = Provider {
                CreateIntentWithConfirmationTokenCallback {
                    callbacks.add(Unit)
                    CreateIntentResult.Success(requireNotNull(intent.clientSecret))
                        .apiConfiguration(ApiConfiguration(""))
                }
            },
        )
        val action = interceptor.intercept(intent, option(false) as PaymentMethodConfirmationOption.New, null)
        assertThat(action).isInstanceOf(ConfirmationDefinition.Action.Fail::class.java)
        callbacks.awaitItem()
        callbacks.ensureAllEventsConsumed()
        repository.preparation.awaitItem()
        repository.retrieval.expectNoEvents()
        repository.ensureConsumed()
    }

    @Test
    fun `configuration snapshots are isolated between successes`() {
        val builder = ApiConfiguration("pk_test_one").stripeAccountId("acct_one")
        val first = CreateIntentResult.Success("secret").apiConfiguration(builder)
        builder.stripeAccountId("acct_two")
        val second = CreateIntentResult.Success("secret").apiConfiguration(builder)
        val absent = CreateIntentResult.Success("secret")
        builder.stripeAccountId(null)
        assertThat(first.apiConfiguration?.stripeAccountId).isEqualTo("acct_one")
        assertThat(second.apiConfiguration?.stripeAccountId).isEqualTo("acct_two")
        assertThat(absent.apiConfiguration).isNull()
    }

    private fun result(
        intent: StripeIntent,
        override: Override,
        builder: ApiConfiguration,
        outcome: Outcome,
    ): CreateIntentResult {
        if (outcome == Outcome.CallbackFailure) return CreateIntentResult.Failure(IllegalStateException("callback"))
        return CreateIntentResult.Success(
            if (outcome == Outcome.Sentinel) {
                IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT
            } else {
                requireNotNull(intent.clientSecret)
            }
        ).also {
            if (override != Override.Absent) it.apiConfiguration(builder)
            builder.stripeAccountId("acct_mutated_after_snapshot")
        }
    }

    private fun integration(token: Boolean, setup: Boolean): IntegrationMetadata {
        val configuration = PaymentSheet.IntentConfiguration(
            mode = if (setup) {
                PaymentSheet.IntentConfiguration.Mode.Setup(currency = "usd")
            } else {
                PaymentSheet.IntentConfiguration.Mode.Payment(amount = 1099, currency = "usd")
            },
        )
        return if (token) {
            IntegrationMetadata.DeferredIntent.WithConfirmationToken(configuration)
        } else {
            IntegrationMetadata.DeferredIntent.WithPaymentMethod(configuration)
        }
    }

    private fun intent(setup: Boolean, outcome: Outcome): StripeIntent {
        val status = when (outcome) {
            Outcome.ServerAction -> StripeIntent.Status.RequiresAction
            Outcome.Completed -> StripeIntent.Status.Succeeded
            else -> StripeIntent.Status.RequiresConfirmation
        }
        return if (setup) {
            SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
            status = status,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        } else {
            PaymentIntentFixtures.PI_SUCCEEDED.copy(
            status = status,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        )
        }
    }

    private fun option(saved: Boolean): PaymentMethodConfirmationOption = if (saved) {
        PaymentMethodConfirmationOption.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(customerId = null),
            optionsParams = null,
            shippingInformation = null,
        )
    } else {
        PaymentMethodConfirmationOption.New(PaymentMethodCreateParamsFixtures.DEFAULT_CARD, null, null, false)
    }

    private class RecordingRepository(
        val intent: StripeIntent,
        val failRetrieval: Boolean,
    ) : AbsFakeStripeRepository() {
        val preparation = Turbine<ApiRequest.Options>()
        val retrieval = Turbine<ApiRequest.Options>()

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options,
        ): Result<PaymentMethod> {
            preparation.add(options)
            return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        }

        override suspend fun createConfirmationToken(
            confirmationTokenParams: ConfirmationTokenParams,
            options: ApiRequest.Options,
        ): Result<ConfirmationToken> {
            preparation.add(options)
            return Result.success(
                requireNotNull(
                ConfirmationTokenJsonParser().parse(ConfirmationTokenFixtures.CONFIRMATION_TOKEN_JSON)
            )
            )
        }

        override suspend fun retrieveStripeIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>,
        ): Result<StripeIntent> {
            retrieval.add(options)
            return if (failRetrieval) Result.failure(IllegalStateException("retrieval")) else Result.success(intent)
        }

        fun ensureConsumed() {
            preparation.ensureAllEventsConsumed()
            retrieval.ensureAllEventsConsumed()
        }
    }

    enum class Override { Absent, Replace, Clear }
    enum class Outcome { Client, ServerAction, Completed, CallbackFailure, RetrievalFailure, Sentinel }
}
