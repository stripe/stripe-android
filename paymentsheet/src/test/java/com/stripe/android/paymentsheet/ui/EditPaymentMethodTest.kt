package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EditPaymentMethodTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Card drop down has accessibility label`() {
        composeRule.setContent {
            EditPaymentMethod(
                interactor = FakeEditPaymentMethodInteractor(selectedBrand = CardBrand.Visa),
                modifier = Modifier
            )
        }

        composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
            .assertContentDescriptionEquals("Visa")
    }

    private class FakeEditPaymentMethodInteractor(selectedBrand: CardBrand = CardBrand.Visa) :
        EditPaymentMethodViewInteractor {
        override val viewState: StateFlow<EditPaymentMethodViewState> = MutableStateFlow(
            EditPaymentMethodViewState(
                status = EditPaymentMethodViewState.Status.Idle,
                last4 = "4242",
                displayName = "Visa".resolvableString,
                canUpdate = true,
                selectedBrand = CardBrandChoice(selectedBrand),
                availableBrands = listOf(
                    CardBrandChoice(CardBrand.Visa),
                    CardBrandChoice(CardBrand.CartesBancaires)
                ),
                canRemove = true,
            )
        )

        override fun handleViewAction(viewAction: EditPaymentMethodViewAction) {
            // Do nothing.
        }
    }
}
