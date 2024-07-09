package com.stripe.android.paymentsheet.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Test
import org.mockito.Mockito.mock
import com.stripe.android.R as StripeR

class HeaderTextFactoryTest {

    @Test
    fun `Shows the correct header in complete flow and showing wallets`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods(
                FakeSelectSavedPaymentMethodsInteractor
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Does not show a header on AddAnotherPaymentMethod screen`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddAnotherPaymentMethod(
                interactor = FakeAddPaymentMethodInteractor
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Shows the correct header in complete flow and editing payment method`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.EditPaymentMethod(
                interactor = mock()
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(StripeR.string.stripe_title_update_card)
    }

    @Test
    fun `Shows the correct header if adding the first payment method in complete flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                interactor = FakeAddPaymentMethodInteractor
            ),
            isWalletEnabled = false,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_add_payment_method_title)
    }

    @Test
    fun `Does not show a header if adding the first payment method and wallets are available`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                interactor = FakeAddPaymentMethodInteractor
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Shows the correct header when displaying saved payment methods in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.SelectSavedPaymentMethods(
                FakeSelectSavedPaymentMethodsInteractor
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_select_payment_method)
    }

    @Test
    fun `Shows the correct header when only credit card form is shown in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                interactor = FakeAddPaymentMethodInteractor
            ),
            isWalletEnabled = false,
            types = listOf("card"),
        )

        assertThat(resource).isEqualTo(StripeR.string.stripe_title_add_a_card)
    }

    @Test
    fun `Shows the correct header when editing a saved payment method is shown in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.EditPaymentMethod(
                interactor = mock()
            ),
            isWalletEnabled = true,
            types = emptyList(),
        )

        assertThat(resource).isEqualTo(StripeR.string.stripe_title_update_card)
    }

    @Test
    fun `Shows the correct header when multiple LPMs are shown in custom flow`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.AddFirstPaymentMethod(
                interactor = FakeAddPaymentMethodInteractor
            ),
            isWalletEnabled = false,
            types = listOf("card", "not_card"),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method)
    }

    @Test
    fun `Shows correct header for manage saved PMs screen when first opened`() {
        val resource = getManagedSavedPaymentMethodsHeaderText(isCompleteFlow = true, isEditing = false)

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_select_payment_method)
    }

    @Test
    fun `Shows correct header for manage saved PMs screen when first opened - FlowController`() {
        val resource = getManagedSavedPaymentMethodsHeaderText(isCompleteFlow = false, isEditing = false)

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_select_payment_method)
    }

    @Test
    fun `Shows correct header for manage saved PMs screen when editing`() {
        val resource = getManagedSavedPaymentMethodsHeaderText(isCompleteFlow = true, isEditing = true)

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_manage_payment_methods)
    }

    @Test
    fun `Shows correct header for manage saved PMs screen when editing - FlowController`() {
        val resource = getManagedSavedPaymentMethodsHeaderText(isCompleteFlow = false, isEditing = true)

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_manage_payment_methods)
    }

    @Test
    fun `Shows correct header for vertical mode when wallets are not enabled`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.VerticalMode(
                FakeVerticalModeInteractor
            ),
            isWalletEnabled = false,
            types = listOf("card", "not_card"),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_select_payment_method)
    }

    @Test
    fun `Doesn't show header text for vertical mode when wallets are enabled`() {
        val resource = HeaderTextFactory(isCompleteFlow = true).create(
            screen = PaymentSheetScreen.VerticalMode(
                FakeVerticalModeInteractor
            ),
            isWalletEnabled = true,
            types = listOf("card", "not_card"),
        )

        assertThat(resource).isNull()
    }

    @Test
    fun `Shows correct header for vertical mode when wallets are not enabled in flow controller`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.VerticalMode(
                FakeVerticalModeInteractor
            ),
            isWalletEnabled = false,
            types = listOf("card", "not_card"),
        )

        assertThat(resource).isEqualTo(R.string.stripe_paymentsheet_choose_payment_method)
    }

    @Test
    fun `Shows correct header for vertical mode when only one payment method in flow controller`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.VerticalMode(
                FakeVerticalModeInteractor
            ),
            isWalletEnabled = false,
            types = listOf("card"),
        )

        assertThat(resource).isEqualTo(StripeR.string.stripe_title_add_a_card)
    }

    @Test
    fun `Doesn't show header for vertical mode when wallets are enabled in flow controller`() {
        val resource = HeaderTextFactory(isCompleteFlow = false).create(
            screen = PaymentSheetScreen.VerticalMode(
                FakeVerticalModeInteractor
            ),
            isWalletEnabled = true,
            types = listOf("card"),
        )

        assertThat(resource).isNull()
    }

    private fun getManagedSavedPaymentMethodsHeaderText(isCompleteFlow: Boolean, isEditing: Boolean): Int? {
        return HeaderTextFactory(isCompleteFlow = isCompleteFlow).create(
            screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = FakeManageScreenInteractor()),
            isWalletEnabled = false,
            types = emptyList(),
            isEditing,
        )
    }

    private object FakeVerticalModeInteractor : PaymentMethodVerticalLayoutInteractor {
        override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State>
            get() = throw NotImplementedError("This fake is not implemented yet.")
        override val showsWalletsHeader: StateFlow<Boolean> = MutableStateFlow(false)

        override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
            // Do nothing.
        }
    }
}
