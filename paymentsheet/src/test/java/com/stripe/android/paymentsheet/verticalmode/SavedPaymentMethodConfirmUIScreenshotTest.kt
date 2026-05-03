package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.PaparazziTest
import org.junit.Rule
import org.junit.experimental.categories.Category
import kotlin.test.Test

@Category(PaparazziTest::class)
internal class SavedPaymentMethodConfirmUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(16.dp)
    )

    @Test
    fun formEnabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodConfirmUI(
                savedPaymentMethodConfirmInteractor = FakeSavedPaymentMethodConfirmInteractor(
                    formEnabled = false,
                )
            )
        }
    }

    @Test
    fun formDisabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodConfirmUI(
                savedPaymentMethodConfirmInteractor = FakeSavedPaymentMethodConfirmInteractor(
                    formEnabled = true,
                )
            )
        }
    }
}
