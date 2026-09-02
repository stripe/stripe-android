package com.stripe.android.elements

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class PaymentElementHorizontalContentHelperTest {
    @Test
    fun `creates horizontal content when resolved layout is horizontal and no saved methods exist`() = runTest {
        val state = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(null)
        val content = FakeHorizontalContent()
        val factory = FakeHorizontalContentFactory(content)
        val customerStateHolder = FakeCustomerStateHolder()
        val helper = DefaultPaymentElementHorizontalContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            customerStateHolder = customerStateHolder,
            contentFactory = factory,
        )
        val horizontalState = createState(PaymentSheet.PaymentMethodLayout.Horizontal)

        helper.content.test {
            assertThat(awaitItem()).isNull()

            state.value = horizontalState
            runCurrent()

            assertThat(awaitItem()).isSameInstanceAs(content)
        }
        assertThat(factory.createCalls.awaitItem()).isEqualTo(horizontalState)
        factory.ensureAllEventsConsumed()
        content.ensureAllEventsConsumed()
        customerStateHolder.validate()
    }

    @Test
    fun `does not create horizontal content when saved methods exist`() = runTest {
        val state = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(null)
        val content = FakeHorizontalContent()
        val factory = FakeHorizontalContentFactory(content)
        val customerStateHolder = FakeCustomerStateHolder(
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        )
        val helper = DefaultPaymentElementHorizontalContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            customerStateHolder = customerStateHolder,
            contentFactory = factory,
        )

        state.value = createState(PaymentSheet.PaymentMethodLayout.Horizontal)
        runCurrent()

        assertThat(helper.content.value).isNull()
        factory.createCalls.expectNoEvents()
        factory.ensureAllEventsConsumed()
        content.ensureAllEventsConsumed()
        customerStateHolder.validate()
    }

    @Test
    fun `does not create horizontal content when resolved layout is vertical`() = runTest {
        val state = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(null)
        val content = FakeHorizontalContent()
        val factory = FakeHorizontalContentFactory(content)
        val customerStateHolder = FakeCustomerStateHolder()
        val helper = DefaultPaymentElementHorizontalContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            customerStateHolder = customerStateHolder,
            contentFactory = factory,
        )

        state.value = createState(PaymentSheet.PaymentMethodLayout.Vertical)
        runCurrent()

        assertThat(helper.content.value).isNull()
        factory.createCalls.expectNoEvents()
        factory.ensureAllEventsConsumed()
        content.ensureAllEventsConsumed()
        customerStateHolder.validate()
    }

    @Test
    fun `closes horizontal content when state is cleared`() = runTest {
        val state = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(
            createState(PaymentSheet.PaymentMethodLayout.Horizontal)
        )
        val content = FakeHorizontalContent()
        val factory = FakeHorizontalContentFactory(content)
        val customerStateHolder = FakeCustomerStateHolder()
        val helper = DefaultPaymentElementHorizontalContentHelper(
            coroutineScope = backgroundScope,
            state = state,
            customerStateHolder = customerStateHolder,
            contentFactory = factory,
        )
        runCurrent()

        assertThat(helper.content.value).isSameInstanceAs(content)
        assertThat(factory.createCalls.awaitItem()).isEqualTo(state.value)

        state.value = null
        runCurrent()

        assertThat(helper.content.value).isNull()
        content.closeCalls.awaitItem()
        factory.ensureAllEventsConsumed()
        content.ensureAllEventsConsumed()
        customerStateHolder.validate()
    }

    private fun createState(layout: PaymentSheet.PaymentMethodLayout): EmbeddedContentHelperStateHolder.State {
        return EmbeddedContentHelperStateFactory.create(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                    paymentMethodTypes = listOf("card"),
                ),
                paymentMethodLayout = layout,
            )
        )
    }

    private class FakeHorizontalContentFactory(
        private val content: PaymentElementHorizontalContent,
    ) : PaymentElementHorizontalContentFactory {
        val createCalls = Turbine<EmbeddedContentHelperStateHolder.State>()

        override fun create(state: EmbeddedContentHelperStateHolder.State): PaymentElementHorizontalContent {
            createCalls.add(state)
            return content
        }

        fun ensureAllEventsConsumed() {
            createCalls.ensureAllEventsConsumed()
        }
    }

    private class FakeHorizontalContent : PaymentElementHorizontalContent {
        val closeCalls = Turbine<Unit>()

        @androidx.compose.runtime.Composable
        override fun Content() = Unit

        override fun close() {
            closeCalls.add(Unit)
        }

        fun ensureAllEventsConsumed() {
            closeCalls.ensureAllEventsConsumed()
        }
    }
}
