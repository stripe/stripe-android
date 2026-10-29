package com.stripe.android.paymentelement.confirmation.interceptor

import com.google.common.truth.Truth.assertThat
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.RadarOptions
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.createIntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationDefinition
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.RadarOptionsFactory
import kotlinx.coroutines.test.runTest
import javax.inject.Provider

@OptIn(SharedPaymentTokenSessionPreview::class)
internal data class InterceptorTestScenario(
    val customerMetadata: CustomerMetadata? = null,
    val stripeRepository: StripeRepository = object : AbsFakeStripeRepository() {},
    val publishableKeyProvider: () -> String = { "pk" },
    val errorReporter: ErrorReporter = FakeErrorReporter(),
    val intentCreationCallbackProvider: Provider<CreateIntentCallback?> = Provider { null },
    val intentCreationConfirmationTokenCallbackProvider: Provider<CreateIntentWithConfirmationTokenCallback?> =
        Provider { null },
    val preparePaymentMethodHandlerProvider: Provider<PreparePaymentMethodHandler?> = Provider { null }
)

@OptIn(SharedPaymentTokenSessionPreview::class)
internal fun runInterceptorScenario(
    integrationMetadata: IntegrationMetadata,
    scenario: InterceptorTestScenario = InterceptorTestScenario(),
    test: suspend (interceptor: IntentConfirmationInterceptor) -> Unit
) = runTest {
    val interceptor = createIntentConfirmationInterceptor(
        integrationMetadata = integrationMetadata,
        customerMetadata = scenario.customerMetadata,
        stripeRepository = scenario.stripeRepository,
        publishableKeyProvider = scenario.publishableKeyProvider,
        errorReporter = scenario.errorReporter,
        intentCreationCallbackProvider = scenario.intentCreationCallbackProvider,
        intentCreationConfirmationTokenCallbackProvider = scenario.intentCreationConfirmationTokenCallbackProvider,
        preparePaymentMethodHandlerProvider = scenario.preparePaymentMethodHandlerProvider,
    )
    test(interceptor)
}

internal suspend fun IntentConfirmationInterceptor.interceptDefaultNewPaymentMethod():
    ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = intercept(
    intent = PaymentIntentFactory.create(),
    confirmationOption = PaymentMethodConfirmationOption.New(
        createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        optionsParams = null,
        extraParams = null,
        shouldSave = false,
    ),
    shippingValues = null,
    )

internal suspend fun IntentConfirmationInterceptor.interceptDefaultSavedPaymentMethod():
    ConfirmationDefinition.Action<IntentConfirmationDefinition.Args> = intercept(
    intent = PaymentIntentFactory.create(),
    confirmationOption = PaymentMethodConfirmationOption.Saved(
        shippingInformation = null,
        paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
        optionsParams = null,
    ),
    shippingValues = null,
    )

@Suppress("UNCHECKED_CAST")
internal fun <T : ConfirmStripeIntentParams>
    ConfirmationDefinition.Action<IntentConfirmationDefinition.Args>.asConfirmParams(): T? {
    return (
        (this as? ConfirmationDefinition.Action.Launch)
            ?.launcherArguments as? IntentConfirmationDefinition.Args.Confirm
    ) ?.confirmNextParams as? T
}

internal fun ConfirmStripeIntentParams.radarOptions(): RadarOptions? {
    return when (this) {
        is ConfirmPaymentIntentParams -> radarOptions
        is ConfirmSetupIntentParams -> radarOptions
    }
}

internal fun stripeRepositoryReturning(
    onCreatePaymentMethodId: String,
    onRetrievePaymentMethodId: String,
): StripeRepository {
    return object : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: RequestCredentials,
            idempotencyKey: String?,
        ): Result<PaymentMethod> {
            return Result.success(
                PaymentMethodFactory.card(random = true).copy(
                    id = onCreatePaymentMethodId
                )
            )
        }

        override suspend fun retrieveStripeIntent(
            clientSecret: String,
            options: RequestCredentials,
            expandFields: List<String>,
            idempotencyKey: String?,
        ): Result<StripeIntent> {
            return Result.success(
                PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.copy(
                    paymentMethodId = onRetrievePaymentMethodId
                )
            )
        }
    }
}

internal fun assertRadarOptionsEquals(
    confirmParams: ConfirmStripeIntentParams?,
    expectedRadarOptions: RadarOptions
) {
    assertThat(confirmParams?.radarOptions()).isEqualTo(expectedRadarOptions)
}

internal fun assertRadarOptionsEquals(confirmParams: ConfirmStripeIntentParams?, expectedToken: String) {
    assertRadarOptionsEquals(
        confirmParams = confirmParams,
        expectedRadarOptions = RadarOptionsFactory.create(
            hCaptchaToken = expectedToken,
            verificationObject = null
        )
    )
}

internal fun assertRadarOptionsIsNull(confirmParams: ConfirmStripeIntentParams?) {
    assertThat(confirmParams?.radarOptions()).isEqualTo(
        RadarOptionsFactory.create(
            hCaptchaToken = null,
            verificationObject = null
        )
    )
}
