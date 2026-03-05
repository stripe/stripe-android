package com.stripe.android.common.spms

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.DefaultCvcFormHelper.Companion.CVC_VALUE_KEY
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultCvcFormHelperTest {
    @Test
    fun `cvc form is unavailable when CVC recollection is not required`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Unused)
        }

        assertThat(helper.formElement).isNull()
    }

    @Test
    fun `cvc form element is available when CVC recollection is required`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)
        }

        assertThat(helper.formElement).isNotNull()
    }

    @Test
    fun `state is Unused when CVC recollection is not required`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Unused)
        }
    }

    @Test
    fun `state is Incomplete when CVC recollection is required and no CVC is entered`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)
        }
    }

    @Test
    fun `cvc value is saved to saved state handle when updated`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        ),
    ) {
        assertThat(handle.get<String>(CVC_VALUE_KEY)).isNull()

        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)

            cvcController!!.onValueChange("123")

            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Complete("123"))

            assertThat(handle.get<String>(CVC_VALUE_KEY)).isEqualTo("123")
        }
    }

    @Test
    fun `state is Complete when valid CVC is entered`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)

            cvcController!!.onValueChange("123")

            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Complete("123"))
        }
    }

    @Test
    fun `cvc form is unavailable for non-card payment methods`() = runScenario(
        paymentMethod = PaymentMethodFactory.usBankAccount(),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
        ),
    ) {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Unused)
        }

        assertThat(helper.formElement).isNull()
    }

    private fun runScenario(
        paymentMethod: PaymentMethod = PaymentMethodFactory.card(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        handle: SavedStateHandle = SavedStateHandle(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val helper = DefaultCvcFormHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            savedStateHandle = handle,
            paymentMethod = paymentMethod,
        )

        val cvcElement = helper.formElement?.let { formElement ->
            // Extract CvcElement from SectionElement
            (formElement as? com.stripe.android.uicore.elements.SectionElement)
                ?.fields
                ?.firstOrNull()
                as? com.stripe.android.ui.core.elements.CvcElement
        }

        val cvcController = cvcElement?.controller

        block(
            Scenario(
                helper = helper,
                handle = handle,
                cvcController = cvcController,
            )
        )
    }

    private class Scenario(
        val helper: CvcFormHelper,
        val handle: SavedStateHandle,
        val cvcController: com.stripe.android.ui.core.elements.CvcController?,
    )
}
