package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

internal class ManageScreenUITransitionScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(PaymentSheetAppearance.DefaultAppearance),
        boxModifier = Modifier
            .padding(16.dp)
            .height(112.dp),
    )

    @Test
    fun testSelectedToPendingTransition() {
        val paymentMethods = listOf(
            createCard(id = "pm_4242", last4 = "4242"),
            createCard(id = "pm_5555", last4 = "5555"),
        ).map { it.toDisplayableSavedPaymentMethod() }
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = paymentMethods,
                currentSelection = paymentMethods.first(),
                isEditing = false,
                canEdit = true,
                linkBrand = LinkBrand.Link,
                isProcessing = false,
                pendingPaymentMethodId = null,
                error = null,
            )
        )

        paparazziRule.gif(end = 2000L) {
            LaunchedEffect(interactor) {
                withFrameNanos {}
                delay(500L)
                interactor.updateState {
                    it.copy(
                        isProcessing = true,
                        pendingPaymentMethodId = paymentMethods[1].paymentMethod.id,
                    )
                }
            }

            ManageScreenUI(interactor = interactor)
        }
    }

    private fun createCard(id: String, last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            id = id,
            card = original.card?.copy(last4 = last4),
        )
    }
}
