package com.stripe.android.paymentelement.confirmation.intent

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiConfiguration
import com.stripe.android.ApiConfigurationPreview
import com.stripe.android.PaymentConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ApiConfigurationPreview::class)
internal class IntentConfirmationModuleTest {

    @Test
    fun `publishableKey lambda uses apiConfiguration when state has one`() = runTest {
        val confirmationStateHolder = createStateHolder()
        confirmationStateHolder.state = stateWithApiConfig(
            publishableKey = "pk_test_api_config_key",
        )

        val publishableKey = capturePublishableKeyLambda(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = "pk_test_fallback_key",
        )

        assertThat(publishableKey()).isEqualTo("pk_test_api_config_key")
    }

    @Test
    fun `publishableKey lambda falls back to PaymentConfiguration when state is null`() = runTest {
        val confirmationStateHolder = createStateHolder()
        // state is null — no configure() has been called

        val publishableKey = capturePublishableKeyLambda(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = "pk_test_fallback_key",
        )

        assertThat(publishableKey()).isEqualTo("pk_test_fallback_key")
    }

    @Test
    fun `stripeAccountId lambda uses apiConfiguration when state has one`() = runTest {
        val confirmationStateHolder = createStateHolder()
        confirmationStateHolder.state = stateWithApiConfig(
            publishableKey = "pk_test_api_config_key",
            stripeAccountId = "acct_test_connected",
        )

        val stripeAccountId = captureStripeAccountIdLambda(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = "pk_test_fallback_key",
        )

        assertThat(stripeAccountId()).isEqualTo("acct_test_connected")
    }

    @Test
    fun `stripeAccountId lambda falls back to null when state is null`() = runTest {
        val confirmationStateHolder = createStateHolder()
        // state is null

        val stripeAccountId = captureStripeAccountIdLambda(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = "pk_test_fallback_key",
        )

        // PaymentConfiguration without stripeAccountId returns null
        assertThat(stripeAccountId()).isNull()
    }

    // ---- helpers ----

    private fun createStateHolder(): EmbeddedConfirmationStateHolder {
        return EmbeddedConfirmationStateHolder(
            savedStateHandle = SavedStateHandle(),
            selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()),
            coroutineScope = TestScope().backgroundScope,
        )
    }

    private fun stateWithApiConfig(
        publishableKey: String,
        stripeAccountId: String? = null,
    ): EmbeddedConfirmationStateHolder.State {
        val builder = ApiConfiguration.Builder(publishableKey)
        stripeAccountId?.let { builder.stripeAccountId(it) }
        return EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = null,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
                .apiConfiguration(builder.build())
                .build(),
        )
    }

    private fun capturePublishableKeyLambda(
        confirmationStateHolder: EmbeddedConfirmationStateHolder,
        fallbackKey: String,
    ): () -> String {
        var captured: (() -> String)? = null
        buildAndTriggerDefinition(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = fallbackKey,
            onPublishableKey = { captured = it },
        )
        return requireNotNull(captured) { "publishableKey lambda was not captured" }
    }

    private fun captureStripeAccountIdLambda(
        confirmationStateHolder: EmbeddedConfirmationStateHolder,
        fallbackKey: String,
    ): () -> String? {
        var captured: (() -> String?)? = null
        buildAndTriggerDefinition(
            confirmationStateHolder = confirmationStateHolder,
            fallbackKey = fallbackKey,
            onStripeAccountId = { captured = it },
        )
        return requireNotNull(captured) { "stripeAccountId lambda was not captured" }
    }

    /**
     * Creates an [IntentConfirmationDefinition] via [IntentConfirmationModule] and triggers
     * [IntentConfirmationDefinition.createLauncher] to force [StripePaymentLauncherAssistedFactory.create]
     * to be called, capturing the key lambdas via [onPublishableKey] and [onStripeAccountId].
     */
    @Suppress("UNCHECKED_CAST")
    private fun buildAndTriggerDefinition(
        confirmationStateHolder: EmbeddedConfirmationStateHolder,
        fallbackKey: String,
        onPublishableKey: ((() -> String)) -> Unit = {},
        onStripeAccountId: ((() -> String?)) -> Unit = {},
    ) {
        val mockLauncherFactory = mock<StripePaymentLauncherAssistedFactory> {
            on {
                create(
                    publishableKey = any(),
                    stripeAccountId = any(),
                    statusBarColor = anyOrNull(),
                    includePaymentSheetNextHandlers = any(),
                    hostActivityLauncher = any(),
                )
            } doAnswer { invocation ->
                onPublishableKey(invocation.getArgument(0) as () -> String)
                onStripeAccountId(invocation.getArgument(1) as () -> String?)
                mock<StripePaymentLauncher>()
            }
        }

        val definition = IntentConfirmationModule().providesIntentConfirmationDefinition(
            interceptorFactory = mock(),
            stripePaymentLauncherAssistedFactory = mockLauncherFactory,
            statusBarColor = null,
            paymentConfigurationProvider = { PaymentConfiguration(fallbackKey) },
            confirmationStateHolder = confirmationStateHolder,
        ) as IntentConfirmationDefinition

        definition.createLauncher(
            activityResultCaller = mock {
                on {
                    registerForActivityResult(
                        any<PaymentLauncherContract>(),
                        any(),
                    )
                } doReturn mock<ActivityResultLauncher<PaymentLauncherContract.Args>>()
            },
            onResult = {},
        )
    }
}
