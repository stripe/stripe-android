package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.junit.Test

internal class InitEventHelperTest {
    @Test
    fun `pending init returns explicit publishable key once`() {
        val helper = InitEventHelper()
        helper.onInit()

        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_explicit",
                paymentMethodMetadata = null,
            )
        ).isEqualTo("pk_test_explicit")
        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_explicit",
                paymentMethodMetadata = null,
            )
        ).isNull()
    }

    @Test
    fun `explicit publishable key takes precedence over metadata`() {
        val helper = InitEventHelper()
        helper.onInit()

        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_explicit",
                paymentMethodMetadata = PAYMENT_METHOD_METADATA,
            )
        ).isEqualTo("pk_test_explicit")
    }

    @Test
    fun `pending init returns metadata publishable key`() {
        val helper = InitEventHelper()
        helper.onInit()

        assertThat(
            helper.publishableKeyForInit(
                publishableKey = null,
                paymentMethodMetadata = PAYMENT_METHOD_METADATA,
            )
        ).isEqualTo("pk_test_metadata")
    }

    @Test
    fun `missing publishable key and metadata do not consume pending init`() {
        val helper = InitEventHelper()
        helper.onInit()

        assertThat(
            helper.publishableKeyForInit(
                publishableKey = null,
                paymentMethodMetadata = null,
            )
        ).isNull()
        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_explicit",
                paymentMethodMetadata = null,
            )
        ).isEqualTo("pk_test_explicit")
    }

    @Test
    fun `onInit queues another event after previous event was consumed`() {
        val helper = InitEventHelper()
        helper.onInit()
        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_first",
                paymentMethodMetadata = null,
            )
        ).isEqualTo("pk_test_first")

        helper.onInit()

        assertThat(
            helper.publishableKeyForInit(
                publishableKey = "pk_test_second",
                paymentMethodMetadata = null,
            )
        ).isEqualTo("pk_test_second")
    }

    private companion object {
        val PAYMENT_METHOD_METADATA = PaymentMethodMetadataFactory.create(
            apiConfiguration = ApiConfiguration.State(
                publishableKey = "pk_test_metadata",
                stripeAccountId = "acct_123",
            )
        )
    }
}
