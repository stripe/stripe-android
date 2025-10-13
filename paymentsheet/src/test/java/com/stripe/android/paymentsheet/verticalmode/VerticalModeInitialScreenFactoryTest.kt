package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class VerticalModeInitialScreenFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `returns form screen when only one payment method available and has interactable elements`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("cashapp"),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
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

    @Test
    fun `returns list screen when only one payment method available with no interactable elements`() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("alipay"),
            )
        ),
        hasSavedPaymentMethods = false
    ) {
        assertThat(screens).hasSize(1)
        assertThat(screens[0]).isInstanceOf<PaymentSheetScreen.VerticalMode>()
    }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        ),
        hasSavedPaymentMethods: Boolean = false,
        selection: PaymentSelection? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeViewModel = FakeBaseSheetViewModel.create(
            paymentMethodMetadata = paymentMethodMetadata,
            initialScreen = PaymentSheetScreen.Loading,
            canGoBack = false,
        )

        val customerStateHolder = CustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = fakeViewModel.selection,
            customerMetadataPermissions = stateFlowOf(
                paymentMethodMetadata.customerMetadata?.permissions
            )
        )
        if (hasSavedPaymentMethods) {
            customerStateHolder.setCustomerState(
                CustomerState(
                    customerMetadata = PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA,
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                    defaultPaymentMethodId = null,
                )
            )
        }

        fakeViewModel.updateSelection(selection)

        val screens = VerticalModeInitialScreenFactory.create(
            viewModel = fakeViewModel,
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
        )

        block(
            Scenario(
                screens = screens
            )
        )
    }

    private class Scenario(
        val screens: List<PaymentSheetScreen>,
    )
}
