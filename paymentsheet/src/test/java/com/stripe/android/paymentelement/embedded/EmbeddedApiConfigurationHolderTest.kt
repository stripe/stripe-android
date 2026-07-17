package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class EmbeddedApiConfigurationHolderTest {

    private val fallbackPublishableKey = "pk_test_fallback"
    private val fallbackStripeAccountId = "acct_fallback"
    private val paymentConfig = PaymentConfiguration(
        publishableKey = fallbackPublishableKey,
        stripeAccountId = fallbackStripeAccountId,
    )

    private fun createConfirmationStateHolder(
        configuration: EmbeddedPaymentElement.Configuration? = null,
    ): EmbeddedConfirmationStateHolder {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
        val holder = EmbeddedConfirmationStateHolder(
            savedStateHandle = savedStateHandle,
            selectionHolder = selectionHolder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        if (configuration != null) {
            holder.state = EmbeddedConfirmationStateHolder.State(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                selection = null,
                configuration = configuration,
            )
        }
        return holder
    }

    @Test
    fun `publishableKey falls back to PaymentConfiguration when confirmationState is null`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        assertThat(holder.publishableKey).isEqualTo(fallbackPublishableKey)
    }

    @Test
    fun `stripeAccountId falls back to PaymentConfiguration when confirmationState is null`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        assertThat(holder.stripeAccountId).isEqualTo(fallbackStripeAccountId)
    }

    @Test
    fun `holder is pre-populated when confirmationState has non-null apiConfiguration`() = runTest {
        val apiConfig = ApiConfiguration(
            publishableKey = "pk_test_from_state",
            stripeAccountId = "acct_from_state",
        )
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .apiConfiguration(apiConfig)
            .build()
        val confirmationStateHolder = createConfirmationStateHolder(configuration = configuration)

        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        assertThat(holder.publishableKey).isEqualTo("pk_test_from_state")
        assertThat(holder.stripeAccountId).isEqualTo("acct_from_state")
    }

    @Test
    fun `publishableKey returns config key after update`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        holder.update(ApiConfiguration(publishableKey = "pk_test_custom", stripeAccountId = "acct_custom"))

        assertThat(holder.publishableKey).isEqualTo("pk_test_custom")
    }

    @Test
    fun `stripeAccountId returns config value after update`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        holder.update(ApiConfiguration(publishableKey = "pk_test_custom", stripeAccountId = "acct_custom"))

        assertThat(holder.stripeAccountId).isEqualTo("acct_custom")
    }

    @Test
    fun `publishableKey falls back to PaymentConfiguration after update with null`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        holder.update(ApiConfiguration(publishableKey = "pk_test_custom"))
        holder.update(null)

        assertThat(holder.publishableKey).isEqualTo(fallbackPublishableKey)
    }

    @Test
    fun `stripeAccountId falls back to PaymentConfiguration after update with null`() = runTest {
        val confirmationStateHolder = createConfirmationStateHolder(configuration = null)
        val holder = EmbeddedApiConfigurationHolder(
            confirmationStateHolder = confirmationStateHolder,
            paymentConfig = { paymentConfig },
        )

        holder.update(ApiConfiguration(publishableKey = "pk_test_custom", stripeAccountId = "acct_custom"))
        holder.update(null)

        assertThat(holder.stripeAccountId).isEqualTo(fallbackStripeAccountId)
    }
}
