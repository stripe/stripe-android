package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class UpdatePaymentMethodUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testUpdatePaymentMethodUI_idle() {
        paparazziRule.snapshot {
            TestUpdatePaymentMethodUI(UpdatePaymentMethodInteractor.Status.Idle)
        }
    }

    @Test
    fun testUpdatePaymentMethodUI_updating() {
        paparazziRule.snapshot {
            TestUpdatePaymentMethodUI(UpdatePaymentMethodInteractor.Status.Updating)
        }
    }

    @Test
    fun testUpdatePaymentMethodUI_removing() {
        paparazziRule.snapshot {
            TestUpdatePaymentMethodUI(UpdatePaymentMethodInteractor.Status.Removing)
        }
    }

    @Composable
    private fun TestUpdatePaymentMethodUI(status: UpdatePaymentMethodInteractor.Status) {
        val interactor = FakeUpdatePaymentMethodInteractor(
            displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
            canRemove = true,
            isExpiredCard = false,
            isModifiablePaymentMethod = true,
            cardBrandFilter = DefaultCardBrandFilter,
            viewActionRecorder = ViewActionRecorder(),
            hasValidBrandChoices = true,
            shouldShowSetAsDefaultCheckbox = false,
            shouldShowSaveButton = true,
            setAsDefaultCheckboxEnabled = false,
            initialState = UpdatePaymentMethodInteractor.State(
                error = null,
                status = status,
                setAsDefaultCheckboxChecked = false,
                isSaveButtonEnabled = false,
            ),
            editCardDetailsInteractorFactory = FakeEditCardDetailsInteractorFactory()
        )

        UpdatePaymentMethodUI(interactor, Modifier)
    }
}
