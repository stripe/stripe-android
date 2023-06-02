package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class CustomerSheetScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        FontSize.values(),
        PaymentSheetAppearance.values(),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                header = "Screenshot testing",
                isLiveMode = false,
                isProcessing = false,
                isEditing = false,
            )
        }
    }
}
