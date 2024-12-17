package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedContentHelper.Companion.MANDATE_KEY_EMBEDDED_CONTENT
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedContentHelper.Companion.PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock
import kotlin.test.Test

internal class DefaultEmbeddedContentHelperTest {
    @Test
    fun `dataLoaded updates savedStateHandle with paymentMethodMetadata`() = testScenario {
        assertThat(savedStateHandle.get<PaymentMethodMetadata?>(PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT))
            .isNull()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        embeddedContentHelper.dataLoaded(paymentMethodMetadata)
        assertThat(savedStateHandle.get<PaymentMethodMetadata?>(PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT))
            .isEqualTo(paymentMethodMetadata)
    }

    @Test
    fun `dataLoaded emits embeddedContent event`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(PaymentMethodMetadataFactory.create())
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `setting mandate emits embeddedContent event`() = testScenario {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNull()
            embeddedContentHelper.dataLoaded(PaymentMethodMetadataFactory.create())
            awaitItem().run {
                assertThat(this).isNotNull()
                assertThat(this?.mandate).isNull()
            }
            savedStateHandle[MANDATE_KEY_EMBEDDED_CONTENT] = "Hi".resolvableString
            awaitItem().run {
                assertThat(this).isNotNull()
                assertThat(this?.mandate).isEqualTo("Hi".resolvableString)
            }
        }
    }

    @Test
    fun `initializing embeddedContentHelper with paymentMethodMetadata emits correct initial event`() = testScenario(
        setup = {
            set(PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT, PaymentMethodMetadataFactory.create())
        }
    ) {
        embeddedContentHelper.embeddedContent.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `initializing embeddedContentHelper with mandate emits correct initial event`() = testScenario(
        setup = {
            set(PAYMENT_METHOD_METADATA_KEY_EMBEDDED_CONTENT, PaymentMethodMetadataFactory.create())
            set(MANDATE_KEY_EMBEDDED_CONTENT, "Hi".resolvableString)
        }
    ) {
        embeddedContentHelper.embeddedContent.test {
            awaitItem().run {
                assertThat(this).isNotNull()
                assertThat(this?.mandate).isEqualTo("Hi".resolvableString)
            }
        }
    }

    private class Scenario(
        val embeddedContentHelper: DefaultEmbeddedContentHelper,
        val savedStateHandle: SavedStateHandle,
    )

    private fun testScenario(
        setup: SavedStateHandle.() -> Unit = {},
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle.setup()
        val embeddedContentHelper = DefaultEmbeddedContentHelper(
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = savedStateHandle,
            eventReporter = mock(),
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            workContext = Dispatchers.Unconfined,
            customerRepository = FakeCustomerRepository(),
            selectionHolder = EmbeddedSelectionHolder(savedStateHandle),
        )
        Scenario(
            embeddedContentHelper = embeddedContentHelper,
            savedStateHandle = savedStateHandle,
        ).block()
    }
}
