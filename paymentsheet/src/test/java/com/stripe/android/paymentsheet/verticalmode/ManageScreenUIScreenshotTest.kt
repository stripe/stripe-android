package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class ManageScreenUIScreenshotTest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.useNewUpdateCardScreen,
        isEnabled = false
    )

    @get:Rule
    val paparazziRule = PaparazziRule(
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testManageUIScreen_noSelectedPMs() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = null,
                        isEditing = false,
                        canRemove = true,
                        canEdit = true,
                    )
                ),
            )
        }
    }

    @Test
    fun testManageUIScreen_withSelectedPM() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = savedPaymentMethods[1],
                        isEditing = false,
                        canRemove = true,
                        canEdit = true,
                    )
                ),
            )
        }
    }

    @Test
    fun testManageUIScreen_inEditMode() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = null,
                        isEditing = true,
                        canRemove = true,
                        canEdit = true,
                    )
                ),
            )
        }
    }

    @Test
    fun testManageUIScreen_usesNewUpdateCardScreen_inEditMode() {
        FeatureFlags.useNewUpdateCardScreen.setEnabled(true)
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = null,
                        isEditing = true,
                        canRemove = true,
                        canEdit = true,
                    )
                ),
            )
        }
    }

    @Test
    fun testManageUIScreen_withoutDeleteIcon() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = listOf(
                            PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
                        ),
                        currentSelection = null,
                        isEditing = true,
                        canRemove = false,
                        canEdit = true,
                    )
                ),
            )
        }
    }

    private val savedPaymentMethods: List<DisplayableSavedPaymentMethod> = listOf(
        createCard("4242"),
        createCard("4000"),
        createUsBank("1001"),
        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
    ).map { it.toDisplayableSavedPaymentMethod() }

    private fun createCard(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            card = original.card?.copy(last4 = last4),
        )
    }

    private fun createUsBank(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.US_BANK_ACCOUNT
        return original.copy(usBankAccount = original.usBankAccount?.copy(last4 = last4))
    }
}
