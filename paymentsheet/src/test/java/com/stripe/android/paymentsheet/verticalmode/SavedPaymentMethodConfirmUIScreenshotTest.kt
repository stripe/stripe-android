package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import kotlin.test.Test

internal class SavedPaymentMethodConfirmUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(16.dp)
    )

    @Test
    fun testSavedPaymentMethodConfirmUI() {
        paparazziRule.snapshot {
            SavedPaymentMethodConfirmUI(
                savedPaymentMethodConfirmInteractor = FakeSavedPaymentMethodConfirmInteractor()
            )
        }
    }
}
