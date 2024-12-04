package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import kotlin.test.Test

class VerticalModeInitialScreenFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `returns form screen when only one payment method available`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            )
        )
    ) {
        assertThat(screens).hasSize(1)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
    }

    @Test
    fun `returns list screen when only one payment method available with saved payment methods`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            )
        ),
        hasSavedPaymentMethods = true
    ) {
        assertThat(screens).hasSize(1)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
    }

    @Test
    fun `returns both screens when selection is new card`() = runScenario(
        selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) {
        assertThat(screens).hasSize(2)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
        assertThat(screens[1]).isInstanceOf<PaymentSheetScreen.VerticalModeForm>()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        hasSavedPaymentMethods: Boolean = false,
        selection: PaymentSelection? = null,
        block: Scenario.() -> Unit,
    ) {
        val fakeViewModel = FakeBaseSheetViewModel.create(
            paymentMethodMetadata = paymentMethodMetadata,
            initialScreen = PaymentSheetScreen.Loading,
        )

        val customerStateHolder = CustomerStateHolder(SavedStateHandle(), fakeViewModel.selection)
        if (hasSavedPaymentMethods) {
            customerStateHolder.setCustomerState(
                CustomerState(
                    id = "cus_foobar",
                    ephemeralKeySecret = "ek_123",
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveLastPaymentMethod = true,
                        canRemoveDuplicates = true,
                    )
                )
            )
        }

        fakeViewModel.updateSelection(selection)

        val screens = VerticalModeInitialScreenFactory.create(
            viewModel = fakeViewModel,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
        )
        Scenario(
            screens = screens
        ).apply(block)
    }

    private class Scenario(
        val screens: List<PaymentSheetScreen>,
    )
}
