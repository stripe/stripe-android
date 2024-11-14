package com.stripe.android.paymentsheet.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.FakeCvcRecollectionInteractor
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcCompletionState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentSheetCvcRecollectionTest {
    @Test
    fun `buy button returns correct state`() = runTest {
        PaymentSheetScreen.CvcRecollection(mock())
            .buyButtonState.test {
                assertThat(awaitItem())
                    .isEqualTo(
                        BuyButtonState(
                            visible = true,
                            buyButtonOverride = BuyButtonState.BuyButtonOverride(
                                label = resolvableString(R.string.stripe_paymentsheet_confirm),
                                lockEnabled = false
                            )
                        )
                    )
            }
    }

    @Test
    fun `top bar returns correct state`() = runTest {
        val interactor = FakeCvcRecollectionInteractor()
        PaymentSheetScreen.CvcRecollection(interactor)
            .topBarState().test {
                val itemOne = awaitItem()
                assertThat(itemOne?.showTestModeLabel).isEqualTo(interactor.viewState.value.isTestMode)
                assertThat(itemOne?.isEditing).isTrue()

                interactor.updateCompletionState(CvcCompletionState.Completed("555"))

                val itemTwo = awaitItem()
                assertThat(itemTwo?.showTestModeLabel).isEqualTo(interactor.viewState.value.isTestMode)
                assertThat(itemTwo?.isEditing).isFalse()
            }
    }
}
