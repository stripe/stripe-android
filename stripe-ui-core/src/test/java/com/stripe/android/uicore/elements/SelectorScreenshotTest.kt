package com.stripe.android.uicore.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.PaparazziTest
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category

@Category(PaparazziTest::class)
class SelectorScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 16.dp)
    )

    @Test
    fun selectorWithNoSelection() {
        paparazziRule.snapshot {
            Selector(
                currentItem = unknownItem,
                items = listOf(visaItem, linkItem),
                onItemSelected = {},
                hasFocus = true,
                popupMessage = "Choose an item".resolvableString,
                hasMadeSelection = false,
            )
        }
    }

    @Test
    fun selectorWithSelectionLeft() {
        paparazziRule.snapshot {
            Selector(
                currentItem = visaItem,
                items = listOf(visaItem, linkItem),
                onItemSelected = {},
                hasFocus = true,
                popupMessage = "Choose an item".resolvableString,
                hasMadeSelection = false,
            )
        }
    }

    @Test
    fun selectorWithSelectionRight() {
        paparazziRule.snapshot {
            Selector(
                currentItem = linkItem,
                items = listOf(visaItem, linkItem),
                onItemSelected = {},
                hasFocus = true,
                popupMessage = "Choose an item".resolvableString,
                hasMadeSelection = false,
            )
        }
    }

    private companion object {
        val unknownItem = TextFieldIcon.Selector.Item(
            id = "unknown",
            label = "Unknown".resolvableString,
            icon = com.stripe.android.uicore.R.drawable.stripe_ic_card_visa
        )

        val visaItem = TextFieldIcon.Selector.Item(
            id = "visa",
            label = "Visa".resolvableString,
            icon = com.stripe.android.uicore.R.drawable.stripe_ic_card_visa
        )

        val linkItem = TextFieldIcon.Selector.Item(
            id = "some_brand",
            label = "Some Brand".resolvableString,
            icon = com.stripe.android.uicore.R.drawable.stripe_ic_card_visa
        )
    }
}
