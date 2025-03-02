package com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection

import android.os.Build
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.FakeCvcRecollectionInteractor
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
internal class CvcRecollectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cvcPage = CvcPage(composeRule)

    @Test
    fun cvcIsUpdatedInViewState() = runTest {
        val interactor = FakeCvcRecollectionInteractor()
        composeRule.setContent {
            CvcRecollectionPaymentSheetScreen(
                interactor = interactor,
            )
        }

        interactor.viewState.test {
            assertThat(awaitItem().cvcState.cvc).isEqualTo("")
            cvcPage.setCvc("444")
            assertThat(awaitItem().cvcState.cvc).isEqualTo("444")
        }
    }

    @Test
    fun processingUpdatesEnabledState() = runTest {
        val interactor = FakeCvcRecollectionInteractor()
        composeRule.setContent {
            CvcRecollectionPaymentSheetScreen(
                interactor = interactor,
            )
        }
        cvcPage.assertCvcField(isEnabled())
        interactor._viewState.update { original ->
            original.copy(isEnabled = false)
        }
        cvcPage.assertCvcField(isEnabled().not())
    }

    @Test
    fun screenDisplaysItemsInStateCorrectly() = runTest {
        val interactor = FakeCvcRecollectionInteractor()
        interactor._viewState.update {
            CvcRecollectionViewState(
                lastFour = "4242",
                isTestMode = false,
                cvcState = CvcState(
                    cvc = "555",
                    cardBrand = CardBrand.Visa,
                ),
                isEnabled = true,
            )
        }

        composeRule.setContent {
            CvcRecollectionPaymentSheetScreen(
                interactor = interactor,
            )
        }

        cvcPage.displaysLastFour("4242")
        cvcPage.assertCvcField(hasText("555"))
        cvcPage.assertCvcField(isEnabled())
        cvcPage.hasLabel("CVC")
        cvcPage.hasTitle("Confirm your CVC")
    }
}
