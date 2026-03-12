package com.stripe.android.common.spms

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.spms.DefaultCvcFormHelper.Companion.CVC_VALUE_KEY
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultCvcFormHelperTest {
    @Test
    fun `state is Incomplete when no CVC is entered`() = runScenario {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)
        }
    }

    @Test
    fun `cvc value is saved to saved state handle when updated`() = runScenario {
        assertThat(handle.get<String>(CVC_VALUE_KEY)).isNull()

        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)

            cvcController.onValueChange("123")

            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Complete("123"))
            assertThat(handle.get<String>(CVC_VALUE_KEY)).isEqualTo("123")
        }
    }

    @Test
    fun `state is Complete when valid CVC is entered`() = runScenario {
        helper.state.test {
            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Incomplete)

            cvcController.onValueChange("123")

            assertThat(awaitItem()).isEqualTo(CvcFormHelper.State.Complete("123"))
        }
    }

    private fun runScenario(
        paymentMethod: PaymentMethod = PaymentMethodFactory.card(),
        handle: SavedStateHandle = SavedStateHandle(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val helper = DefaultCvcFormHelper(
            savedStateHandle = handle,
            paymentMethod = paymentMethod,
        )

        val cvcElement = (helper.formElement as com.stripe.android.uicore.elements.SectionElement)
            .fields
            .first() as com.stripe.android.ui.core.elements.CvcElement

        block(
            Scenario(
                helper = helper,
                handle = handle,
                cvcController = cvcElement.controller,
            )
        )
    }

    private class Scenario(
        val helper: CvcFormHelper,
        val handle: SavedStateHandle,
        val cvcController: com.stripe.android.ui.core.elements.CvcController,
    )
}
