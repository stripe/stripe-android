package com.stripe.android.common.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterialApi::class)
class ElementsBottomSheetLayoutScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxSize(),
    )

    @Test
    fun testExpanded() {
        paparazzi.snapshot {
            Text(
                text = "My screen",
                modifier = Modifier.padding(20.dp)
            )

            ElementsBottomSheetLayout(
                state = rememberStripeBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                ),
                onDismissed = {},
            ) {
                Text(
                    text = "My bottom sheet",
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}
